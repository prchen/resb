package pub.resb.reactor.models;

import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Reply<T> implements Serializable {
    private Boolean success;
    private String errorName;
    private Map<String, String> errorAttrs;
    private T result;

    public Reply() {
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorName() {
        return errorName;
    }

    public void setErrorName(String errorName) {
        this.errorName = errorName;
    }

    public Map<String, String> getErrorAttrs() {
        return errorAttrs;
    }

    public void setErrorAttrs(Map<String, String> errorAttrs) {
        this.errorAttrs = errorAttrs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reply<?> reply = (Reply<?>) o;
        return Objects.equals(result, reply.result) &&
                Objects.equals(success, reply.success) &&
                Objects.equals(errorName, reply.errorName) &&
                Objects.equals(errorAttrs, reply.errorAttrs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, success, errorName, errorAttrs);
    }

    @Override
    public String toString() {
        return "Reply{" +
                "result=" + result +
                ", success=" + success +
                ", errorName='" + errorName + '\'' +
                ", errorAttrs=" + errorAttrs +
                '}';
    }

    public static <T> Reply<T> of(T result) {
        Reply<T> reply = new Reply<>();
        reply.setSuccess(true);
        reply.setResult(result);
        return reply;
    }

    public static ErrorBuilder fromError(String name) {
        return new ErrorBuilder(name);
    }

    @SuppressWarnings("unchecked")
    public static class ErrorBuilder {
        private Reply reply;

        private ErrorBuilder(String name) {
            reply = new Reply();
            reply.setSuccess(false);
            reply.setErrorName(name);
            reply.setErrorAttrs(new HashMap<>());
        }

        public ErrorBuilder attr(String name, Object value) {
            reply.getErrorAttrs().put(name, value != null ? value.toString() : null);
            return this;
        }

        public ErrorBuilder attrs(Map<String, Object> attrs) {
            if (attrs != null) {
                attrs.forEach((key, value) -> reply.getErrorAttrs().put(key, value != null ? value.toString() : null));
            }
            return this;
        }

        public <R> Reply<R> build() {
            return (Reply<R>) reply;
        }

        public <R> Mono<Reply<R>> buildMono() {
            return Mono.just(build());
        }
    }
}
