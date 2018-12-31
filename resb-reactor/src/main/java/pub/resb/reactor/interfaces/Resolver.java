package pub.resb.reactor.interfaces;

import reactor.core.publisher.Mono;

import java.net.InetAddress;

@FunctionalInterface
public interface Resolver extends Ordered {
    Mono<InetAddress> resolve(String name);
}
