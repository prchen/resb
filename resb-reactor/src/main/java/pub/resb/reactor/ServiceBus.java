package pub.resb.reactor;

import pub.resb.reactor.models.Command;
import pub.resb.reactor.models.Reply;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

public interface ServiceBus {
    <R> Mono<Reply<R>> exchange(Command<R> command);

    <T, R> Mono<Reply<R>> exchange(String cellName, T payload);

    Mono<InetSocketAddress> discover(URI uri);

    Mono<InetAddress> resolve(String name);
}
