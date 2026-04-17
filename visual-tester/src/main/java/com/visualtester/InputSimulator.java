package com.visualtester;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/** Simulates keyboard/mouse input via AWT Robot at OS level. */
public class InputSimulator {
    private static final Logger LOGGER = LoggerFactory.getLogger("InputSimulator");
    private static Robot robot;
    private static final Map<String, Integer> KEY_MAP = new HashMap<>();

    static {
        try { robot = new Robot(); robot.setAutoDelay(10); }
        catch (AWTException e) { LOGGER.error("AWT Robot init failed: {}", e.getMessage()); }

        for (char c = 'A'; c <= 'Z'; c++) KEY_MAP.put(String.valueOf(c), (int) c);
        KEY_MAP.put("0", KeyEvent.VK_0); KEY_MAP.put("1", KeyEvent.VK_1);
        KEY_MAP.put("2", KeyEvent.VK_2); KEY_MAP.put("3", KeyEvent.VK_3);
        KEY_MAP.put("4", KeyEvent.VK_4); KEY_MAP.put("5", KeyEvent.VK_5);
        KEY_MAP.put("6", KeyEvent.VK_6); KEY_MAP.put("7", KeyEvent.VK_7);
        KEY_MAP.put("8", KeyEvent.VK_8); KEY_MAP.put("9", KeyEvent.VK_9);
        KEY_MAP.put("SPACE",     KeyEvent.VK_SPACE);
        KEY_MAP.put("ENTER",     KeyEvent.VK_ENTER);
        KEY_MAP.put("ESCAPE",    KeyEvent.VK_ESCAPE);
        KEY_MAP.put("ESC",       KeyEvent.VK_ESCAPE);
        KEY_MAP.put("TAB",       KeyEvent.VK_TAB);
        KEY_MAP.put("SHIFT",     KeyEvent.VK_SHIFT);
        KEY_MAP.put("CTRL",      KeyEvent.VK_CONTROL);
        KEY_MAP.put("ALT",       KeyEvent.VK_ALT);
        KEY_MAP.put("UP",        KeyEvent.VK_UP);
        KEY_MAP.put("DOWN",      KeyEvent.VK_DOWN);
        KEY_MAP.put("LEFT",      KeyEvent.VK_LEFT);
        KEY_MAP.put("RIGHT",     KeyEvent.VK_RIGHT);
        KEY_MAP.put("BACKSPACE", KeyEvent.VK_BACK_SPACE);
        KEY_MAP.put("DELETE",    KeyEvent.VK_DELETE);
        KEY_MAP.put("HOME",      KeyEvent.VK_HOME);
        KEY_MAP.put("END",       KeyEvent.VK_END);
        for (int i = 1; i <= 12; i++) KEY_MAP.put("F" + i, KeyEvent.VK_F1 + (i - 1));
    }

    public static void pressKey(String keyName) {
        if (robot == null) { LOGGER.error("Robot not available"); return; }
        Integer kc = KEY_MAP.get(keyName.toUpperCase());
        if (kc == null) { LOGGER.warn("Unknown key: '{}'", keyName); return; }
        robot.keyPress(kc); robot.delay(50); robot.keyRelease(kc);
        LOGGER.debug("Pressed: {}", keyName);
    }

    public static void mouseClick(int windowX, int windowY) {
        if (robot == null) { LOGGER.error("Robot not available"); return; }
        try {
            long wh = Minecraft.getInstance().getWindow().handle();
            int[] wx = new int[1], wy = new int[1];
            GLFW.glfwGetWindowPos(wh, wx, wy);
            robot.mouseMove(wx[0] + windowX, wy[0] + windowY);
            robot.delay(50);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            LOGGER.debug("Click window({},{}) screen({},{})", windowX, windowY, wx[0]+windowX, wy[0]+windowY);
        } catch (Exception e) { LOGGER.error("Click failed: {}", e.getMessage()); }
    }

    public static void typeText(String text) {
        if (robot == null) { LOGGER.error("Robot not available"); return; }
        for (char c : text.toCharArray()) {
            if (c == ' ') {
                robot.keyPress(KeyEvent.VK_SPACE); robot.delay(20); robot.keyRelease(KeyEvent.VK_SPACE);
            } else {
                Integer kc = KEY_MAP.get(String.valueOf(c).toUpperCase());
                if (kc == null) continue;
                boolean shift = Character.isUpperCase(c);
                if (shift) robot.keyPress(KeyEvent.VK_SHIFT);
                robot.keyPress(kc); robot.delay(20); robot.keyRelease(kc);
                if (shift) robot.keyRelease(KeyEvent.VK_SHIFT);
            }
            robot.delay(30);
        }
        LOGGER.debug("Typed: {}", text);
    }
}
