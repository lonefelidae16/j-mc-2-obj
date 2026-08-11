package org.jmc.util;

import java.util.function.Consumer;

public class StaticInit {
    public static <T> T make(T object, Consumer<? super T> initializer) {
        initializer.accept(object);
        return object;
    }
}
