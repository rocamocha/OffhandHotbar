package archives.tater.offhandhotbar.mixin.client;

import archives.tater.offhandhotbar.OffhandHotbar;
import archives.tater.offhandhotbar.OffhandHotbarConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Hand;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Mixin to intercept scroll wheel input for hotbar selection.
 * 
 * <h2>Functionality:</h2>
 * When the user scrolls with CONTROL_OPPOSITE_KEY held (and scrollControls is set to MAIN_HAND),
 * or when scrollControls is set to OFF_HAND (without the key), this mixin redirects the
 * scroll to change the offhand hotbar selection instead of the main hotbar.
 */
@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
	
	/**
	 * Intercepts the assignment of selectedSlot in scrollInHotbar.
	 * If we should control the offhand hotbar, updates our selection and triggers a swap.
	 */
	@WrapOperation(
		method = "scrollInHotbar",
		at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I", opcode = Opcodes.PUTFIELD)
	)
	private void scrollOffhand(PlayerInventory instance, int value, Operation<Void> original) {
		// Determine if this scroll should control the offhand hotbar
		boolean controlOffhand = OffhandHotbarConfig.scrollControls == Hand.OFF_HAND 
			^ OffhandHotbar.CONTROL_OPPOSITE_KEY.isPressed();
		
		if (controlOffhand) {
			// Update offhand selection and trigger the swap
			OffhandHotbar.selectedOffhandSlot = value;
			OffhandHotbar.onOffhandSlotChanged(MinecraftClient.getInstance());
		} else {
			// Normal main hotbar scroll
			original.call(instance, value);
		}
	}

	/**
	 * When reading the current selectedSlot for scroll calculations,
	 * return the offhand slot if we're controlling the offhand hotbar.
	 */
	@ModifyExpressionValue(
		method = "scrollInHotbar",
		at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I", opcode = Opcodes.GETFIELD)
	)
	private int getScrollBaseSlot(int original) {
		boolean controlOffhand = OffhandHotbarConfig.scrollControls == Hand.OFF_HAND 
			^ OffhandHotbar.CONTROL_OPPOSITE_KEY.isPressed();
		
		return controlOffhand ? OffhandHotbar.selectedOffhandSlot : original;
	}
}
