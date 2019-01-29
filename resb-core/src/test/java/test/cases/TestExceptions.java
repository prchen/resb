package test.cases;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import pub.resb.api.constants.Pointcut;
import pub.resb.api.interfaces.ServiceBus;
import pub.resb.api.models.Reply;
import pub.resb.core.DefaultServiceBus;
import pub.resb.core.exceptions.NativeCellNotExistsException;
import pub.resb.core.exceptions.UnexpectedException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import test.classes.*;

@RunWith(JUnit4.class)
public class TestExceptions {

    @Test
    public void testEmptyReply() {
        ServiceBus bus = DefaultServiceBus.builder()
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
        ServiceBus bus = DefaultServiceBus.builder()
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
        ServiceBus bus = DefaultServiceBus.builder()
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
