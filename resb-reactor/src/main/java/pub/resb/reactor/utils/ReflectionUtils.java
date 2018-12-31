package pub.resb.reactor.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.stream.Stream;

public class ReflectionUtils {

    public static <A extends Annotation> A findAnnotation(Class<?> type, Class<A> annotationType) {
        if (type == null) {
            return null;
        }
        A meta = type.getAnnotation(annotationType);
        if (meta != null) {
            return meta;
        }
        return findAnnotation(type.getSuperclass(), annotationType);
    }

    public static Class<?> getTypeArgumentType(Class<?> type, Class<?> parameterizedType, int typeArgumentIndex) {
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
