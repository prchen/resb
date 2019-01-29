package test.classes;

import pub.resb.api.interfaces.Cell;
import pub.resb.api.interfaces.ServiceBus;
import pub.resb.api.models.Reply;
import reactor.core.publisher.Mono;

public class NativeStreamErrCell implements Cell<NativeStreamErrCmd, Void> {
    @Override
    public Mono<Reply<Void>> exchange(ServiceBus serviceBus, NativeStreamErrCmd command) {
        int nullInteger = (int) (Integer) null;
        return Mono.just(new Reply<>());
    }
}
