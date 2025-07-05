package com.commonutils;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

public class CommonUtils implements ClientModInitializer {
    public static final String MOD_ID = "commonutils";

	@Override
    public void onInitializeClient() {
		openKeybindScreen();
    }

	private void openKeybindScreen() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registry) -> {
		    dispatcher.register(
		        ClientCommandManager.literal("cu")
		            .executes(context -> {
		                MinecraftClient client = context.getSource().getClient();
		                client.send(() -> client.setScreen(new KeybindScreen()));
		                return 1;
		            })
		    );
		});
	}
}