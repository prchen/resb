package pub.resb.reactor.exceptions;

import java.util.Collections;
import java.util.Map;

public class HostedCellNotExistsException extends GenericException {
    private String cellName;

    public HostedCellNotExistsException(String cellName) {
        super("Hosted cell not found: " + cellName);
        this.cellName = cellName;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return Collections.singletonMap("cellName", cellName);
    }
}
