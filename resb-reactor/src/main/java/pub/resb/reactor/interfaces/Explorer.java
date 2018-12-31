package pub.resb.reactor.interfaces;

import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;

@FunctionalInterface
public interface Explorer extends Ordered {
    Mono<InetSocketAddress> discover(URI uri);
}
