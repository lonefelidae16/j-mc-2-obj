package org.jmc.util;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class Colors {
    public static final Color WHITE = new Color(0xE9ECEC);
    public static final Color ORANGE = new Color(0xF07613);
    public static final Color MAGENTA = new Color(0xBD44B3);
    public static final Color LIGHT_BLUE = new Color(0x3AAFD9);
    public static final Color YELLOW = new Color(0xF8C627);
    public static final Color LIME = new Color(0x70B919);
    public static final Color PINK = new Color(0xED8DAC);
    public static final Color GRAY = new Color(0x3E4447);
    public static final Color LIGHT_GRAY = new Color(0x8E8E86);
    public static final Color CYAN = new Color(0x158991);
    public static final Color PURPLE = new Color(0x792AAC);
    public static final Color BLUE = new Color(0x35399D);
    public static final Color BROWN = new Color(0x724728);
    public static final Color GREEN = new Color(0x546D1B);
    public static final Color RED = new Color(0xA12722);
    public static final Color BLACK = new Color(0x141519);

    private static final Map<String, Color> COLOR_MAP = new HashMap<>();

    public static Optional<Color> fromString(String str) {
        if (str == null) {
            return Optional.empty();
        }

        final String target = str.toLowerCase(Locale.ROOT);

        Color result = null;

        synchronized (COLOR_MAP) {
            if (!COLOR_MAP.isEmpty()) {
                return Optional.ofNullable(COLOR_MAP.get(target));
            }

            for (Field field : Colors.class.getFields()) {
                final String fieldName = field.getName().toLowerCase(Locale.ROOT);
                try {
                    Object instance = field.get(Colors.class);
                    if (instance instanceof Color) {
                        COLOR_MAP.put(fieldName, (Color) instance);
                        if (fieldName.equals(target)) {
                            result = (Color) instance;
                        }
                    }
                } catch (Exception ignore) {
                }
            }
        }

        return Optional.ofNullable(result);
    }

    public static Color lerp(Color from, Color to, float alpha) {
        final int red = MathHelper.lerp(from.getRed(), to.getRed(), alpha);
        final int green = MathHelper.lerp(from.getGreen(), to.getGreen(), alpha);
        final int blue = MathHelper.lerp(from.getBlue(), to.getBlue(), alpha);
        final int a = MathHelper.lerp(from.getAlpha(), to.getAlpha(), alpha);
        return new Color(red, green, blue, a);
    }
}
