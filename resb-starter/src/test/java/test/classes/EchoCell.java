package test.classes;

import org.springframework.stereotype.Service;
import pub.resb.reactor.interfaces.Cell;
import pub.resb.reactor.models.Reply;
import reactor.core.publisher.Mono;

@Service
public class EchoCell implements Cell<EchoCmd, String> {
    @Override
    public Mono<Reply<String>> exchange(EchoCmd command) {
        return Mono.just(Reply.of(command.getContent()));
    }
}
