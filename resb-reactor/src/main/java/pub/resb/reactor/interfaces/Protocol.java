package pub.resb.reactor.interfaces;

import pub.resb.reactor.ServiceBus;
import pub.resb.reactor.models.Command;
import pub.resb.reactor.models.Reply;
import reactor.core.publisher.Mono;

import java.net.URI;

public interface Protocol<T> extends Ordered {
    boolean accept(URI entry);

    <C extends Command<R>, R> Mono<Reply<R>> exchange(ServiceBus serviceBus, URI entry, C command);

    <C extends Command<R>, R> Mono<C> deserialize(Class<C> commandType, T payload);
}
