package pub.resb.reactor.implementations;

import pub.resb.reactor.interfaces.Explorer;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;

public class HostAndPortExplorer implements Explorer {
    private final int priority;

    public HostAndPortExplorer() {
        this.priority = DEFAULT_PRIORITY;
    }

    public HostAndPortExplorer(int priority) {
        this.priority = priority;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public Mono<InetSocketAddress> discover(URI uri) {
        String host = uri.getHost();
        int port = uri.getPort();
        if (host == null || port < 0 || host.matches("\\s+")) {
            return Mono.empty();
        } else if (host.matches("^\\[.*]$")) {
            // IPv6 Address
            return Mono.just(new InetSocketAddress(host.substring(1, host.length() - 1), port));
        } else if (host.matches(".*\\.\\d+$")) {
            // IPv4 Address
            return Mono.just(new InetSocketAddress(host, port));
        } else {
            // Hostname
            return Mono.just(InetSocketAddress.createUnresolved(host, port));
        }
    }
}
