package archives.tater.offhandhotbar;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeScreenHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import static net.minecraft.util.Util.createTranslationKey;

public class OffhandHotbar implements ModInitializer, ClientModInitializer {
	public static final String MOD_ID = "offhandhotbar";

	public static int selectedOffhandSlot = 0;
	private static int lastOffhandSlot = selectedOffhandSlot;
	public static boolean swapped = false;
	public static boolean focusSwapped = false;

	public static final int OFFHAND_SWAP_ID = 40;
	public static final int SLOTS_OFFSET = 18;

	public static final int HOTBAR_WIDTH = 91;
	public static final int HOTBAR_GAP = 4;
	public static final int HOTBAR_X_OFFSET = HOTBAR_WIDTH + HOTBAR_GAP / 2;
	public static final int HOTBAR_HEIGHT = 22;
	public static final int HOTBAR_Y_OFFSET = -(HOTBAR_HEIGHT + HOTBAR_GAP);

	private static final int[] forwardSlotOrder = {0, 9, 18, 0};
	private static final int[] backwardSlotOrder = {18, 9, 0, 18};

	public static final String KEY_CATEGORY = createTranslationKey("category", Identifier.of(MOD_ID, "offhandhotbar"));

	public static final KeyBinding CONTROL_OPPOSITE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			createTranslationKey("key", Identifier.of(MOD_ID, "control_opposite")),
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_LEFT_ALT,
			KEY_CATEGORY
	));

	public static final KeyBinding SCROLL_INVENTORY_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			createTranslationKey("key", Identifier.of(MOD_ID, "scroll_inventory")),
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			KEY_CATEGORY
	));

	public static int getOffhandHotbarSlot(int selectedSlot) {
		return PlayerScreenHandler.INVENTORY_START + SLOTS_OFFSET + selectedSlot;
	}

	public static int getOffhandHotbarScreenHandlerSlot(int selectedSlot, MinecraftClient client) {
		var player = client.player;
		if (player == null) return -1;
		var playerSlot = getOffhandHotbarSlot(selectedSlot);
		var currentScreenHandler = player.currentScreenHandler;
		if (currentScreenHandler == null || currentScreenHandler instanceof PlayerScreenHandler || currentScreenHandler instanceof CreativeScreenHandler)
			return playerSlot;
		return currentScreenHandler.getSlotIndex(player.getInventory(), playerSlot).orElse(-1);
	}

	public static void offhandCycle(MinecraftClient client, int slot1, int slot2) {
		if (slot1 == -1 || slot2 == -1) return;
		var interactionManager = client.interactionManager;
		if (interactionManager == null) return;
		var player = client.player;
		if (player == null) return;
		offhandCycle(interactionManager, player, slot1, slot2);
	}

	public static void offhandCycle(ClientPlayerInteractionManager interactionManager, ClientPlayerEntity player, int slot1, int slot2) {
		interactionManager.clickSlot(player.playerScreenHandler.syncId, slot1, OFFHAND_SWAP_ID, SlotActionType.SWAP, player);
		interactionManager.clickSlot(player.playerScreenHandler.syncId, slot2, OFFHAND_SWAP_ID, SlotActionType.SWAP, player);
	}

	public static void swapOffhand(MinecraftClient client) {
		swapOffhand(client, getOffhandHotbarScreenHandlerSlot(selectedOffhandSlot, client));
	}

	public static void swapOffhand(MinecraftClient client, int slot) {
		if (slot == -1) return;
		var interactionManager = client.interactionManager;
		if (interactionManager == null) return;
		var player = client.player;
		if (player == null) return;
		swapOffhand(interactionManager, player, slot);
	}

	public static void swapOffhand(ClientPlayerInteractionManager interactionManager, ClientPlayerEntity player, int slot) {
		interactionManager.clickSlot(player.currentScreenHandler.syncId, slot, OFFHAND_SWAP_ID, SlotActionType.SWAP, player);
	}

	public static void updateOffhandSlots(MinecraftClient client) {
        if (selectedOffhandSlot == lastOffhandSlot) return;
		if (client.player == null) return;
		
		lastOffhandSlot = selectedOffhandSlot;
		
		if (!swapped) return; // Not in swapped state, nothing to swap

		var focusSwap = focusSwapped;
		if (focusSwap) updateFocusSwap(client, false);

        // Swap the offhand with the newly selected inventory slot
        swapOffhand(client, getOffhandHotbarScreenHandlerSlot(selectedOffhandSlot, client));

		if (focusSwap) updateFocusSwap(client, true);
    }

	public static void scrollInventory(MinecraftClient client, boolean forward) {
		if (client.player == null) return;
		var interactionManager = client.interactionManager;
		if (interactionManager == null) return;
		var syncId = client.player.playerScreenHandler.syncId;

		var focusSwap = focusSwapped;
		if (focusSwap) updateFocusSwap(client, false);

		swapOffhand(client);

		for (var i = 0; i < 9; i++) {
			for (var j : forward ? forwardSlotOrder : backwardSlotOrder)
				interactionManager.clickSlot(syncId, PlayerScreenHandler.INVENTORY_START + i + j, 0, SlotActionType.PICKUP, client.player);
		}

		swapOffhand(client);

		if (focusSwap) updateFocusSwap(client, true);
	}

	public static void updateFocusSwap(MinecraftClient client, boolean swap) {
        if (swap == focusSwapped) return;
        focusSwapped = !focusSwapped;

        var player = client.player;
        if (player == null) return;
        client.interactionManager.clickSlot(player.playerScreenHandler.syncId, PlayerScreenHandler.HOTBAR_START + player.getInventory().selectedSlot, OFFHAND_SWAP_ID, SlotActionType.SWAP, player);
    }

	/**
	 * Checks if the CONTROL_OPPOSITE_KEY is being used to control the offhand hotbar.
	 * When true, focusSwap should NOT be activated since the key is being used for offhand control.
	 */
	public static boolean isControlKeyForOffhandHotbar() {
		// If scroll controls main hand by default, then CONTROL_OPPOSITE_KEY + scroll = offhand hotbar
		// If keyboard controls main hand by default, then CONTROL_OPPOSITE_KEY + keyboard = offhand hotbar
		return (OffhandHotbarConfig.scrollControls == Hand.MAIN_HAND) ||
			   (OffhandHotbarConfig.keyboardControls == Hand.MAIN_HAND);
	}

	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			updateOffhandSlots(client);

            if (client.player == null) return;

            if (!(client.currentScreen instanceof HandledScreen<?>) || client.player.currentScreenHandler == null) {
                if (!swapped) {
                    swapOffhand(client);
                    swapped = !swapped;
                }

				// Only activate focusSwap when CONTROL_OPPOSITE_KEY is pressed AND it's not being
				// used to control the offhand hotbar (i.e., neither scroll nor keyboard uses the key for offhand)
				boolean shouldFocusSwap = CONTROL_OPPOSITE_KEY.isPressed() && !isControlKeyForOffhandHotbar();
				updateFocusSwap(client, shouldFocusSwap);
            } else {
                if (swapped) {
                    swapOffhand(client);
                    swapped = !swapped;
                }
            }
        });
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			swapped = false;
			focusSwapped = false;
			selectedOffhandSlot = 0;
			lastOffhandSlot = 0;
		});
	}

	@Override
	public void onInitialize() {
		MidnightConfig.init(MOD_ID, OffhandHotbarConfig.class);
	}
}