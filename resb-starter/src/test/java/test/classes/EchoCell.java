package test.classes;

import org.springframework.stereotype.Service;
import pub.resb.api.interfaces.Cell;
import pub.resb.api.interfaces.ServiceBus;
import pub.resb.api.models.Reply;
import reactor.core.publisher.Mono;

@Service
public class EchoCell implements Cell<EchoCmd, String> {
    @Override
    public Mono<Reply<String>> exchange(ServiceBus serviceBus, EchoCmd command) {
        return Mono.just(Reply.of(command.getContent()));
    }
}
