package pub.resb.api.interfaces;

import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;

public interface Explorer extends Ordered {
    Mono<InetSocketAddress> discover(ServiceBus serviceBus, URI uri);
}
