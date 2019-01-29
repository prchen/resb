package pub.resb.api.models;

import pub.resb.api.interfaces.ServiceBus;
import reactor.core.publisher.Mono;

import java.io.Serializable;

public interface Command<R> extends Serializable {

    default Mono<Reply<R>> exchange(ServiceBus serviceBus) {
        return serviceBus.exchange(this);
    }
}
