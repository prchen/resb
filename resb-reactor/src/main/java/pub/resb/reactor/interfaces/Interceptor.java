package pub.resb.reactor.interfaces;

import pub.resb.reactor.constants.Pointcut;
import pub.resb.reactor.models.Command;
import pub.resb.reactor.models.Reply;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.function.Function;

public interface Interceptor extends Ordered {

    <C extends Command<R>, R> boolean acceptCommandType(Class<C> commandType);

    Set<Pointcut> getPointcuts();

    <C extends Command<R>, R> Mono<Reply<R>> intercept(C command, Pointcut pointcut, Function<C, Mono<Reply<R>>> next);

}
