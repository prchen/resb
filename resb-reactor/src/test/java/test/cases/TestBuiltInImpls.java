package test.cases;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pub.resb.reactor.ServiceBus;
import pub.resb.reactor.implementations.BlockedSystemResolver;
import pub.resb.reactor.implementations.HostAndPortExplorer;
import pub.resb.reactor.interfaces.Ordered;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.UUID;

@RunWith(JUnit4.class)
public class TestBuiltInImpls {

    @Test
    public void testHostAndPortExplorerConstructions() {
        int random = new Random().nextInt();
        HostAndPortExplorer explorer1 = new HostAndPortExplorer();
        HostAndPortExplorer explorer2 = new HostAndPortExplorer(random);
        Assert.assertEquals(Ordered.DEFAULT_PRIORITY, explorer1.getPriority());
        Assert.assertEquals(random, explorer2.getPriority());
    }

    @Test
    public void testHostAndPortExplore() throws UnknownHostException {
        InetAddress localhost = InetAddress.getLocalHost();
        String ipv4LocalStr = "127.0.0.1";
        String ipv6LocalStr = "::1";
        byte[] ipv6LocalAddr = new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1};
        Assert.assertTrue(localhost.getHostName().matches("^\\w+$"));
        HostAndPortExplorer explorer = new HostAndPortExplorer();
        Mono<InetSocketAddress> result;
        result = explorer.discover(URI.create("http://" + localhost.getHostName() + ":8080"));
        StepVerifier.create(result)
                .assertNext(x -> {
                    Assert.assertTrue(x.isUnresolved());
                    Assert.assertEquals(localhost.getHostName(), x.getHostString());
                    Assert.assertEquals(8080, x.getPort());
                })
                .expectComplete()
                .verify();
        result = explorer.discover(URI.create("http://" + ipv4LocalStr + ":8080"));
        StepVerifier.create(result)
                .assertNext(x -> {
                    Assert.assertFalse(x.isUnresolved());
                    Assert.assertEquals(ipv4LocalStr, x.getHostString());
                    Assert.assertEquals(8080, x.getPort());
                })
                .expectComplete()
                .verify();
        result = explorer.discover(URI.create("http://[" + ipv6LocalStr + "]:8080"));
        StepVerifier.create(result)
                .assertNext(x -> {
                    Assert.assertFalse(x.isUnresolved());
                    Assert.assertArrayEquals(ipv6LocalAddr, x.getAddress().getAddress());
                    Assert.assertEquals(8080, x.getPort());
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void testBlockedSystemResolverConstructions() {
        int random = new Random().nextInt();

        BlockedSystemResolver resolver1 = new BlockedSystemResolver();
        BlockedSystemResolver resolver2 = new BlockedSystemResolver(random);

        Assert.assertEquals(Ordered.DEFAULT_PRIORITY, resolver1.getPriority());
        Assert.assertEquals(random, resolver2.getPriority());
    }

    @Test
    public void testBlockSystemResolve() throws UnknownHostException {
        InetAddress localhost = InetAddress.getLocalHost();
        Assert.assertTrue(localhost.getHostName().matches("^\\w+$"));

        BlockedSystemResolver resolver = new BlockedSystemResolver();

        Mono<InetAddress> result = resolver.resolve(localhost.getHostName());

        StepVerifier.create(result)
                .expectNext(localhost)
                .expectComplete()
                .verify();

        Mono<InetAddress> result2 = resolver.resolve(UUID.randomUUID().toString());

        StepVerifier.create(result2)
                .expectComplete()
                .verify();
    }

    @Test
    public void testHostPortDiscover() throws UnknownHostException {
        InetAddress localhost = InetAddress.getLocalHost();

        Assert.assertTrue(localhost.getHostName().matches("^\\w+$"));

        ServiceBus bus = ServiceBus.builder()
                .explorer(new HostAndPortExplorer())
                .resolver(new BlockedSystemResolver())
                .build();

        Mono<InetSocketAddress> result;

        result = bus.discover(URI.create("http://" + localhost.getHostName() + ":8080/Foo"));

        StepVerifier.create(result)
                .assertNext(x -> {
                    Assert.assertFalse(x.isUnresolved());
                    Assert.assertEquals(localhost.getHostName(), x.getHostString());
                    Assert.assertEquals(8080, x.getPort());
                    Assert.assertEquals(localhost.getHostAddress(), x.getAddress().getHostAddress());
                })
                .expectComplete()
                .verify();
    }
}
