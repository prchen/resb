package test.caes;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pub.resb.reactor.ServiceBus;
import pub.resb.reactor.ServiceBusBuilder;
import pub.resb.reactor.constants.BuiltInError;
import pub.resb.reactor.constants.Pointcut;
import pub.resb.reactor.implementations.BlockedSystemResolver;
import pub.resb.reactor.implementations.HostAndPortExplorer;
import pub.resb.reactor.interfaces.Ordered;
import pub.resb.reactor.models.Reply;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import test.classes.*;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

@RunWith(JUnit4.class)
public class AllInOneTestCase {

    @Test
    public void testReplyModel() {
        String RESULT_1 = "RESULT_1";
        String RESULT_2 = "RESULT_2";
        Reply<String> reply1 = new Reply<>(RESULT_1);
        Reply<String> reply2 = new Reply<>(RESULT_1);
        Reply<String> reply3 = new Reply<>(RESULT_2);

        Assert.assertTrue(StringUtils.isNoneBlank(reply1.toString()));
        Assert.assertEquals(reply1, reply2);
        Assert.assertEquals(reply1.hashCode(), reply2.hashCode());
        Assert.assertNotEquals(reply1, reply3);
    }

    @Test
    public void testDefault() {
        Mono<Integer> result = Mono.just(new Ordered() {
        })
                .map(Ordered::getPriority);
        StepVerifier.create(result)
                .expectNext(Ordered.DEFAULT_PRIORITY)
                .expectComplete()
                .verify();
    }

    @Test
    public void testAscSort() {
        Flux<Integer> result = Flux.fromArray(OrderedObject.PRIORITIES)
                .collectList()
                .doOnNext(Collections::shuffle)
                .flatMapIterable(x -> x)
                .map(OrderedObject::new)
                .collectList()
                .map(x -> Ordered.adjust(x, true))
                .flatMapIterable(x -> x)
                .map(OrderedObject::getPriority);
        StepVerifier.create(result)
                .expectNext(OrderedObject.PRIORITIES)
                .expectComplete()
                .verify();
    }

    @Test
    public void testDescSort() {
        Integer[] reversed = Arrays.copyOf(OrderedObject.PRIORITIES, OrderedObject.PRIORITIES.length);
        ArrayUtils.reverse(reversed);
        Flux<Integer> result = Flux.fromArray(OrderedObject.PRIORITIES)
                .collectList()
                .doOnNext(Collections::shuffle)
                .flatMapIterable(x -> x)
                .map(OrderedObject::new)
                .collectList()
                .map(x -> Ordered.adjust(x, false))
                .flatMapIterable(x -> x)
                .map(OrderedObject::getPriority);
        StepVerifier.create(result)
                .expectNext(reversed)
                .expectComplete()
                .verify();
    }

    @Test
    public void testBuildBus() {
        new ServiceBusBuilder()
                .cell(new NativeEchoCell())
                .explorer(new HostAndPortExplorer())
                .resolver(new BlockedSystemResolver())
                .cell(new NativeEchoCell())
                .build();
    }

    @Test
    public void testNativeEcho() {
        ServiceBus bus = new ServiceBusBuilder()
                .cell(new NativeEchoCell())
                .build();
        String content = RandomStringUtils.randomNumeric(128);
        Mono<String> result = Mono.just(new NativeEchoCmd(content))
                .flatMap(bus::exchange)
                .map(Reply::getResult);
        StepVerifier.create(result)
                .expectNext(content)
                .expectComplete()
                .verify();
    }

