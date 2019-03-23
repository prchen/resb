package pub.resb.sample.helloworld;

import pub.resb.core.DefaultServiceBus;
import pub.resb.api.interfaces.ServiceBus;
import pub.resb.api.models.Reply;

public class HelloWorldMain {
    private static final ServiceBus serviceBus;

    static {
        serviceBus = DefaultServiceBus.builder()
                .cell(new HelloWorldCell())
                .build();
    }

    public static void main(String... args) {
        serviceBus.exchange(new HelloWorldCommand())
                .map(Reply::getResult)
                .subscribe(System.out::println);
    }
}
