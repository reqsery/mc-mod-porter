package com.autoporter;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.*;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class GuiReleaseTest {
    @TempDir Path temp;

    @Test @EnabledOnOs(OS.WINDOWS)
    void guiPortBuildAndRunClientActionsStreamLogsFromPathsWithSpaces() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        Path source = Files.createDirectories(temp.resolve("GUI project with spaces"));
        Files.writeString(source.resolve("gradle.properties"), "minecraft_version=1.21.10\n");
        Files.writeString(source.resolve("build.gradle"), "plugins { id 'java' }\n");
        Path resources = Files.createDirectories(source.resolve("src/main/resources"));
        Files.writeString(resources.resolve("fabric.mod.json"), "{\"schemaVersion\":1,\"id\":\"example\",\"version\":\"1\",\"depends\":{\"minecraft\":\"1.21.10\"}}");
        Files.writeString(source.resolve("gradlew.bat"), "@echo off\r\necho WRAPPER_TASK=%1\r\nexit /b 0\r\n");
        MainWindow[] holder = new MainWindow[1];
        SwingUtilities.invokeAndWait(() -> {
            holder[0] = new MainWindow();
            holder[0].setVisible(true);
            ((JTextField) value(holder[0], "projectPathField")).setText(source.toString());
            ((JComboBox<?>) value(holder[0], "targetVersionCombo")).setSelectedItem("1.21.11");
            ((JButton) value(holder[0], "portButton")).doClick();
        });
        try {
            waitForIdle(holder[0]);
            assertTrue(logs(holder[0]).contains("WRAPPER_TASK=compileJava"), logs(holder[0]));
            assertTrue(logs(holder[0]).contains("Port complete. Found an issue? Use Report Bug below, or join Discord for help."));
            assertTrue(Files.exists(AutoPorterMain.outputDirectoryFor(source, "1.21.11").resolve("build/port-report.json")));
            for (String[] action : List.of(new String[]{"buildButton", "build"}, new String[]{"runClientButton", "runClient"})) {
                SwingUtilities.invokeAndWait(() -> {
                    JButton button = (JButton) value(holder[0], action[0]);
                    assertTrue(button.isEnabled());
                    button.doClick();
                });
                waitForIdle(holder[0]);
                assertTrue(logs(holder[0]).contains("WRAPPER_TASK=" + action[1]), logs(holder[0]));
            }
            Files.writeString(source.resolve("gradlew.bat"), "@echo off\r\necho BUILD FAILED\r\nexit /b 1\r\n");
            SwingUtilities.invokeAndWait(() -> ((JButton) value(holder[0], "portButton")).doClick());
            waitForIdle(holder[0]);
            assertTrue(logs(holder[0]).contains("Port completed with issues. Review the logs, then use Report Bug below if needed."));
            assertFalse(logs(holder[0]).contains("Port complete. Found an issue?"));
        } finally { SwingUtilities.invokeAndWait(holder[0]::dispose); }
    }

    private void waitForIdle(MainWindow window) throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(30);
        while (System.nanoTime() < deadline) {
            boolean[] idle = new boolean[1];
            SwingUtilities.invokeAndWait(() -> idle[0] = value(window, "activeWorker") == null);
            if (idle[0]) return;
            Thread.sleep(50);
        }
        fail("GUI worker did not complete within 30 seconds");
    }

    private String logs(MainWindow window) throws Exception {
        String[] text = new String[1];
        SwingUtilities.invokeAndWait(() -> text[0] = ((JTextArea) value(window, "logArea")).getText());
        return text[0];
    }

    private Object value(MainWindow window, String name) {
        try {
            Field field = MainWindow.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(window);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }

    @Test void targetVersionsAndExistingQuickLinksFitTheWindow() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(() -> {
            com.formdev.flatlaf.FlatLightLaf.setup();
            for (String key : List.of("Button.arc", "Component.arc", "TextComponent.arc", "ProgressBar.arc"))
                UIManager.put(key, 0);
            MainWindow window = new MainWindow();
            try {
                window.setVisible(true);
                window.validate();
                List<JButton> buttons = new ArrayList<>();
                collectButtons(window.getContentPane(), buttons);
                int previousX = -1;
                for (String name : List.of("Discord", "GitHub", "Contribute", "Report Bug", "Suggest Feature")) {
                    JButton button = buttons.stream().filter(b -> name.equals(b.getText())).findFirst().orElseThrow();
                    assertEquals(1, button.getActionListeners().length);
                    assertEquals(12, button.getFont().getSize());
                    assertNotNull(button.getIcon(), name + " icon is packaged");
                    Rectangle bounds = SwingUtilities.convertRectangle(button.getParent(), button.getBounds(), window.getContentPane());
                    assertTrue(bounds.x > previousX, name + " follows the requested left-to-right order");
                    previousX = bounds.x;
                    assertTrue(window.getContentPane().getBounds().contains(bounds), name + " fits in the window");
                    assertTrue(bounds.y >= 600, name + " stays below the log panel");
                }
                try {
                    Field field = MainWindow.class.getDeclaredField("targetVersionCombo");
                    field.setAccessible(true);
                    JComboBox<?> versions = (JComboBox<?>) field.get(window);
                    assertEquals(VersionDatabase.allVersions().size(), versions.getItemCount());
                    versions.setSelectedItem("26.3");
                    assertEquals("26.3", versions.getSelectedItem());
                    versions.setSelectedItem("1.21.11");
                    assertEquals("1.21.11", versions.getSelectedItem());
                } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
                try {
                    java.awt.image.BufferedImage preview = new java.awt.image.BufferedImage(
                        window.getContentPane().getWidth(), window.getContentPane().getHeight(),
                        java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = preview.createGraphics();
                    try { window.getContentPane().printAll(graphics); }
                    finally { graphics.dispose(); }
                    Path output = Path.of("build/reports/gui-quick-links.png");
                    Files.createDirectories(output.getParent());
                    javax.imageio.ImageIO.write(preview, "png", output.toFile());
                } catch (java.io.IOException exception) { throw new AssertionError(exception); }
            } finally { window.dispose(); }
        });
    }

    private void collectButtons(Container parent, List<JButton> buttons) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton button) buttons.add(button);
            if (component instanceof Container child) collectButtons(child, buttons);
        }
    }
}
