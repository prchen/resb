package pub.resb.reactor.interfaces;

import pub.resb.reactor.models.Command;
import pub.resb.reactor.models.Reply;
import reactor.core.publisher.Mono;

public interface Cell<C extends Command<R>, R> extends Ordered {
    Mono<Reply<R>> exchange(C command);
}
