package pub.resb.core.exceptions;

import pub.resb.api.models.Command;

import java.util.HashMap;
import java.util.Map;

public class NativeCellNotExistsException extends GenericException {
    private Command<?> command;

    public NativeCellNotExistsException(Command<?> command) {
        super("No native Cell found for command type: " + command.getClass().getName());
        this.command = command;
    }

    @Override
    public Map<String, Object> getAttributes() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("command", command);
        attrs.put("commandType", command.getClass());
        return attrs;
    }
}
