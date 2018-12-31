package pub.resb.reactor.constants;

import pub.resb.reactor.models.Reply;

public enum BuiltInError {
    BUILTIN_UNKNOWN_ERROR,
    BUILTIN_UNRECOGNIZED_REPLY,
    BUILTIN_REPLY_NOT_PRESENT,
    BUILTIN_UNEXPECTED_EXCEPTION,
    BUILTIN_UNKNOWN_PROTOCOL,
    BUILTIN_SERIALIZATION_ERROR,
    BUILTIN_DESERIALIZATION_ERROR,
    ;

    public Reply.ErrorBuilder toBuilder() {
        return Reply.error(name());
    }
}
