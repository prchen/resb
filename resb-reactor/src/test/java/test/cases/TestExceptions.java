package test.cases;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pub.resb.reactor.ServiceBus;
import pub.resb.reactor.constants.Pointcut;
import pub.resb.reactor.exceptions.NativeCellNotExistsException;
import pub.resb.reactor.exceptions.UnexpectedException;
import pub.resb.reactor.models.Reply;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import test.classes.*;

@RunWith(JUnit4.class)
public class TestExceptions {

    @Test
    public void testEmptyReply() {
        ServiceBus bus = ServiceBus.builder()
                .build();
        Mono<Reply<Void>> result = Mono.just(new WanderedCmd())
                .flatMap(bus::exchange);
        StepVerifier.create(result)
                .assertNext(x -> {
                    Assert.assertNotNull(x.getSuccess());
                    Assert.assertFalse(x.getSuccess());
                    Assert.assertEquals(NativeCellNotExistsException.class.getSimpleName(), x.getErrorName());
                    Assert.assertNotNull(x.getErrorAttrs());
                    Assert.assertFalse(x.getErrorAttrs().isEmpty());
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void testStreamError() {
        ServiceBus bus = ServiceBus.builder()
                .cell(new NativeStreamErrCell())
                .build();
        Mono<Reply<Void>> result = Mono.just(new NativeStreamErrCmd())
                .flatMap(bus::exchange);
        StepVerifier.create(result)
                .assertNext(x -> {
                    Assert.assertNotNull(x.getSuccess());
                    Assert.assertFalse(x.getSuccess());
                    Assert.assertEquals(UnexpectedException.class.getSimpleName(), x.getErrorName());
                    Assert.assertNotNull(x.getErrorAttrs());
                    Assert.assertFalse(x.getErrorAttrs().isEmpty());
                })
                .expectComplete()
                .verify();
    }

    @Test
    public void testInterceptorThrow() {
        ServiceBus bus = ServiceBus.builder()
                .cell(new NativeEchoCell())
                .interceptor(new KeywordEchoInterceptor(0, Pointcut.NATIVE_EXCHANGE, null, null, true))
                .build();
        Mono<Reply<String>> result = Mono.just(new NativeEchoCmd("Whatever"))
                .flatMap(bus::exchange);
        StepVerifier.create(result)
                .assertNext(x -> {
                    Assert.assertNotNull(x.getSuccess());
                    Assert.assertFalse(x.getSuccess());
                    Assert.assertEquals(UnexpectedException.class.getSimpleName(), x.getErrorName());
                    Assert.assertNotNull(x.getErrorAttrs());
                    Assert.assertFalse(x.getErrorAttrs().isEmpty());
                })
                .expectComplete()
                .verify();
    }


}
