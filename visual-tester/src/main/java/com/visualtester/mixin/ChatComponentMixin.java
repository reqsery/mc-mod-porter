package com.visualtester.mixin;

import com.visualtester.ChatCapture;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks ChatComponent.addMessage() to forward all chat messages
 * (player and system) into ChatCapture for test assertions.
 */
@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @Inject(
        method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
        at = @At("HEAD")
    )
    private void onAddMessage(Component message, MessageSignature sig, GuiMessageTag tag, CallbackInfo ci) {
        String text = message.getString();
        if (text != null && !text.isBlank()) ChatCapture.addMessage(text);
    }
}
