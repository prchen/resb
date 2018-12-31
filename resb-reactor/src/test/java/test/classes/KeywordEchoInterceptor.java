package test.classes;

import pub.resb.reactor.constants.Pointcut;
import pub.resb.reactor.interfaces.Interceptor;
import pub.resb.reactor.models.Command;
import pub.resb.reactor.models.Reply;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KeywordEchoInterceptor implements Interceptor {
    private int priority;
    private Pointcut pointcut;
    private String keyword;
    private String reply;
    private boolean error;

    public KeywordEchoInterceptor(int priority, Pointcut pointcut, String keyword, String reply, boolean error) {
        this.priority = priority;
        this.pointcut = pointcut;
        this.keyword = keyword;
        this.reply = reply;
        this.error = error;
    }

    @Override
    public <C extends Command<R>, R> boolean acceptCommandType(Class<C> commandType) {
        return commandType.isAssignableFrom(NativeEchoCmd.class);
    }

    @Override
    public Set<Pointcut> getPointcuts() {
        return Stream.of(pointcut)
                .collect(Collectors.toSet());
    }

    @Override
    public <C extends Command<R>, R> Mono<Reply<R>> intercept(C command, Function<C, Mono<Reply<R>>> next) {
        if (error) {
            throw new RuntimeException();
        }
        if (command instanceof NativeEchoCmd) {
            if (((NativeEchoCmd) command).getContent().equals(keyword)) {
                return Mono.just((Reply<R>) new Reply<>(reply));
            }
        }
        return next.apply(command);
    }

    @Override
    public int getPriority() {
        return priority;
    }
}
