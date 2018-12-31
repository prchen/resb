package test.classes;

import pub.resb.reactor.interfaces.Cell;
import pub.resb.reactor.models.Reply;
import reactor.core.publisher.Mono;

public class RemoteEchoCell implements Cell<RemoteEchoCmd, String> {

    @Override
    public Mono<Reply<String>> exchange(RemoteEchoCmd command) {
        return Mono.just(new Reply<>(command.getContent()));
    }
}
