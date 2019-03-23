package test.classes;

import pub.resb.api.interfaces.Cell;
import pub.resb.api.interfaces.ServiceBus;
import pub.resb.api.models.Reply;
import reactor.core.publisher.Mono;

public class NativeEchoCell implements Cell<NativeEchoCmd, String> {
    @Override
    public Mono<Reply<String>> exchange(ServiceBus serviceBus, NativeEchoCmd command) {
        return Mono.just(Reply.of(command.getContent()));
    }
}
