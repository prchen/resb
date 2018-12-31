package pub.resb.reactor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pub.resb.reactor.constants.BuiltInError;
import pub.resb.reactor.constants.Pointcut;
import pub.resb.reactor.interfaces.*;
import pub.resb.reactor.models.Command;
import pub.resb.reactor.models.Reply;
import pub.resb.reactor.utils.ModelUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
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

    public <C extends Command<R>, R> ServiceBusBuilder cell(Cell<C, R> cell) {
        cells.add(cell);
        return this;
    }

    public ServiceBusBuilder explorer(Explorer explorer) {
        explorers.add(explorer);
        return this;
    }

    public ServiceBusBuilder resolver(Resolver resolver) {
        resolvers.add(resolver);
        return this;
    }

    public ServiceBusBuilder protocol(Protocol protocol) {
        protocols.add(protocol);
        return this;
    }

    public ServiceBusBuilder interceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
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
            URI entry = ModelUtils.getEntry((Class<? extends Command<?>>) command.getClass());
            Mono<Reply<R>> local = Flux.fromIterable(units)
                    .filter(x -> x.commandType.isAssignableFrom(command.getClass()))
                    .next()
                    .flatMap(x -> chainedNativeExchange(command, x));
            Mono<Reply<R>> remote = Flux.fromIterable(protocols)
                    .filter(x -> x.accept(entry))
                    .next()
                    .flatMap(x -> chainedClientExchange(command, x, entry));
            return local.switchIfEmpty(remote)
                    .switchIfEmpty(buildEmptyReppyError(command))
                    .onErrorResume(this::resumeExchangeError);
        }

        @Override
        public <T, R> Mono<Reply<R>> exchange(String cellName, T payload) {
            Mono<Unit> unit = Flux.fromIterable(units)
                    .filter(x -> Objects.equals(cellName, x.name))
                    .next();
            Mono<Command> command = unit.flatMap(x -> x.protocol.deserialize(x.commandType, payload));
            return Mono.zip(command, unit)
                    .flatMap(t -> chainedServerExchange(t.getT1(), t.getT2()))
                    .map(x -> (Reply<R>) x)
                    .onErrorResume(this::resumeExchangeError);
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

        private <R> Mono<Reply<R>> chainedNativeExchange(Command<R> command, Unit unit) {
            return chainedExchange(command, NATIVE_EXCHANGE,
                    c -> unit.cell.exchange(c));
        }

        private <R> Mono<Reply<R>> chainedClientExchange(Command<R> command, Protocol protocol, URI entry) {
            return chainedExchange(command, CLIENT_EXCHANGE,
                    c -> (Mono<Reply<R>>) protocol.exchange(this, entry, c));
        }

        private <R> Mono<Reply<R>> chainedServerExchange(Command command, Unit unit) {
            return chainedExchange(command, SERVER_EXCHANGE,
                    c -> unit.cell.exchange(c));
        }

        private <R> Mono<Reply<R>> chainedExchange(Command<R> command, Pointcut pointcut,
                                                   Function<Command<R>, Mono<Reply<R>>> tail) {
            Function<Command<R>, Mono<Reply<R>>> now = tail;
            for (Interceptor interceptor : interceptors) {
                if (interceptor.getPointcuts().contains(pointcut)) {
                    if (interceptor.acceptCommandType(command.getClass())) {
                        Function<Command<R>, Mono<Reply<R>>> next = now;
                        now = x -> interceptor.intercept(x, next);
                    }
                }
            }
            return now.apply(command)
                    .map(this::verifyReply);
        }

        private <T extends Reply<R>, R> T verifyReply(T reply) {
            boolean valid = true;
            if (reply.getSuccess() == null) {
                valid = false;
            } else if (!reply.getSuccess() && (reply.getErrorName() == null || reply.getErrorAttrs() == null)) {
                valid = false;
            }
            if (!valid) {
                return (T) BuiltInError.BUILTIN_UNRECOGNIZED_REPLY.toBuilder()
                        .attr("original", reply)
                        .build();
            } else {
                return reply;
            }
        }

        private <R> Mono<Reply<R>> buildEmptyReppyError(Command command) {
            return BuiltInError.BUILTIN_REPLY_NOT_PRESENT
                    .toBuilder()
                    .attr("command", command)
                    .attr("commandType", command.getClass())
                    .buildMono();
        }

        private <R> Mono<Reply<R>> resumeExchangeError(Throwable e) {
            logger.error("Failed to execute exchange method", e);
            return BuiltInError.BUILTIN_UNEXPECTED_EXCEPTION
                    .toBuilder()
                    .attr("message", e.getMessage())
                    .attr("errorType", e.getClass().getName())
                    .buildMono();
        }
    }
}
