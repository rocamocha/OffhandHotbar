package archives.tater.offhandhotbar;

import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import static net.minecraft.util.Util.createTranslationKey;

/**
 * Offhand Hotbar Mod - Adds a second hotbar that controls the player's offhand slot.
 * 
 * <h2>Core Concept:</h2>
 * The mod displays inventory slots 27-35 (the bottom row of the main inventory, above the hotbar)
 * as a "second hotbar". One of these 9 slots is selected at any time. The selected slot's item
 * is what appears in the player's offhand.
 * 
 * <h2>How It Works:</h2>
 * <ul>
 *   <li>The display shows the 9 inventory slots (27-35) as the offhand hotbar</li>
 *   <li>The selected slot is highlighted, and its item is shown as the offhand item</li>
 *   <li>When the user changes selection, we swap the offhand with the newly selected slot</li>
 *   <li>This means the offhand always contains the "selected" item from the offhand hotbar</li>
 * </ul>
 * 
 * <h2>Simplified Architecture:</h2>
 * Unlike the original approach that constantly swapped items back and forth every tick,
 * this version only performs swaps when:
 * <ol>
 *   <li>The user changes their offhand hotbar selection</li>
 *   <li>The user uses the scroll inventory feature</li>
 * </ol>
 */
public class OffhandHotbar implements ModInitializer, ClientModInitializer {
	public static final String MOD_ID = "offhandhotbar";

	// ==================== State Variables ====================
	
	/** 
	 * Currently selected slot in the offhand hotbar (0-8).
	 * This corresponds to inventory slots 27-35 (INVENTORY_START + SLOTS_OFFSET + selectedOffhandSlot).
	 */
	public static int selectedOffhandSlot = 0;
	
	/** 
	 * Tracks the previous selection to detect changes.
	 * When selectedOffhandSlot != lastOffhandSlot, we need to perform a swap.
	 */
	private static int lastOffhandSlot = selectedOffhandSlot;

	// ==================== Constants ====================
	
	/** 
	 * The button ID for the offhand slot in Minecraft's slot click system.
	 * Used with SlotActionType.SWAP to swap items with the offhand.
	 */
	public static final int OFFHAND_SWAP_BUTTON = 40;
	
	/** 
	 * Offset from INVENTORY_START to reach the bottom inventory row.
	 * Inventory layout: slots 9-17 (top row), 18-26 (middle), 27-35 (bottom/offhand hotbar).
	 * So offset = 18 means we start at slot 27 (9 + 18 = 27).
	 */
	public static final int SLOTS_OFFSET = 18;

	// ==================== Display Constants ====================
	
	/** Width of a hotbar in pixels */
	public static final int HOTBAR_WIDTH = 91;
	/** Gap between hotbars */
	public static final int HOTBAR_GAP = 4;
	/** Horizontal offset for side-by-side display mode */
	public static final int HOTBAR_X_OFFSET = HOTBAR_WIDTH + HOTBAR_GAP / 2;
	/** Height of a hotbar in pixels */
	public static final int HOTBAR_HEIGHT = 22;
	/** Vertical offset for stacked display mode */
	public static final int HOTBAR_Y_OFFSET = -(HOTBAR_HEIGHT + HOTBAR_GAP);

	// ==================== Scroll Inventory Feature ====================
	
	/** 
	 * Slot order for scrolling inventory rows forward.
	 * This rotates items through the three inventory rows.
	 */
	private static final int[] forwardSlotOrder = {0, 9, 18, 0};
	
	/** Slot order for scrolling inventory rows backward */
	private static final int[] backwardSlotOrder = {18, 9, 0, 18};

	// ==================== Key Bindings ====================
	
	public static final String KEY_CATEGORY = createTranslationKey("category", Identifier.of(MOD_ID, "offhandhotbar"));

