package pub.resb.api.interfaces;

import pub.resb.api.constants.Pointcut;
import pub.resb.api.models.Command;
import pub.resb.api.models.Reply;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.function.Function;

public interface Interceptor extends Ordered {

    <C extends Command<R>, R> boolean acceptCommandType(Class<C> commandType);

    Set<Pointcut> getPointcuts();

    <C extends Command<R>, R> Mono<Reply<R>> intercept(ServiceBus serviceBus, C command, Pointcut pointcut, Function<C, Mono<Reply<R>>> next);
}
