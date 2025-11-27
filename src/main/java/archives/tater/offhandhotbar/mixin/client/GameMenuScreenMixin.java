package archives.tater.offhandhotbar.mixin.client;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin for the game menu screen.
 * 
 * <h2>Note:</h2>
 * This mixin was previously used to handle item swapping when disconnecting.
 * With the simplified architecture, continuous swapping has been removed,
 * so this mixin is no longer needed but kept for potential future use.
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
	protected GameMenuScreenMixin(Text title) {
		super(title);
	}
	
	// No longer needed - the simplified architecture doesn't continuously swap items
}
