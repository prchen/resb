package pub.resb.reactor.models;

import reactor.core.publisher.Mono;

public interface Catchable {
    <R> Mono<Reply<R>> resume();
}
