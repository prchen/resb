package test.cases;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import pub.resb.reactor.ServiceBus;
import pub.resb.reactor.implementations.BlockedSystemResolver;
import pub.resb.reactor.models.Reply;
import pub.resb.starter.protocol.RestProtocol;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import test.classes.EchoCmd;
import test.classes.TestExplorer;
import test.classes.TextConf;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TextConf.class)
public class AllInOneTestCase {
    @Autowired(required = false)
    private ServiceBus serviceBus;
    @Autowired(required = false)
    private RestProtocol protocol;
    @LocalServerPort
    private int port;

    private ServiceBus clientBus;

    @Before
    public void initClient() {
        clientBus = ServiceBus.builder()
                .explorer(new TestExplorer(port))
                .resolver(new BlockedSystemResolver())
                .protocol(protocol)
                .build();
    }

    @Test
    public void testBootstrap() {
        Assert.assertNotNull(serviceBus);
        Assert.assertNotNull(protocol);
        Assert.assertNotNull(clientBus);
    }

    @Test
    public void testEcho() {
        String text = RandomStringUtils.randomAlphanumeric(128);
        Mono<Reply<String>> result = clientBus.exchange(new EchoCmd(text));
        StepVerifier.create(result)
                .assertNext(x -> {
                    Assert.assertNotNull(x.getSuccess());
                    Assert.assertTrue(x.getSuccess());
                    Assert.assertNotNull(x.getResult());
                    Assert.assertEquals(text, x.getResult());
                })
                .expectComplete()
                .verify();
    }
}
