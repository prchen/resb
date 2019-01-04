package pub.resb.reactor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pub.resb.reactor.constants.Pointcut;
import pub.resb.reactor.exceptions.*;
import pub.resb.reactor.interfaces.*;
import pub.resb.reactor.models.Catchable;
import pub.resb.reactor.models.Command;
import pub.resb.reactor.models.Reply;
import pub.resb.reactor.utils.ModelUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import static pub.resb.reactor.constants.Pointcut.*;

@SuppressWarnings("unchecked")
public class ServiceBusBuilder {
    private List<Cell<?, ?>> cells = new LinkedList<>();
    private List<Explorer> explorers = new LinkedList<>();
    private List<Resolver> resolvers = new LinkedList<>();
    private List<Protocol> protocols = new LinkedList<>();
    private List<Interceptor> interceptors = new LinkedList<>();
    private Logger logger = LoggerFactory.getLogger(ServiceBus.class);
    private List<Unit> units = new LinkedList<>();

    ServiceBusBuilder() {
    }

    public <C extends Command<R>, R> ServiceBusBuilder cell(Cell<C, R> cell) {
        this.cells.add(cell);
        return this;
    }

    public ServiceBusBuilder cells(Collection<Cell<?, ?>> cells) {
        this.cells.addAll(cells);
        return this;
    }

    public ServiceBusBuilder explorer(Explorer explorer) {
        this.explorers.add(explorer);
        return this;
    }

    public ServiceBusBuilder explorers(Collection<Explorer> explorers) {
        this.explorers.addAll(explorers);
        return this;
    }

    public ServiceBusBuilder resolver(Resolver resolver) {
        this.resolvers.add(resolver);
        return this;
    }

    public ServiceBusBuilder resolvers(Collection<Resolver> resolvers) {
        this.resolvers.addAll(resolvers);
        return this;
    }

    public ServiceBusBuilder protocol(Protocol protocol) {
        this.protocols.add(protocol);
        return this;
    }

    public ServiceBusBuilder protocols(Collection<Protocol<?>> protocols) {
        this.protocols.addAll(protocols);
        return this;
    }

    public ServiceBusBuilder interceptor(Interceptor interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    public ServiceBusBuilder interceptors(Collection<Interceptor> interceptors) {
        this.interceptors.addAll(interceptors);
        return this;
    }

    public ServiceBus build() {
        cells = Ordered.adjust(cells, true);
        explorers = Ordered.adjust(explorers, true);
        resolvers = Ordered.adjust(resolvers, true);
        protocols = Ordered.adjust(protocols, true);
        interceptors = Ordered.adjust(interceptors, false);
        units = cells.stream().map(Unit::new).collect(Collectors.toList());
        String tpl = "ServiceBus built with " +
                "%s cells " +
                "%s explorers " +
                "%s resolvers " +
                "%s protocols " +
                "and %s interceptors";
        logger.info(String.format(tpl, cells.size(), explorers.size(),
                resolvers.size(), protocols.size(), interceptors.size()));
        return new DefaultServiceBus();
    }

    private class Unit {
        private Cell cell;
        private Class commandType;
        private URI entry;
        private String name;
        private Protocol protocol;

        private Unit(Cell<?, ?> cell) {
            this.cell = cell;
            this.commandType = ModelUtils.getCommandType((Class<? extends Cell<?, ?>>) cell.getClass());
            this.entry = ModelUtils.getEntry(commandType);
            if (entry != null) {
                this.name = ModelUtils.getCellName(entry);
                this.protocol = protocols.stream()
                        .filter(x -> x.accept(entry))
                        .findFirst()
                        .orElse(null);
            }
        }
    }

    private class DefaultServiceBus implements ServiceBus {
        @Override
        public <R> Mono<Reply<R>> exchange(Command<R> command) {
            return Mono.fromCallable(() -> command)
                    .flatMap(this::doExchange)
                    .switchIfEmpty(Mono.error(new InvalidReplyException("reply not present")))
                    .onErrorResume(this::resume);
        }

        @Override
        public <T, R> Mono<Reply<R>> exchange(String cellName, T payload) {
            return Mono.fromCallable(() -> Tuples.of(cellName, payload))
                    .flatMap(this::doExchange)
                    .map(x -> (Reply<R>) x)
                    .switchIfEmpty(Mono.error(new InvalidReplyException("reply not present")))
                    .onErrorResume(this::resume);
        }

        @Override
        public Mono<InetSocketAddress> discover(URI uri) {
            Mono<InetSocketAddress> endpoint = Flux.fromIterable(explorers)
                    .flatMap(x -> x.discover(uri))
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
                    .flatMap(r -> r.resolve(name))
                    .next();
        }

        private <R> Mono<Reply<R>> doExchange(Command<R> command) {
            return doLocalExchange(command)
                    .switchIfEmpty(wrapCallable(() -> doRemoteExchange(command)));
        }

        private <T, R> Mono<Reply<R>> doExchange(Tuple2<String, T> cellNameAndPayload) {
            String cellName = cellNameAndPayload.getT1();
            T payload = cellNameAndPayload.getT2();
            Mono<Unit> unit = Flux.fromIterable(units)
                    .filter(x -> Objects.equals(cellName, x.name))
                    .next()
                    .switchIfEmpty(Mono.error(new HostedCellNotExistsException(cellName)));
            Mono<Command> command = unit.flatMap(x -> x.protocol.deserialize(x.commandType, payload));
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
            URI entry = ModelUtils.getEntry((Class<? extends Command<?>>) command.getClass());
            if (entry == null) {
                throw new NativeCellNotExistsException(command);
            }
            return Flux.fromIterable(protocols)
                    .filter(x -> x.accept(entry))
                    .next()
                    .switchIfEmpty(Mono.error(new ProtocolNotExistsException(entry)))
                    .flatMap(x -> doChainedClientExchange(command, x, entry));
        }

        private <R> Mono<Reply<R>> doChainedNativeExchange(Command<R> command, Unit unit) {
            return doChainedExchange(command, NATIVE_EXCHANGE,
                    c -> unit.cell.exchange(c));
        }

        private <R> Mono<Reply<R>> doChainedClientExchange(Command<R> command, Protocol protocol, URI entry) {
            return doChainedExchange(command, CLIENT_EXCHANGE,
                    c -> (Mono<Reply<R>>) protocol.exchange(this, entry, c));
        }

        private <R> Mono<Reply<R>> doChainedServerExchange(Command command, Unit unit) {
            return doChainedExchange(command, SERVER_EXCHANGE,
                    c -> unit.cell.exchange(c));
        }

        private <R> Mono<Reply<R>> doChainedExchange(Command<R> command, Pointcut pointcut, Function<Command<R>, Mono<Reply<R>>> tail) {
            Function<Command<R>, Mono<Reply<R>>> now = tail;
            for (Interceptor interceptor : interceptors) {
                if (interceptor.getPointcuts().contains(pointcut)) {
                    if (interceptor.acceptCommandType(command.getClass())) {
                        Function<Command<R>, Mono<Reply<R>>> next = now;
                        now = x -> interceptor.intercept(x, pointcut, next);
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
    }
}
