package org.jmc.util;

public class MathHelper {
    public static int floor(double value) {
        int i = (int) value;
        return value < (double) i ? i - 1 : i;
    }

    public static int lerp(int from, int to, float alpha) {
        return floor(from + alpha * (to - from));
    }
}
