package pub.resb.core.utils;

import pub.resb.api.annotations.Entry;
import pub.resb.api.interfaces.Cell;
import pub.resb.api.models.Command;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;


public class ResbApiUtils {

    public static URI getEntry(Class<? extends Command<?>> commandType) {
        Entry entry = findAnnotation(commandType, Entry.class);
        return entry != null ? URI.create(entry.value()) : null;
    }

    public static String getCellName(URI entry) {
        Path path = Paths.get(entry.getPath()).getName(0);
        return path.toString();
    }

    public static Class<?> getCommandType(Class<? extends Cell<?, ?>> cellType) {
        return getTypeArgumentType(cellType, Cell.class, 0);
    }

    public static Class<?> getResultType(Class<? extends Command<?>> commandType) {
        return getTypeArgumentType(commandType, Command.class, 0);
    }

    private static <A extends Annotation> A findAnnotation(Class<?> type, Class<A> annotationType) {
        if (type == null) {
            return null;
        }
        A meta = type.getAnnotation(annotationType);
        if (meta != null) {
            return meta;
        }
        return findAnnotation(type.getSuperclass(), annotationType);
    }

    private static Class<?> getTypeArgumentType(Class<?> type, Class<?> parameterizedType, int typeArgumentIndex) {
        return getAllGenericInterfaces(type)
                .filter(x -> x instanceof ParameterizedType)
                .map(x -> (ParameterizedType) x)
                .filter(x -> x.getRawType() == parameterizedType)
                .filter(x -> x.getActualTypeArguments().length > typeArgumentIndex)
                .map(x -> x.getActualTypeArguments()[typeArgumentIndex])
                .filter(x -> x instanceof Class)
                .map(x -> (Class<?>) x)
                .findFirst()
                .orElse(null);
    }

    private static Stream<Type> getAllGenericInterfaces(Class<?> clazz) {
        if (clazz == null) {
            return Stream.empty();
        }
        return Stream.concat(
                Stream.of(clazz.getGenericInterfaces()),
                getAllGenericInterfaces(clazz.getSuperclass()));
    }
}
