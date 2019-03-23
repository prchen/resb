package pub.resb.api.interfaces;

import pub.resb.api.models.Command;
import pub.resb.api.models.Reply;
import pub.resb.core.utils.ResbApiUtils;
import reactor.core.publisher.Mono;

public interface Cell<C extends Command<R>, R> extends Ordered {
    @SuppressWarnings("unchecked")
    default Class<C> getCommandType() {
        return (Class<C>) ResbApiUtils.getCommandType((Class<? extends Cell<?, ?>>) getClass());
    }

    Mono<Reply<R>> exchange(ServiceBus serviceBus, C command);
}
