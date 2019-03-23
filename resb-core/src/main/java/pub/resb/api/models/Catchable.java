package pub.resb.api.models;

import reactor.core.publisher.Mono;

public interface Catchable {
    <R> Mono<Reply<R>> resume();
}
