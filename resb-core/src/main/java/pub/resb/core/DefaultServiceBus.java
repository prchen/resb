package pub.resb.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pub.resb.api.constants.Pointcut;
import pub.resb.api.interfaces.*;
import pub.resb.api.models.Catchable;
import pub.resb.api.models.Command;
import pub.resb.api.models.Reply;
import pub.resb.core.exceptions.*;
import pub.resb.core.utils.ResbApiUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import javax.annotation.PreDestroy;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public final class DefaultServiceBus implements ServiceBus {
    private static Logger logger = LoggerFactory.getLogger(DefaultServiceBus.class);
    private static int instanceCounter = 0;

    // Internal properties
    private int id = instanceCounter;
    private int threadCounter = 0;
    private boolean built = false;
    private List<CellUnit> units = new LinkedList<>();
    private ExecutorService worker;

    // Configurable properties
    private List<Cell<?, ?>> cells = new LinkedList<>();
    private List<Explorer> explorers = new LinkedList<>();
    private List<Resolver> resolvers = new LinkedList<>();
    private List<Protocol> protocols = new LinkedList<>();
    private List<Interceptor> interceptors = new LinkedList<>();
    private int threadPoolSize = 2 * Runtime.getRuntime().availableProcessors();

    private DefaultServiceBus() {
        instanceCounter++;
    }

    private Builder createBuilder() {
        return new Builder();
    }

    private synchronized Thread createThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setName(String.format("bus-%s-%s", id, threadCounter));
        threadCounter++;
        return thread;
    }

    private ServiceBus build() {
        cells = Ordered.adjust(cells, true);
        explorers = Ordered.adjust(explorers, true);
        resolvers = Ordered.adjust(resolvers, true);
        protocols = Ordered.adjust(protocols, true);
        interceptors = Ordered.adjust(interceptors, false);
        worker = Executors.newFixedThreadPool(threadPoolSize, this::createThread);
        units = cells.stream().map(CellUnit::new).collect(Collectors.toList());
        DefaultServiceBus serviceBus = new DefaultServiceBus();
        String tpl = "ServiceBus built with " +
                "%s cells " +
                "%s explorers " +
                "%s resolvers " +
                "%s protocols " +
                "and %s interceptors";
        logger.info(String.format(tpl, cells.size(), explorers.size(),
                resolvers.size(), protocols.size(), interceptors.size()));
        built = true;
        return this;
    }

    public static synchronized Builder builder() {
        return new DefaultServiceBus().createBuilder();
    }

    @Override
    public <R> Mono<Reply<R>> exchange(Command<R> command) {
        return Mono.fromCallable(() -> command)
                .flatMap(this::doExchange)
                .switchIfEmpty(Mono.error(new InvalidReplyException("reply not present")))
                .onErrorResume(this::resume);
    }

    @Override
    public <T, R> Mono<Reply<R>> serverExchange(String cellName, T payload) {
        return Mono.fromCallable(() -> Tuples.of(cellName, payload))
                .flatMap(this::doExchange)
                .map(x -> (Reply<R>) x)
                .switchIfEmpty(Mono.error(new InvalidReplyException("reply not present")))
                .onErrorResume(this::resume);
    }

    @Override
    public Mono<InetSocketAddress> discover(URI uri) {
        Mono<InetSocketAddress> endpoint = Flux.fromIterable(explorers)
                .flatMap(x -> x.discover(this, uri))
                .next();
        Mono<InetAddress> address = endpoint.flatMap(x -> {
            if (x.isUnresolved()) {
                return this.resolve(x.getHostString());
            } else {
                return Mono.just(x.getAddress());
            }
        });
        return Mono.zip(endpoint, address, (e, a) -> new InetSocketAddress(a, e.getPort()));
    }

    @Override
    public Mono<InetAddress> resolve(String name) {
        return Flux.fromIterable(resolvers)
                .flatMap(r -> r.resolve(this, name))
                .next();
    }

    @Override
    public <T> Mono<T> wrapBlocking(Supplier<T> supplier) {
        return Mono.fromFuture(() -> CompletableFuture.supplyAsync(supplier, worker));
    }

    @Override
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down DefaultServiceBus...");
        worker.shutdown();
    }

    @Override
    protected void finalize() {
        shutdown();
    }

    private <R> Mono<Reply<R>> doExchange(Command<R> command) {
        return doLocalExchange(command)
                .switchIfEmpty(wrapCallable(() -> doRemoteExchange(command)));
    }

    private <T, R> Mono<Reply<R>> doExchange(Tuple2<String, T> cellNameAndPayload) {
        String cellName = cellNameAndPayload.getT1();
        T payload = cellNameAndPayload.getT2();
        Mono<CellUnit> unit = Flux.fromIterable(units)
                .filter(x -> Objects.equals(cellName, x.name))
                .next()
                .switchIfEmpty(Mono.error(new HostedCellNotExistsException(cellName)));
        Mono<Command> command = unit.flatMap(x -> x.protocol.serverDeserialize(this, x.commandType, payload));
        return Mono.zip(command, unit)
                .flatMap(t -> doChainedServerExchange(t.getT1(), t.getT2()))
                .map(x -> (Reply<R>) x);
    }

    private <R> Mono<Reply<R>> doLocalExchange(Command<R> command) {
        return Flux.fromIterable(units)
                .filter(x -> x.commandType.isAssignableFrom(command.getClass()))
                .next()
                .flatMap(x -> doChainedNativeExchange(command, x));
    }

    private <R> Mono<Reply<R>> doRemoteExchange(Command<R> command) {
        URI entry = ResbApiUtils.getEntry((Class<? extends Command<?>>) command.getClass());
        if (entry == null) {
            throw new NativeCellNotExistsException(command);
        }
        return Flux.fromIterable(protocols)
                .filter(x -> x.accept(entry))
                .next()
                .switchIfEmpty(Mono.error(new ProtocolNotExistsException(entry)))
                .flatMap(x -> doChainedClientExchange(command, x, entry));
    }

    private <R> Mono<Reply<R>> doChainedNativeExchange(Command<R> command, CellUnit unit) {
        return doChainedExchange(command, Pointcut.NATIVE_EXCHANGE,
                c -> unit.cell.exchange(this, c));
    }

    private <R> Mono<Reply<R>> doChainedClientExchange(Command<R> command, Protocol protocol, URI entry) {
        return doChainedExchange(command, Pointcut.CLIENT_EXCHANGE,
                c -> (Mono<Reply<R>>) protocol.clientExchange(this, entry, c));
    }

    private <R> Mono<Reply<R>> doChainedServerExchange(Command command, CellUnit unit) {
        return doChainedExchange(command, Pointcut.SERVER_EXCHANGE,
                c -> unit.cell.exchange(this, c));
    }

    private <R> Mono<Reply<R>> doChainedExchange(Command<R> command, Pointcut pointcut, Function<Command<R>, Mono<Reply<R>>> tail) {
        Function<Command<R>, Mono<Reply<R>>> now = tail;
        for (Interceptor interceptor : interceptors) {
            if (interceptor.getPointcuts().contains(pointcut)) {
                if (interceptor.acceptCommandType(command.getClass())) {
                    Function<Command<R>, Mono<Reply<R>>> next = now;
                    now = x -> interceptor.intercept(this, x, pointcut, next);
                }
            }
        }
        return now.apply(command).doOnNext(this::verifyReply);
    }

    private void verifyReply(Reply<?> reply) {
        if (reply.getSuccess() == null) {
            throw new InvalidReplyException("success not present", reply);
        } else if (!reply.getSuccess() && (reply.getErrorName() == null || reply.getErrorAttrs() == null)) {
            throw new InvalidReplyException("errorName or errorAttrs not present", reply);
        }
    }

    private <T> Mono<T> wrapCallable(Callable<Mono<T>> callable) {
        return Mono.fromCallable(callable).flatMap(x -> x);
    }

    private <R> Mono<Reply<R>> resume(Throwable e) {
        if (e instanceof Catchable) {
            return ((Catchable) e).resume();
        } else {
            return new UnexpectedException(e).resume();
        }
    }

    public class Builder {

        private Builder() {
        }

        public synchronized <C extends Command<R>, R> Builder cell(Cell<C, R> cell) {
            assertNotBuilt();
            DefaultServiceBus.this.cells.add(cell);
            return this;
        }

        public synchronized Builder cells(Collection<Cell<?, ?>> cells) {
            assertNotBuilt();
            DefaultServiceBus.this.cells.addAll(cells);
            return this;
        }

        public synchronized Builder explorer(Explorer explorer) {
            assertNotBuilt();
            DefaultServiceBus.this.explorers.add(explorer);
            return this;
        }

        public synchronized Builder explorers(Collection<Explorer> explorers) {
            assertNotBuilt();
            DefaultServiceBus.this.explorers.addAll(explorers);
            return this;
        }

        public synchronized Builder resolver(Resolver resolver) {
            assertNotBuilt();
            DefaultServiceBus.this.resolvers.add(resolver);
            return this;
        }

        public synchronized Builder resolvers(Collection<Resolver> resolvers) {
            assertNotBuilt();
            DefaultServiceBus.this.resolvers.addAll(resolvers);
            return this;
        }

        public synchronized Builder protocol(Protocol protocol) {
            assertNotBuilt();
            DefaultServiceBus.this.protocols.add(protocol);
            return this;
        }

        public synchronized Builder protocols(Collection<Protocol<?>> protocols) {
            assertNotBuilt();
            DefaultServiceBus.this.protocols.addAll(protocols);
            return this;
        }

        public synchronized Builder interceptor(Interceptor interceptor) {
            assertNotBuilt();
            DefaultServiceBus.this.interceptors.add(interceptor);
            return this;
        }

        public synchronized Builder interceptors(Collection<Interceptor> interceptors) {
            assertNotBuilt();
            DefaultServiceBus.this.interceptors.addAll(interceptors);
            return this;
        }

        public synchronized Builder threadPoolSize(int threadPoolSize) {
            assertNotBuilt();
            DefaultServiceBus.this.threadPoolSize = threadPoolSize;
            return this;
        }

        public synchronized ServiceBus build() {
            assertNotBuilt();
            return DefaultServiceBus.this.build();
        }

        private void assertNotBuilt() {
            if (built) {
                throw new IllegalStateException("ServiceBus has bean already built");
            }
        }
    }

    private class CellUnit {
        private Cell cell;
        private Class commandType;
        private URI entry;
        private String name;
        private Protocol protocol;

        private CellUnit(Cell<?, ?> cell) {
            this.cell = cell;
            this.commandType = cell.getCommandType();
            this.entry = ResbApiUtils.getEntry(commandType);
            if (entry != null) {
                this.name = ResbApiUtils.getCellName(entry);
                this.protocol = protocols.stream()
                        .filter(x -> x.accept(entry))
                        .findFirst()
                        .orElse(null);
            }
        }
    }
}
