package test.classes;

import pub.resb.reactor.interfaces.Cell;
import pub.resb.reactor.models.Reply;
import reactor.core.publisher.Mono;

public class NativeStreamErrCell implements Cell<NativeStreamErrCmd, Void> {
    @Override
    public Mono<Reply<Void>> exchange(NativeStreamErrCmd command) {
        int nullInteger = (int) (Integer) null;
        return Mono.just(new Reply<>());
    }
}