    @Test
    public void testEmptyReply() {
        ServiceBus bus = new ServiceBusBuilder()
                .build();
        Mono<Reply<Void>> result = Mono.just(new WanderedCmd())
                .flatMap(bus::exchange);
        StepVerifier.create(result)
                .assertNext(x -> {
                    Assert.assertNotNull(x.getSuccess());
                    Assert.assertFalse(x.getSuccess());
                    Assert.assertEquals(BuiltInError.BUILTIN_REPLY_NOT_PRESENT.name(), x.getErrorName());
                    Assert.assertNotNull(x.getErrorAttrs());
                    Assert.assertNotNull(x.getErrorAttrs().get("command"));
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void testStreamError() {
        ServiceBus bus = new ServiceBusBuilder()
                .cell(new NativeStreamErrCell())
                .build();
        Mono<Reply<Void>> result = Mono.just(new NativeStreamErrCmd())
                .flatMap(bus::exchange);
        StepVerifier.create(result)
                .assertNext(x -> {
                    Assert.assertNotNull(x.getSuccess());
                    Assert.assertFalse(x.getSuccess());
                    Assert.assertEquals(BuiltInError.BUILTIN_UNEXPECTED_EXCEPTION.name(), x.getErrorName());
                    Assert.assertNotNull(x.getErrorAttrs());
                    Assert.assertTrue(x.getErrorAttrs().containsKey("message"));
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void testRpcEcho() {
        RemoteEchoProtocol protocol = new RemoteEchoProtocol();
        ServiceBus clientBus = new ServiceBusBuilder()
                .explorer(x -> Mono.just(new InetSocketAddress("localhost", 8080)))
                .protocol(protocol)
                .build();
        ServiceBus serverBus = new ServiceBusBuilder()
                .cell(new RemoteEchoCell())
                .protocol(protocol)
                .build();
        protocol.setServerBus(serverBus);
        String content = RandomStringUtils.randomNumeric(128);
        Mono<String> result = Mono.just(new RemoteEchoCmd())
                .doOnNext(x -> x.setContent(content))
                .flatMap(clientBus::exchange)
                .map(Reply::getResult);
        StepVerifier.create(result)
                .expectNext(content)
                .expectComplete()
                .verify();
    }

    @Test
    public void testInterceptorThrow() {
        ServiceBus bus = new ServiceBusBuilder()
                .cell(new NativeEchoCell())
                .interceptor(new KeywordEchoInterceptor(0, Pointcut.NATIVE_EXCHANGE, null, null, true))
                .build();
        Mono<Reply<String>> result = Mono.just(new NativeEchoCmd("Whatever"))
                .flatMap(bus::exchange);
        StepVerifier.create(result)
                .assertNext(x -> {
                    Assert.assertNotNull(x.getSuccess());
                    Assert.assertFalse(x.getSuccess());
                    Assert.assertEquals(BuiltInError.BUILTIN_UNEXPECTED_EXCEPTION.name(), x.getErrorName());
                    Assert.assertNotNull(x.getErrorAttrs());
                    Assert.assertTrue(x.getErrorAttrs().containsKey("message"));
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void testInterceptorInOrder() {
        String KEY1 = "FOO_A";
        String VAL1 = "BAR_A";
        String KEY2 = "FOO_B";
        String VAL2 = "BAR_B";
        ServiceBus bus = new ServiceBusBuilder()
                .cell(new NativeEchoCell())
                .interceptor(new KeywordEchoInterceptor(-1, Pointcut.NATIVE_EXCHANGE, KEY1, VAL1, false))
                .interceptor(new KeywordEchoInterceptor(0, Pointcut.NATIVE_EXCHANGE, KEY2, VAL2, false))
                .interceptor(new KeywordEchoInterceptor(1, Pointcut.NATIVE_EXCHANGE, null, null, true))
                .build();
        Mono<Reply<String>> result1 = Mono.just(new NativeEchoCmd(KEY1))
                .flatMap(bus::exchange);
        StepVerifier.create(result1)
                .assertNext(x -> {
                    Assert.assertTrue(x.getSuccess());
                    Assert.assertEquals(VAL1, x.getResult());
                })
                .expectComplete()
                .verify();
        Mono<Reply<String>> result2 = Mono.just(new NativeEchoCmd(KEY2))
                .flatMap(bus::exchange);
        StepVerifier.create(result2)
                .assertNext(x -> {
                    Assert.assertTrue(x.getSuccess());
                    Assert.assertEquals(VAL2, x.getResult());
                })
                .expectComplete()
                .verify();
    }

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

        ServiceBus bus = new ServiceBusBuilder()
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
