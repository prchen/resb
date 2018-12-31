package pub.resb.reactor.interfaces;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public interface Ordered {
    int HIGHEST_PRIORITY = Integer.MIN_VALUE;
    int LOWEST_PRIORITY = Integer.MAX_VALUE;
    int DEFAULT_PRIORITY = 0;

    default int getPriority() {
        return DEFAULT_PRIORITY;
    }

    static int compare(Ordered x, Ordered y) {
        if (x.getPriority() < y.getPriority()) {
            return -1;
        } else if (x.getPriority() > y.getPriority()) {
            return 1;
        } else {
            return x.getClass().getName().compareTo(y.getClass().getName());
        }
    }

    static <T extends Ordered> List<T> adjust(List<T> list, boolean asc) {
        List<T> result = list.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted(asc ? Ordered::compare : (a, b) -> compare(b, a))
                .collect(Collectors.toList());
        return result;
    }
}
