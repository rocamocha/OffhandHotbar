package archives.tater.offhandhotbar;

import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.util.Hand;

/**
 * Configuration options for the Offhand Hotbar mod.
 * Uses MidnightLib for the config UI.
 */
public class OffhandHotbarConfig extends MidnightConfig {
	
	/**
	 * Display modes for positioning the offhand hotbar relative to the main hotbar.
	 */
	public enum DisplayMode {
		/** Main hotbar on right, offhand hotbar on left (or vice versa based on arm) */
		SIDE_BY_SIDE,
		/** Main hotbar on bottom, offhand hotbar stacked above */
		STACKED,
		/** Offhand hotbar on bottom, main hotbar stacked above */
		STACKED_SWAPPED,
		/** Hotbars arranged vertically on the side */
		VERTICAL,
		/** Hotbars arranged vertically on the side (swapped positions) */
		VERTICAL_SWAPPED;

		/** Returns true if the display mode uses stacked (vertical) positioning */
		public final boolean isStacked() { 
			return this == STACKED || this == STACKED_SWAPPED; 
		}
		
		/** Returns true if the offhand hotbar should be rendered first (in the "main" position) */
		public final boolean isSwapped() { 
			return this == STACKED_SWAPPED || this == VERTICAL_SWAPPED; 
		}
	}

	/** How the two hotbars are positioned on screen */
	@Entry
	public static DisplayMode displayMode = DisplayMode.SIDE_BY_SIDE;
	
	/** 
	 * Which hotbar the scroll wheel controls by default.
	 * Hold CONTROL_OPPOSITE_KEY to control the other hotbar.
	 */
	@Entry
	public static Hand scrollControls = Hand.MAIN_HAND;
	
	/** 
	 * Which hotbar the number keys (1-9) control by default.
	 * Hold CONTROL_OPPOSITE_KEY to control the other hotbar.
	 */
	@Entry
	public static Hand keyboardControls = Hand.OFF_HAND;
	
	/** 
	 * When true, item use checks the offhand before the main hand.
	 * Useful for shields and other offhand items.
	 */
	@Entry
	public static boolean invertItemUseHandPriority = true;

	@Comment(centered = true)
	public static Comment keybindInfo;
}
