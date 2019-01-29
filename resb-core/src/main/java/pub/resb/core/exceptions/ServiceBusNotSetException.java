package pub.resb.core.exceptions;

public class ServiceBusNotSetException extends GenericException {
    public ServiceBusNotSetException() {
        super("ServiceBus should be set");
    }
}
