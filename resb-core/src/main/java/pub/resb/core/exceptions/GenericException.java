package pub.resb.core.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pub.resb.api.models.Catchable;
import pub.resb.api.models.Reply;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;

public abstract class GenericException extends RuntimeException implements Catchable {
    protected Logger logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    protected GenericException(String message) {
        super(message);
    }

    protected GenericException(String message, Throwable cause) {
        super(message, cause);
    }

    protected GenericException(Throwable cause) {
        super(cause != null ? cause.getClass().getName() + ": " + cause.getMessage() : null, cause);
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    public Map<String, Object> getAttributes() {
        return Collections.emptyMap();
    }

    @Override
    public <R> Mono<Reply<R>> resume() {
        return Reply.fromError(getName())
                .attr("type", getClass().getName())
                .attr("message", getMessage())
                .attr("stacktrace", formatStackTrace())
                .attrs(getAttributes())
                .buildMono();
    }

    private String formatStackTrace() {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            try (OutputStreamWriter writer = new OutputStreamWriter(buffer)) {
                try (PrintWriter printer = new PrintWriter(writer)) {
                    printStackTrace(printer);
                    printer.flush();
                    writer.flush();
                    buffer.flush();
                    return buffer.toString();
                }
            }
        } catch (Exception e) {
            logger.error("Failed to format stacktrace", e);
            return "";
        }
    }
}
