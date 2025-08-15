package com.shadow.keytotem.client;

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
        // Silently fails if no totem is found
    }

    private int findTotemSlot(PlayerInventory inventory) {
        // Search hotbar first (slots 0-8)
        for (int i = 0; i < 9; i++) {
            if (inventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }

        // Then search main inventory (slots 9-35)
        for (int i = 9; i < 36; i++) {
            if (inventory.getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Sends a ClickSlotC2SPacket to the server to swap an inventory item with the offhand.
     * This method is updated for Minecraft 1.21.5 compatibility.
     */
    private void moveTotem(MinecraftClient client, int totemInventorySlot) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.getNetworkHandler() == null) return;

        PlayerScreenHandler screenHandler = player.playerScreenHandler;

        // Convert inventory slot to the correct packet slot index.
        // Hotbar (0-8) -> Packet (36-44)
        // Main Inv (9-35) -> Packet (9-35)
        int packetSlot = totemInventorySlot < 9 ? totemInventorySlot + 36 : totemInventorySlot;

        // For SlotActionType.SWAP, the 'button' argument is the target slot.
        // Hotbar buttons are 0-8, and the offhand button is 40.
        final int OFFHAND_BUTTON = 40;

        // The ClickSlotC2SPacket constructor for MC 1.21+ requires an ItemStackHash.
        ItemStackHash carriedStackHash = ItemStackHash.fromItemStack(
                screenHandler.getCursorStack(), // The item on the cursor (should be empty)
                client.getNetworkHandler().getComponentHasher()
        );

        ClickSlotC2SPacket packet = new ClickSlotC2SPacket(
                screenHandler.syncId,
                screenHandler.getRevision(),
                packetSlot,
                OFFHAND_BUTTON,
                SlotActionType.SWAP,
                screenHandler.getTrackedSlots(), // A map of slots that have changed
                carriedStackHash
        );

        client.getNetworkHandler().sendPacket(packet);
    }
}