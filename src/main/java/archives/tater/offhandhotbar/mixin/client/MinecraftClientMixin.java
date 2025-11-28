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

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Unique
    private static final Hand[] HANDS_INVERTED = {Hand.OFF_HAND, Hand.MAIN_HAND};

    @WrapOperation(
            method = "handleInputEvents",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I")
    )
    private void setOffhandSlot(PlayerInventory instance, int value, Operation<Void> original) {
        if (OffhandHotbarConfig.keyboardControls == Hand.OFF_HAND ^ OffhandHotbar.CONTROL_OPPOSITE_KEY.isPressed())
            OffhandHotbar.selectedOffhandSlot = value;
        else
            original.call(instance, value);
    }

    @WrapOperation(
            method = "handleInputEvents",
            at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/PlayerInventory;selectedSlot:I")
    )
    private void unswapItems(PlayerInventory instance, int value, Operation<Void> original) {
        if (!OffhandHotbar.focusSwapped) {
            original.call(instance, value);
            return;
        }
        OffhandHotbar.updateFocusSwap((MinecraftClient) (Object) this, false);
        original.call(instance, value);
        OffhandHotbar.updateFocusSwap((MinecraftClient) (Object) this, true);
    }

    @ModifyExpressionValue(
            method = "doItemUse",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Hand;values()[Lnet/minecraft/util/Hand;")
    )
    private Hand[] invertHandPriority(Hand[] original) {
        return OffhandHotbarConfig.invertItemUseHandPriority ? HANDS_INVERTED : original;
    }
}