	/** 
	 * When held, this key makes scroll/keyboard control the opposite hotbar.
	 * Default: Left Alt
	 */
	public static final KeyBinding CONTROL_OPPOSITE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			createTranslationKey("key", Identifier.of(MOD_ID, "control_opposite")),
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_LEFT_ALT,
			KEY_CATEGORY
	));

	/** 
	 * When held while scrolling, rotates items through all three inventory rows.
	 * Default: R
	 */
	public static final KeyBinding SCROLL_INVENTORY_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			createTranslationKey("key", Identifier.of(MOD_ID, "scroll_inventory")),
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_R,
			KEY_CATEGORY
	));

	// ==================== Slot Calculation Methods ====================

	/**
	 * Converts an offhand hotbar slot index (0-8) to the corresponding inventory slot number.
	 * 
	 * @param slotIndex The offhand hotbar slot (0-8)
	 * @return The inventory slot number (27-35)
	 */
	public static int getOffhandHotbarSlot(int slotIndex) {
		// PlayerScreenHandler.INVENTORY_START = 9 (first slot after crafting/armor)
		// SLOTS_OFFSET = 18 (skip top two inventory rows)
		// Result: 9 + 18 + slotIndex = 27 + slotIndex
		return PlayerScreenHandler.INVENTORY_START + SLOTS_OFFSET + slotIndex;
	}

	// ==================== Inventory Swap Methods ====================

	/**
	 * Swaps the offhand slot with a specific inventory slot.
	 * This is the core operation that changes what item is in the player's offhand.
	 * 
	 * @param client The Minecraft client instance
	 * @param inventorySlot The inventory slot to swap with offhand
	 */
	public static void swapWithOffhand(MinecraftClient client, int inventorySlot) {
		if (inventorySlot == -1) return;
		
		var interactionManager = client.interactionManager;
		var player = client.player;
		if (interactionManager == null || player == null) return;
		
		// Use SlotActionType.SWAP with button 40 (offhand) to swap the inventory slot with offhand
		interactionManager.clickSlot(
			player.playerScreenHandler.syncId,
			inventorySlot,
			OFFHAND_SWAP_BUTTON,
			SlotActionType.SWAP,
			player
		);
	}

	/**
	 * Called when the user changes their offhand hotbar selection.
	 * Swaps the offhand with the newly selected inventory slot.
	 * 
	 * @param client The Minecraft client instance
	 */
	public static void onOffhandSlotChanged(MinecraftClient client) {
		if (selectedOffhandSlot == lastOffhandSlot) return;
		if (client.player == null) return;

		int newSlot = getOffhandHotbarSlot(selectedOffhandSlot);
		
		// Swap the offhand with the newly selected slot
		// This puts the new slot's item into the offhand, and the old offhand item into that slot
		swapWithOffhand(client, newSlot);
		
		lastOffhandSlot = selectedOffhandSlot;
	}

	/**
	 * Scrolls items through all three inventory rows.
	 * This is a power-user feature activated by holding SCROLL_INVENTORY_KEY while scrolling.
	 * 
	 * @param client The Minecraft client instance
	 * @param forward True to scroll forward, false to scroll backward
	 */
	public static void scrollInventoryRows(MinecraftClient client, boolean forward) {
		var player = client.player;
		var interactionManager = client.interactionManager;
		if (player == null || interactionManager == null) return;
		
		var syncId = player.playerScreenHandler.syncId;

		// Rotate items through each column of the inventory
		for (int column = 0; column < 9; column++) {
			for (int rowOffset : forward ? forwardSlotOrder : backwardSlotOrder) {
				interactionManager.clickSlot(
					syncId, 
					PlayerScreenHandler.INVENTORY_START + column + rowOffset, 
					0, 
					SlotActionType.PICKUP, 
					player
				);
			}
		}
	}

	// ==================== Initialization ====================

	@Override
	public void onInitializeClient() {
		// Reset state when disconnecting from a server
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			selectedOffhandSlot = 0;
			lastOffhandSlot = 0;
		});
	}

	@Override
	public void onInitialize() {
		MidnightConfig.init(MOD_ID, OffhandHotbarConfig.class);
	}
}