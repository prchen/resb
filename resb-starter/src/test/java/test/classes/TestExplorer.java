package test.classes;

import pub.resb.api.interfaces.Explorer;
import pub.resb.api.interfaces.ServiceBus;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;

public class TestExplorer implements Explorer {
    private int port;

    public TestExplorer(int port) {
        this.port = port;
    }

    @Override
    public Mono<InetSocketAddress> discover(ServiceBus serviceBus, URI uri) {
        try {
            return Mono.just(new InetSocketAddress(InetAddress.getLocalHost(), port));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
