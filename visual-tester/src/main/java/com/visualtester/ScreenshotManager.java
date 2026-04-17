package com.visualtester;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Captures screenshots from the Minecraft render target to test-screenshots/ (relative to run dir). */
public class ScreenshotManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("ScreenshotManager");
    private static final File DIR = new File("test-screenshots");

    public static File takeAutoScreenshot() {
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        return takeNamedScreenshot("auto_" + ts);
    }

    public static File takeNamedScreenshot(String name) {
        DIR.mkdirs();
        File file = new File(DIR, name + ".png");
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getMainRenderTarget() == null) return file;
            // takeScreenshot is async - callback receives the NativeImage
            Screenshot.takeScreenshot(mc.getMainRenderTarget(), (NativeImage img) -> {
                try {
                    img.writeToFile(file.toPath());
                    LOGGER.debug("Screenshot: {}", file.getName());
                } catch (IOException e) {
                    LOGGER.error("Screenshot write failed '{}': {}", name, e.getMessage());
                } finally {
                    img.close();
                }
            });
        } catch (Exception e) {
            LOGGER.error("Screenshot error '{}': {}", name, e.getMessage());
        }
        return file;
    }
}
