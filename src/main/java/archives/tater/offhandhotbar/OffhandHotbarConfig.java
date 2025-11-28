package archives.tater.offhandhotbar;

import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.util.Hand;

public class OffhandHotbarConfig extends MidnightConfig {
    public enum DisplayMode {
        SIDE_BY_SIDE, STACKED, STACKED_SWAPPED, VERTICAL, VERTICAL_SWAPPED;

        public final boolean isStacked() { return this == STACKED || this == STACKED_SWAPPED; }
        public final boolean isSwapped() { return this == STACKED_SWAPPED || this == VERTICAL_SWAPPED; }
    }

    @Entry
    public static DisplayMode displayMode = DisplayMode.SIDE_BY_SIDE;
    @Entry
    public static Hand scrollControls = Hand.MAIN_HAND;
    @Entry
    public static Hand keyboardControls = Hand.OFF_HAND;
    @Entry
    public static boolean invertItemUseHandPriority = true;

    @Comment(centered = true)
    public static Comment keybindInfo;
}
