package com.shadow.keytotem.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class KeyTotemClient implements ClientModInitializer {

    private static KeyBinding equipTotemKeybind;

    @Override
    public void onInitializeClient() {
        equipTotemKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.keytotem.equip",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN, // Default: Unassigned
                "key.categories.keytotem"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (equipTotemKeybind.wasPressed()) {
                equipTotem(client);
            }
        });
    }

    private void equipTotem(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) {
            return;
        }

        int totemSlot = findTotemSlot(client.player.getInventory());

        if (totemSlot != -1) {
            moveTotem(client, totemSlot);
        }
    }

    private int findTotemSlot(PlayerInventory inventory) {
        for (int i = 0; i < 9; i++) {
            if (inventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }
        for (int i = 9; i < 36; i++) {
            if (inventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }
        return -1;
    }

    private void moveTotem(MinecraftClient client, int totemInventorySlot) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.getNetworkHandler() == null) return;

        PlayerScreenHandler screenHandler = player.playerScreenHandler;
        int packetSlot = totemInventorySlot < 9 ? totemInventorySlot + 36 : totemInventorySlot;
        final int OFFHAND_BUTTON = 40;

        // CORRECTED: The method is getComponentChangesHash()
        ItemStackHash carriedStackHash = ItemStackHash.fromItemStack(
                screenHandler.getCursorStack(),
                screenHandler.getComponentChangesHash()
        );

        ClickSlotC2SPacket packet = new ClickSlotC2SPacket(
                screenHandler.syncId,
                screenHandler.getRevision(),
                (short) packetSlot, // CORRECTED: Explicitly cast the int to a short
                OFFHAND_BUTTON,
                SlotActionType.SWAP,
                new Int2ObjectOpenHashMap<>(),
                carriedStackHash
        );

        client.getNetworkHandler().sendPacket(packet);
    }
}
