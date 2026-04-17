package com.visualtester;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;

import java.util.*;

/** Reads the current GUI screen class name, title, and visible widget text. */
public class ScreenReader {
    public static Map<String, Object> readScreen(Minecraft client) {
        Map<String, Object> info = new LinkedHashMap<>();
        Screen screen = client.screen;

        if (screen == null) {
            info.put("screenClass", "null");
            info.put("screenSimpleName", "null");
            info.put("screenType", "IN_GAME");
            info.put("title", "");
            info.put("texts", List.of());
            return info;
        }

        info.put("screenClass", screen.getClass().getName());
        info.put("screenSimpleName", screen.getClass().getSimpleName());

        String title = "";
        try { title = screen.getTitle().getString(); } catch (Exception ignored) {}
        info.put("title", title);
        info.put("width", screen.width);
        info.put("height", screen.height);

        List<String> texts = new ArrayList<>();
        if (!title.isBlank()) texts.add(title);

        for (var child : screen.children()) {
            try {
                if (child instanceof AbstractButton btn) {
                    String msg = btn.getMessage().getString();
                    if (!msg.isBlank() && !texts.contains(msg)) texts.add(msg);
                }
            } catch (Exception ignored) {}
            try {
                if (child instanceof EditBox eb) {
                    String val = eb.getValue();
                    if (!val.isBlank() && !texts.contains(val)) texts.add(val);
                }
            } catch (Exception ignored) {}
        }

        info.put("texts", texts);
        return info;
    }
}
