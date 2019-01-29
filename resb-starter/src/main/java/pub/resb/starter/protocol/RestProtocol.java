package pub.resb.starter.protocol;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import pub.resb.api.interfaces.Protocol;
import pub.resb.api.interfaces.ServiceBus;
import pub.resb.api.models.Command;
import pub.resb.api.models.Reply;
import pub.resb.core.utils.ResbApiUtils;
import reactor.core.publisher.Mono;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

@Component
public class RestProtocol implements Protocol<ObjectNode> {
    private ObjectMapper objectMapper = new ObjectMapper();
    private WebClient webClient = WebClient.create();

    public RestProtocol() {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public boolean accept(URI entry) {
        return entry.getScheme().equals("resb+rest");
    }

    @Override
    public <C extends Command<R>, R> Mono<Reply<R>> clientExchange(ServiceBus serviceBus, URI entry, C command) {
        return serviceBus.discover(entry)
                .flatMap(x -> httpExchange(entry, x, command))
                .flatMap(x -> wrapResponse(command, x));
    }

    @Override
    public <C extends Command<R>, R> Mono<C> serverDeserialize(ServiceBus serviceBus, Class<C> commandType, ObjectNode payload) {
        return Mono.just(objectMapper.convertValue(payload, commandType));
    }


    private Mono<ClientResponse> httpExchange(URI entry, InetSocketAddress endpoint, Command command) {
        InetAddress address = endpoint.getAddress();
        String host = address instanceof Inet6Address ?
                String.format("[%s]", address.getHostAddress()) :
                address.getHostAddress();
        int port = endpoint.getPort();
        return webClient.post()
                .uri(String.format("http://%s:%s/Cells/%s", host, port, ResbApiUtils.getCellName(entry)))
                .header("Host", endpoint.getHostString() + (port == 80 ? "" : ":" + String.valueOf(port)))
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .syncBody(objectMapper.convertValue(command, ObjectNode.class))
                .exchange();
    }

    @SuppressWarnings("unchecked")
    private <C extends Command<R>, R> Mono<Reply<R>> wrapResponse(C command, ClientResponse response) {
        Class<R> resultType = (Class<R>) ResbApiUtils.getResultType((Class<? extends Command<?>>) command.getClass());
        return Mono.just(response)
                .filter(x -> x.rawStatusCode() == 200)
                .switchIfEmpty(Mono.error(new RestProtocolException(response)))
                .flatMap(x -> x.bodyToMono(ObjectNode.class))
                .map(x -> convertBody(x, resultType));
    }

    private <R> Reply<R> convertBody(ObjectNode node, Class<R> resultType) {
        if (node.findValue("success").asBoolean()) {
            return Reply.of(objectMapper.convertValue(node.findValue("result"), resultType));
        } else {
            return objectMapper.convertValue(node, new TypeReference<Reply<Object>>() {
            });
        }
    }
}
