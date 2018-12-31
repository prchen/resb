package test.classes;

import pub.resb.reactor.interfaces.Cell;
import pub.resb.reactor.models.Reply;
import reactor.core.publisher.Mono;

public class NativeEchoCell implements Cell<NativeEchoCmd, String> {
    @Override
    public Mono<Reply<String>> exchange(NativeEchoCmd command) {
        return Mono.just(new Reply<>(command.getContent()));
    }
}
