package pub.resb.starter.protocol;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pub.resb.reactor.ServiceBus;
import pub.resb.reactor.models.Reply;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class RestProtocolController {
    @Autowired
    private ServiceBus serviceBus;

    @PostMapping("/Cells/{cellName}")
    public <R> Mono<Reply<R>> call(@PathVariable String cellName, @RequestBody ObjectNode json) {
        return serviceBus.exchange(cellName, json);
    }
}
