package pub.resb.api.interfaces;

import pub.resb.api.models.Command;
import pub.resb.api.models.Reply;
import reactor.core.publisher.Mono;

import java.net.URI;

public interface Protocol<T> extends Ordered {
    boolean accept(URI entry);

    <C extends Command<R>, R> Mono<Reply<R>> clientExchange(ServiceBus serviceBus, URI entry, C command);

    <C extends Command<R>, R> Mono<C> serverDeserialize(ServiceBus serviceBus, Class<C> commandType, T payload);
}
