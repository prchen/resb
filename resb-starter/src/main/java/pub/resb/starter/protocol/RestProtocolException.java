package pub.resb.starter.protocol;

import org.springframework.web.reactive.function.client.ClientResponse;
import pub.resb.core.exceptions.GenericException;

import java.util.Collections;
import java.util.Map;

public class RestProtocolException extends GenericException {
    private ClientResponse response;

    public RestProtocolException(ClientResponse response) {
        super("Failed to get response");
        this.response = response;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.singletonMap("statusCode", response.statusCode());
    }
}
