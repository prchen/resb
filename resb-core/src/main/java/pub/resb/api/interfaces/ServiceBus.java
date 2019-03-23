package pub.resb.api.interfaces;

import pub.resb.api.models.Command;
import pub.resb.api.models.Reply;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.function.Supplier;

public interface ServiceBus {
    <R> Mono<Reply<R>> exchange(Command<R> command);

    <T, R> Mono<Reply<R>> serverExchange(String cellName, T payload);

    Mono<InetSocketAddress> discover(URI uri);

    Mono<InetAddress> resolve(String name);

    <T> Mono<T> wrapBlocking(Supplier<T> supplier);

    void shutdown();
}
