package test.cases;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pub.resb.api.constants.Pointcut;
import pub.resb.api.interfaces.Ordered;
import pub.resb.api.interfaces.ServiceBus;
import pub.resb.api.models.Reply;
import pub.resb.core.DefaultServiceBus;
import pub.resb.core.implementations.HostAndPortExplorer;
import pub.resb.core.implementations.SystemResolver;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import test.classes.*;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;

@RunWith(JUnit4.class)
public class TestFunctions {

    @Test
    public void testReplyModel() {
        String RESULT_1 = "RESULT_1";
        String RESULT_2 = "RESULT_2";
        Reply<String> reply1 = Reply.of(RESULT_1);
        Reply<String> reply2 = Reply.of(RESULT_1);
        Reply<String> reply3 = Reply.of(RESULT_2);

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
        DefaultServiceBus.builder()
                .cell(new NativeEchoCell())
                .explorer(new HostAndPortExplorer())
                .resolver(new SystemResolver())
                .cell(new NativeEchoCell())
                .protocol(new RemoteEchoProtocol())
                .interceptor(new KeywordEchoInterceptor())
                .build();
    }

    @Test
    public void testBuildBusByCollections() {
        DefaultServiceBus.builder()
                .cells(Collections.singleton(new NativeEchoCell()))
                .explorers(Collections.singleton(new HostAndPortExplorer()))
                .resolvers(Collections.singleton(new SystemResolver()))
                .cells(Collections.singleton(new NativeEchoCell()))
                .protocols(Collections.singleton(new RemoteEchoProtocol()))
                .interceptors(Collections.singleton(new KeywordEchoInterceptor()))
                .build();
    }

    @Test
    public void testNativeEcho() {
        ServiceBus bus = DefaultServiceBus.builder()
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
    public void testRpcEcho() {
        RemoteEchoProtocol protocol = new RemoteEchoProtocol();
        ServiceBus clientBus = DefaultServiceBus.builder()
                .explorer((b,x) -> Mono.just(new InetSocketAddress("localhost", 8080)))
                .protocol(protocol)
                .build();
        ServiceBus serverBus = DefaultServiceBus.builder()
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
    public void testInterceptorInOrder() {
        String KEY1 = "FOO_A";
        String VAL1 = "BAR_A";
        String KEY2 = "FOO_B";
        String VAL2 = "BAR_B";
        ServiceBus bus = DefaultServiceBus.builder()
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
}
