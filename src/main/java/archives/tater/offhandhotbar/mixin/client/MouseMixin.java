package archives.tater.offhandhotbar.mixin.client;

import archives.tater.offhandhotbar.OffhandHotbar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add the scroll inventory feature.
 * 
 * <h2>Functionality:</h2>
 * When the user holds SCROLL_INVENTORY_KEY and scrolls, this rotates items through
 * all three rows of the inventory instead of changing hotbar selection.
 */
@Mixin(Mouse.class)
public class MouseMixin {
	@Shadow @Final private MinecraftClient client;

	/**
	 * Intercepts scroll events to implement the scroll inventory feature.
	 * When SCROLL_INVENTORY_KEY is held, rotates items through all inventory rows.
	 */
	@Inject(
		method = "onMouseScroll",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"),
		cancellable = true
	)
	private void scrollInventory(long window, double horizontal, double vertical, CallbackInfo ci) {
		// Only activate when not in a screen
		if (client.currentScreen != null) return;
		
		// Only activate when the scroll inventory key is held
		if (!OffhandHotbar.SCROLL_INVENTORY_KEY.isPressed()) return;
		
		// Need actual scroll input
		if (vertical == 0) return;

		// Rotate items through inventory rows
		OffhandHotbar.scrollInventoryRows(client, vertical > 0);
		
		// Cancel normal scroll handling
		ci.cancel();
	}
}
