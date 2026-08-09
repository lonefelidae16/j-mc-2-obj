package org.jmc.util;

public class MathHelper {
    public static int floor(double value) {
        int i = (int) value;
        return value < (double) i ? i - 1 : i;
    }
}
