package test.classes;

import pub.resb.api.interfaces.Ordered;

public class OrderedObject implements Ordered {
    public static final Integer[] PRIORITIES = new Integer[]{
            Ordered.HIGHEST_PRIORITY,
            Ordered.HIGHEST_PRIORITY + 1,
            Ordered.DEFAULT_PRIORITY - 1,
            Ordered.DEFAULT_PRIORITY,
            Ordered.DEFAULT_PRIORITY,
            Ordered.DEFAULT_PRIORITY + 1,
            Ordered.LOWEST_PRIORITY - 1,
            Ordered.LOWEST_PRIORITY,
    };

    private int value;

    public OrderedObject(int value) {
        this.value = value;
    }

    @Override
    public int getPriority() {
        return value;
    }
}

