package test.classes;

import pub.resb.api.interfaces.Cell;
import pub.resb.api.interfaces.ServiceBus;
import pub.resb.api.models.Reply;
import reactor.core.publisher.Mono;

public class RemoteEchoCell implements Cell<RemoteEchoCmd, String> {

    @Override
    public Mono<Reply<String>> exchange(ServiceBus serviceBus, RemoteEchoCmd command) {
        return Mono.just(Reply.of(command.getContent()));
    }
}
