package pub.resb.sample.helloworld;

import pub.resb.reactor.ServiceBus;
import pub.resb.reactor.ServiceBusBuilder;
import pub.resb.reactor.models.Reply;

public class HelloWorldMain {
    private static final ServiceBus serviceBus;

    static {
        serviceBus = new ServiceBusBuilder()
                .cell(new HelloWorldCell())
                .build();
    }

    public static void main(String... args) {
        serviceBus.exchange(new HelloWorldCommand())
                .map(Reply::getResult)
                .subscribe(System.out::println);
    }
}
