package pub.resb.reactor.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pub.resb.reactor.interfaces.Resolver;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class BlockedSystemResolver implements Resolver {
    public static final int MAX_ATTEMPT = 2;
    private Logger logger = LoggerFactory.getLogger(BlockedSystemResolver.class);
    private final int priority;

    public BlockedSystemResolver() {
        this.priority = DEFAULT_PRIORITY;
    }

    public BlockedSystemResolver(int priority) {
        this.priority = priority;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public Mono<InetAddress> resolve(String host) {
        InetAddress address = null;
        for (int i = MAX_ATTEMPT; i > 0; i--) {
            try {
                address = InetAddress.getByName(host);
                break;
            } catch (UnknownHostException e) {
                logger.warn("Failed to resolve name: " + host, e);
            }
        }
        return address != null ? Mono.just(address) : Mono.empty();
    }
}
