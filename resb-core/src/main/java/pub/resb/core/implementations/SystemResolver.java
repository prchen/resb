package pub.resb.core.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pub.resb.api.interfaces.Resolver;
import pub.resb.api.interfaces.ServiceBus;
import pub.resb.core.exceptions.ServiceBusNotSetException;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SystemResolver implements Resolver {
    private static final int MAX_ATTEMPT = 2;
    private Logger logger = LoggerFactory.getLogger(SystemResolver.class);
    private final int priority;

    public SystemResolver() {
        this.priority = DEFAULT_PRIORITY;
    }

    public SystemResolver(int priority) {
        this.priority = priority;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public Mono<InetAddress> resolve(ServiceBus serviceBus, String host) {
        if (serviceBus == null) {
            return Mono.error(new ServiceBusNotSetException());
        }
        return serviceBus.wrapBlocking(() -> blockedResolve(host));
    }

    private InetAddress blockedResolve(String host) {
        for (int i = MAX_ATTEMPT; i > 0; i--) {
            try {
                return InetAddress.getByName(host);
            } catch (UnknownHostException e) {
                logger.warn("Failed to resolve name: " + host, e);
            }
        }
        return null;
    }
}
