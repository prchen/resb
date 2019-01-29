package test.classes;

import pub.resb.api.constants.Pointcut;
import pub.resb.api.interfaces.Interceptor;
import pub.resb.api.interfaces.ServiceBus;
import pub.resb.api.models.Command;
import pub.resb.api.models.Reply;
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

    public KeywordEchoInterceptor() {
        this(DEFAULT_PRIORITY, null, null, null, false);
    }

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
    public <C extends Command<R>, R> Mono<Reply<R>> intercept(ServiceBus serviceBus, C command, Pointcut pointcut, Function<C, Mono<Reply<R>>> next) {
        if (error) {
            throw new RuntimeException();
        }
        if (command instanceof NativeEchoCmd) {
            if (((NativeEchoCmd) command).getContent().equals(keyword)) {
                return Mono.just((Reply<R>) Reply.of(reply));
            }
        }
        return next.apply(command);
    }

    @Override
    public int getPriority() {
        return priority;
    }
}
