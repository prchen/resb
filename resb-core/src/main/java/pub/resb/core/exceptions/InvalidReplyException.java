package pub.resb.core.exceptions;

import pub.resb.api.models.Reply;

import java.util.Collections;
import java.util.Map;

public class InvalidReplyException extends GenericException {
    private Reply original;

    public InvalidReplyException(String message) {
        super(message);
    }

    public InvalidReplyException(String message, Reply original) {
        super(message);
        this.original = original;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.singletonMap("original", original);
    }
}
