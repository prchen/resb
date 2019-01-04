package pub.resb.sample.helloworld;

import pub.resb.reactor.interfaces.Cell;
import pub.resb.reactor.models.Reply;
import reactor.core.publisher.Mono;

public class HelloWorldCell implements Cell<HelloWorldCommand, String> {
    @Override
    public Mono<Reply<String>> exchange(HelloWorldCommand command) {
        return Mono.just(Reply.of("Hello World!"));
    }
}
