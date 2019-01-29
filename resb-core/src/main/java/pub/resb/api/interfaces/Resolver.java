package pub.resb.api.interfaces;

import reactor.core.publisher.Mono;

import java.net.InetAddress;

public interface Resolver extends Ordered {
    Mono<InetAddress> resolve(ServiceBus serviceBus, String name);
}
