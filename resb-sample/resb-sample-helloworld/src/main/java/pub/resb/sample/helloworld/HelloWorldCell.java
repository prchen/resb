package pub.resb.sample.helloworld;

import pub.resb.api.interfaces.Cell;
import pub.resb.api.interfaces.ServiceBus;
import pub.resb.api.models.Reply;
import reactor.core.publisher.Mono;

public class HelloWorldCell implements Cell<HelloWorldCommand, String> {
    @Override
    public Mono<Reply<String>> exchange(ServiceBus serviceBus, HelloWorldCommand command) {
        return Mono.just(Reply.of("Hello World!"));
    }
}
