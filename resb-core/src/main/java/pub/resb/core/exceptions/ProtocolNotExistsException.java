package pub.resb.core.exceptions;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

public class ProtocolNotExistsException extends GenericException {
    private URI entry;

    public ProtocolNotExistsException(URI entry) {
        super("No protocol found for entry: " + entry.toString());
        this.entry = entry;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.singletonMap("entry", entry);
    }
}
