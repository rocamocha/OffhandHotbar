package archives.tater.offhandhotbar.mixin.client;

import archives.tater.offhandhotbar.OffhandHotbar;
import archives.tater.offhandhotbar.OffhandHotbarConfig;
import archives.tater.offhandhotbar.OffhandHotbarConfig.DisplayMode;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.RotationAxis;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static archives.tater.offhandhotbar.OffhandHotbar.getOffhandHotbarSlot;

/**
 * Mixin to render the offhand hotbar alongside the main hotbar.
 * 
 * <h2>Rendering Strategy:</h2>
 * We wrap the vanilla renderHotbar method and call it twice:
 * <ol>
 *   <li>First call: Renders the main hotbar (offhand flag = false)</li>
 *   <li>Second call: Renders the offhand hotbar (offhand flag = true)</li>
 * </ol>
 * 
 * <h2>Key Modifications:</h2>
 * <ul>
 *   <li>{@link #shiftHotbar}: Positions the two hotbars based on display mode</li>
 *   <li>{@link #modifyDisplayedItem}: Shows correct items for each hotbar</li>
 *   <li>{@link #useOffhandSlot}: Highlights the correct selected slot</li>
 *   <li>{@link #hideVanillaOffhand}: Hides the vanilla offhand display</li>
 * </ul>
 */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Shadow
	protected abstract @Nullable PlayerEntity getCameraPlayer();

	// ==================== HUD Layout Adjustments ====================

	/**
	 * Shifts the entire main HUD up when in stacked display mode to make room for both hotbars.
	 */
	@Inject(method = "renderMainHud", at = @At(value = "HEAD"))
	private void shiftHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (!OffhandHotbarConfig.displayMode.isStacked()) return;
		context.getMatrices().push();
		context.getMatrices().translate(0, OffhandHotbar.HOTBAR_Y_OFFSET, 0);
	}

	/**
	 * Restores matrix state after rendering in stacked mode.
	 */
	@Inject(method = "renderMainHud", at = @At("TAIL"))
	private void unshiftHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (!OffhandHotbarConfig.displayMode.isStacked()) return;
		context.getMatrices().pop();
	}

	/**
	 * Adjusts XP level text position in stacked display mode.
	 */
	@ModifyVariable(method = "renderExperienceLevel", ordinal = 2, at = @At(value = "STORE"))
	private int shiftXpLevel(int value) {
		return OffhandHotbarConfig.displayMode.isStacked() ? value + OffhandHotbar.HOTBAR_Y_OFFSET : value;
	}

	// ==================== Dual Hotbar Rendering ====================

	/**
	 * Helper method for rotating the hotbar in vertical display modes.
	 */
	@Unique
	private void offhandhotbar$hotbarRotate(DrawContext context, boolean leftSide) {
		context.getMatrices().translate(
			leftSide
				? context.getScaledWindowHeight()
				: context.getScaledWindowHeight() + context.getScaledWindowWidth() - OffhandHotbar.HOTBAR_HEIGHT,
			-(context.getScaledWindowWidth() - context.getScaledWindowHeight()) / 2f,
			0
		);
		context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90));
	}

	/**
	 * Wraps the renderHotbar method to render both main and offhand hotbars.
	 * 
	 * The shared "offhand" boolean tracks which hotbar we're currently rendering:
	 * - false = main hotbar (slots 0-8)
	 * - true = offhand hotbar (inventory slots 27-35)
	 */
	@WrapMethod(method = "renderHotbar")
	private void shiftHotbar(DrawContext context, RenderTickCounter tickCounter, Operation<Void> original, 
			@Share("offhand") LocalBooleanRef offhand) {
		var cameraPlayer = getCameraPlayer();
		var mainArm = cameraPlayer == null ? Arm.RIGHT : cameraPlayer.getMainArm();
		var mx = mainArm == Arm.RIGHT ? 1 : -1;
		var matrices = context.getMatrices();

		matrices.push();
		
		// Position for main hotbar based on display mode
		switch (OffhandHotbarConfig.displayMode) {
			case SIDE_BY_SIDE -> matrices.translate(mx * OffhandHotbar.HOTBAR_X_OFFSET, 0, 0);
			case STACKED -> {
				matrices.push();
				matrices.translate(0, -OffhandHotbar.HOTBAR_Y_OFFSET, 0);
			}
			case VERTICAL_SWAPPED -> {
				matrices.push();
				offhandhotbar$hotbarRotate(context, mainArm == Arm.LEFT);
			}
			// STACKED_SWAPPED and VERTICAL: no initial offset needed, handled after first render
			default -> { /* No positioning needed for first hotbar */ }
		}
		
		// Render main hotbar
		offhand.set(false);
		original.call(context, tickCounter);
		
		// Reposition for offhand hotbar
		switch (OffhandHotbarConfig.displayMode) {
			case SIDE_BY_SIDE -> matrices.translate(-mx * OffhandHotbar.HOTBAR_X_OFFSET * 2, 0, 0);
			case STACKED, VERTICAL_SWAPPED -> matrices.pop();
			case STACKED_SWAPPED -> matrices.translate(0, -OffhandHotbar.HOTBAR_Y_OFFSET, 0);
			case VERTICAL -> offhandhotbar$hotbarRotate(context, mainArm == Arm.RIGHT);
		}
		
		// Render offhand hotbar
		offhand.set(true);
		original.call(context, tickCounter);
		
		matrices.pop();
	}

	// ==================== Item Display Modifications ====================

	/**
	 * Fixes a visual glitch with the selection border.
	 */
	@WrapOperation(
		method = "renderHotbar",
		slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/hud/InGameHud;HOTBAR_SELECTION_TEXTURE:Lnet/minecraft/util/Identifier;")),
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lnet/minecraft/util/Identifier;IIII)V", ordinal = 0)
	)
	private void fixSelectionBottomBorder(DrawContext instance, Identifier texture, int x, int y, int width, int height, Operation<Void> original) {
		original.call(instance, texture, x, y, width, height);
		instance.drawGuiTexture(texture, 24, 23, 0, 0, x, y + height, width, 1);
	}

	/**
	 * Rotates items in vertical display modes.
	 */
	@WrapOperation(
		method = "renderHotbar",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderHotbarItem(Lnet/minecraft/client/gui/DrawContext;IILnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/ItemStack;I)V")
	)
	private void rotateHotbarItem(InGameHud instance, DrawContext context, int x, int y, RenderTickCounter tickCounter, 
			PlayerEntity player, ItemStack stack, int seed, Operation<Void> original, 
			@Share("offhand") LocalBooleanRef offhand) {
		if (OffhandHotbarConfig.displayMode != (offhand.get() ? DisplayMode.VERTICAL : DisplayMode.VERTICAL_SWAPPED)) {
			original.call(instance, context, x, y, tickCounter, player, stack, seed);
			return;
		}
		context.getMatrices().push();
		context.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90), x + 8, y + 8, 0);
		original.call(instance, context, x, y, tickCounter, player, stack, seed);
		context.getMatrices().pop();
	}

	/**
	 * Modifies which items are displayed in each hotbar slot.
	 * 
	 * For the main hotbar (offhand=false): Shows normal hotbar slots (0-8)
	 * For the offhand hotbar (offhand=true): Shows inventory slots 27-35,
	 * except for the selected slot which shows the actual offhand item.
	 * 
	 * This way, the selected slot always displays what's currently equipped
	 * in the offhand, matching the expected behavior.
	 */
	@WrapOperation(
		method = "renderHotbar",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/collection/DefaultedList;get(I)Ljava/lang/Object;")
	)
	private Object modifyDisplayedItem(DefaultedList<ItemStack> instance, int index, Operation<ItemStack> original,
			@Local ItemStack offhandStack, @Local PlayerEntity player, @Share("offhand") LocalBooleanRef offhand) {
		// Main hotbar: show normal items
		if (!offhand.get()) {
			return original.call(instance, index);
		}
		
		// Offhand hotbar: selected slot shows offhand item, others show inventory
		if (index == OffhandHotbar.selectedOffhandSlot) {
			return offhandStack;
		}
		return original.call(instance, getOffhandHotbarSlot(index));
	}

	/**
	 * Hides the vanilla offhand item display (the small slot on the opposite side).
	 * We show the offhand through our second hotbar instead.
	 * 
	 * This affects all isEmpty() checks in renderHotbar to ensure the vanilla
	 * offhand slot rendering is completely suppressed.
	 */
	@ModifyExpressionValue(
		method = "renderHotbar",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z")
	)
	private boolean hideVanillaOffhand(boolean original) {
		return true;
	}

	/**
	 * Returns the correct selected slot for each hotbar.
	 * Main hotbar: uses the player's actual selected slot
	 * Offhand hotbar: uses our tracked selectedOffhandSlot
	 */
	@ModifyExpressionValue(
		method = "renderHotbar",
		at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I")
	)
	private int useOffhandSlot(int original, @Share("offhand") LocalBooleanRef offhand) {
		return offhand.get() ? OffhandHotbar.selectedOffhandSlot : original;
	}

	/**
	 * Cancels early to prevent double rendering of certain elements.
	 * Without this, some mod compatibility issues and double-rendering can occur.
	 */
	@Inject(
		method = "renderHotbar",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 1),
		cancellable = true
	)
	private void earlyReturn(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci, 
			@Share("offhand") LocalBooleanRef offhand) {
		if (offhand.get() ^ OffhandHotbarConfig.displayMode.isSwapped()) {
			ci.cancel();
		}
	}
}