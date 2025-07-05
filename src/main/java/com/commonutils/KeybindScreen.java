package com.commonutils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeybindScreen extends Screen {
    private static final int ELEMENT_HEIGHT = 20;
    private static final int ELEMENT_SPACING = 10;

    private TextFieldWidget keyField;
    private TextFieldWidget messageField;
    private final List<KeybindEntry> keybinds = new ArrayList<>();
    private final Set<String> keysPressed = new HashSet<>();
    private boolean registeredKeyCallback = false;
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("commonutils.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public KeybindScreen() {
        super(Text.of(CommonUtils.MOD_ID + " Keybind Config"));
        loadKeybinds();
    }

    @Override
    public void init() {
        this.clearChildren();
        int yOffset = ELEMENT_SPACING;

        for (KeybindEntry entry : keybinds) {
            addKeybindRow(entry, yOffset);
            yOffset += ELEMENT_HEIGHT + ELEMENT_SPACING;
        }

        keyField = new TextFieldWidget(textRenderer, ELEMENT_SPACING, yOffset, 80, ELEMENT_HEIGHT, Text.of("Key"));
        keyField.setPlaceholder(Text.of("e.g. R"));
        addDrawableChild(keyField);

        messageField = new TextFieldWidget(textRenderer, ELEMENT_SPACING + 90, yOffset, width - 300, ELEMENT_HEIGHT, Text.of("Message"));
        messageField.setPlaceholder(Text.of("/say Hello!"));
        addDrawableChild(messageField);

        ButtonWidget saveButton = ButtonWidget.builder(Text.of("Bind Key"), btn -> {
            String key = keyField.getText().toUpperCase();
            String msg = messageField.getText();
            if (key.length() == 1 && !msg.isEmpty()) {
                keybinds.add(new KeybindEntry(key, msg));
                keyField.setText("");
                messageField.setText("");
                client.inGameHud.getChatHud().addMessage(Text.literal("Bound key: " + key + " → " + msg));
                saveKeybinds();
                this.init();
            }
        }).dimensions(width - 100 - ELEMENT_SPACING, yOffset, 100, ELEMENT_HEIGHT)
                .tooltip(Tooltip.of(Text.of("Bind the key to the message"))).build();

        addDrawableChild(saveButton);
        initCloseButton();

        if (!registeredKeyCallback) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (client.currentScreen == null) {
                    long handle = client.getWindow().getHandle();
                    Set<String> currentlyPressed = new HashSet<>();

                    for (KeybindEntry entry : keybinds) {
                        int code = InputUtil.fromTranslationKey("key.keyboard." + entry.key.toLowerCase()).getCode();
                        boolean keyPressed = InputUtil.isKeyPressed(handle, code);
                        boolean shiftDown = InputUtil.isKeyPressed(handle, 340) || InputUtil.isKeyPressed(handle, 344);
                        boolean ctrlDown = InputUtil.isKeyPressed(handle, 341) || InputUtil.isKeyPressed(handle, 345);

                        boolean requiredShift = entry.shift;
                        boolean requiredCtrl = entry.ctrl;

                        boolean shiftMatch = !requiredShift || shiftDown;
                        boolean ctrlMatch = !requiredCtrl || ctrlDown;

                        String keyIdentifier = entry.key + ":" + entry.ctrl + ":" + entry.shift;

                        if (keyPressed && shiftMatch && ctrlMatch) {
                            currentlyPressed.add(keyIdentifier);
                            if (!keysPressed.contains(keyIdentifier)) {
                                client.getNetworkHandler().sendChatMessage(entry.message);
                            }
                        }
                    }
                    keysPressed.clear();
                    keysPressed.addAll(currentlyPressed);
                } else {
                    keysPressed.clear();
                }
            });
            registeredKeyCallback = true;
        }
    }

    private void addKeybindRow(KeybindEntry entry, int y) {
        ButtonWidget keyButton = ButtonWidget.builder(
                Text.of(entry.key + " → " + entry.message), btn -> {}
        ).dimensions(ELEMENT_SPACING, y, width - 250, ELEMENT_HEIGHT).build();

        ButtonWidget ctrlToggle = ButtonWidget.builder(
                Text.of("Ctrl: " + (entry.ctrl ? "ON" : "OFF")), btn -> {
                    entry.ctrl = !entry.ctrl;
                    saveKeybinds();
                    this.init();
                }
        ).dimensions(width - 240, y, 60, ELEMENT_HEIGHT).build();

        ButtonWidget shiftToggle = ButtonWidget.builder(
                Text.of("Shift: " + (entry.shift ? "ON" : "OFF")), btn -> {
                    entry.shift = !entry.shift;
                    saveKeybinds();
                    this.init();
                }
        ).dimensions(width - 175, y, 60, ELEMENT_HEIGHT).build();

        ButtonWidget deleteButton = ButtonWidget.builder(Text.of("X"), btn -> {
            keybinds.remove(entry);
            saveKeybinds();
            this.init();
        }).dimensions(width - 110, y, 30, ELEMENT_HEIGHT).tooltip(Tooltip.of(Text.of("Remove this keybind"))).build();

        addDrawableChild(keyButton);
        addDrawableChild(ctrlToggle);
        addDrawableChild(shiftToggle);
        addDrawableChild(deleteButton);
    }

    private void initCloseButton() {
        int closeButtonWidth = 100;
        int closeButtonX = width - closeButtonWidth - ELEMENT_SPACING;
        int closeButtonY = height - ELEMENT_HEIGHT - ELEMENT_SPACING;
        ButtonWidget closeButton = ButtonWidget.builder(Text.of("Close"), button -> client.setScreen(null))
                .dimensions(closeButtonX, closeButtonY, closeButtonWidth, ELEMENT_HEIGHT).build();
        addDrawableChild(closeButton);
    }

    private void saveKeybinds() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(keybinds, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadKeybinds() {
        if (!Files.exists(CONFIG_PATH)) return;
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            Type listType = new TypeToken<ArrayList<KeybindEntry>>() {}.getType();
            List<KeybindEntry> loaded = GSON.fromJson(reader, listType);
            if (loaded != null) keybinds.addAll(loaded);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class KeybindEntry {
        String key;
        String message;
        boolean ctrl = false;
        boolean shift = false;

        KeybindEntry(String key, String message) {
            this.key = key;
            this.message = message;
        }
    }
}
