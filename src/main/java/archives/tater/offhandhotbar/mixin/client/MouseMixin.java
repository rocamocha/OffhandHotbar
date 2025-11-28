package archives.tater.offhandhotbar.mixin.client;

import archives.tater.offhandhotbar.OffhandHotbar;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.entity.player.PlayerInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Shadow @Final private MinecraftClient client;

    @Inject(
            method = "onMouseScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"),
            cancellable = true
    )
    private void scrollInventory(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (client.currentScreen != null) return;
        if (!OffhandHotbar.SCROLL_INVENTORY_KEY.isPressed()) return;
        if (vertical == 0) return;

        OffhandHotbar.scrollInventory(client, vertical > 0);

        ci.cancel();
    }

    @WrapOperation(
            method = "onMouseScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;scrollInHotbar(D)V")
    )
    private void unswapFocus(PlayerInventory instance, double scrollAmount, Operation<Void> original) {
        if (!OffhandHotbar.focusSwapped) {
            original.call(instance, scrollAmount);
            return;
        }
        OffhandHotbar.updateFocusSwap(client, false);
        original.call(instance, scrollAmount);
        OffhandHotbar.updateFocusSwap(client, true);
    }
}
