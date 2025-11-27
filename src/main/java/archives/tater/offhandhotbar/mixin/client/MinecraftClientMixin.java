package archives.tater.offhandhotbar.mixin.client;

import archives.tater.offhandhotbar.OffhandHotbar;
import archives.tater.offhandhotbar.OffhandHotbarConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin to intercept keyboard number key input for hotbar selection.
 * 
 * <h2>Functionality:</h2>
 * When the user presses 1-9 with CONTROL_OPPOSITE_KEY held (and keyboardControls is MAIN_HAND),
 * or when keyboardControls is OFF_HAND (without the key), this mixin redirects the
 * key press to change the offhand hotbar selection instead of the main hotbar.
 * 
 * Also inverts item use hand priority if configured.
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	
	/** Inverted hand order for item use priority */
	@Unique
	private static final Hand[] HANDS_INVERTED = {Hand.OFF_HAND, Hand.MAIN_HAND};

	/**
	 * Intercepts keyboard hotbar selection (number keys 1-9).
	 * If we should control the offhand hotbar, updates our selection and triggers a swap.
	 */
	@WrapOperation(
		method = "handleInputEvents",
		at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I")
	)
	private void setOffhandSlot(PlayerInventory instance, int value, Operation<Void> original) {
		// Determine if this key should control the offhand hotbar
		boolean controlOffhand = OffhandHotbarConfig.keyboardControls == Hand.OFF_HAND 
			^ OffhandHotbar.CONTROL_OPPOSITE_KEY.isPressed();
		
		if (controlOffhand) {
			// Update offhand selection and trigger the swap
			OffhandHotbar.selectedOffhandSlot = value;
			OffhandHotbar.onOffhandSlotChanged((MinecraftClient)(Object)this);
		} else {
			// Normal main hotbar selection
			original.call(instance, value);
		}
	}

	/**
	 * Inverts the order hands are checked when using items.
	 * This makes the offhand take priority over main hand for item use.
	 */
	@ModifyExpressionValue(
		method = "doItemUse",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Hand;values()[Lnet/minecraft/util/Hand;")
	)
	private Hand[] invertHandPriority(Hand[] original) {
		return OffhandHotbarConfig.invertItemUseHandPriority ? HANDS_INVERTED : original;
	}
}
