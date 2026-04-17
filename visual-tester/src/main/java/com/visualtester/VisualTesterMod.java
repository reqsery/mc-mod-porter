package com.visualtester;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class VisualTesterMod implements ClientModInitializer {
    public static final String MOD_ID = "visualtester";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private CommandProcessor commandProcessor;
    private int screenshotTimer = 0;
    private static final int SCREENSHOT_INTERVAL = 40; // 2 seconds @ 20 TPS

    @Override
    public void onInitializeClient() {
        new File("run/test-screenshots").mkdirs();
        new File("run/test-output").mkdirs();

        commandProcessor = new CommandProcessor();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            screenshotTimer++;
            if (screenshotTimer >= SCREENSHOT_INTERVAL) {
                screenshotTimer = 0;
                ScreenshotManager.takeAutoScreenshot();
            }
            commandProcessor.tick(client);
        });

        // Capture player chat (cancellable)
        ClientReceiveMessageEvents.ALLOW_CHAT.register(
            (message, signedMessage, sender, params, receptionTimestamp) -> {
                ChatCapture.addMessage(message.getString());
                return true;
            });

        // Capture system/game messages (GAME = the non-cancel version)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) ChatCapture.addMessage("[SYSTEM] " + message.getString());
        });

        LOGGER.info("Visual Tester initialized. Watching: run/test-commands.json");
    }
}
