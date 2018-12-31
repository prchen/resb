package pub.resb.reactor.utils;

import pub.resb.reactor.annotations.Entry;
import pub.resb.reactor.interfaces.Cell;
import pub.resb.reactor.models.Command;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;


public class ModelUtils {

    public static URI getEntry(Class<? extends Command<?>> commandType) {
        Entry entry = ReflectionUtils.findAnnotation(commandType, Entry.class);
        return entry != null ? URI.create(entry.value()) : null;
    }

    public static String getCellName(URI entry) {
        Path path = Paths.get(entry.getPath()).getName(0);
        return path.toString();
    }

    public static Class<?> getCommandType(Class<? extends Cell<?, ?>> cellType) {
        return ReflectionUtils.getTypeArgumentType(cellType, Cell.class, 0);
    }

    public static Class<?> getResultType(Class<? extends Command<?>> commandType) {
        return ReflectionUtils.getTypeArgumentType(commandType, Command.class, 0);
    }

}
