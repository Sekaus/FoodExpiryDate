package net.mcreator.foodexpirydate;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class Settings {

    // Config holder and spec
    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    // Cached runtime value (safe to access after bakeConfig() runs)
    private static volatile int radiusOfTheAreaToScanForBlocks = 8;
    private static volatile int daysBeforeItExpires = 5;
    private static volatile int daysBeforeItIsDried = 3;

    static {
        Pair<Common, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = specPair.getLeft();
        COMMON_SPEC = specPair.getRight();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

    /** Copy values from config holders into runtime fields. Call only after config is loaded. */
    private static void bakeConfig() {
        radiusOfTheAreaToScanForBlocks = COMMON.scanRadius.get();
        daysBeforeItExpires = COMMON.maxDays.get();     // <- updated to use IntValue.get()
        daysBeforeItIsDried = COMMON.dryDays.get();    // if you add a dryDays config below
    }

    public static int getRadiusOfTheAreaToScanForBlocks() {
        return radiusOfTheAreaToScanForBlocks;
    }

    public static int getDaysBeforeItExpires() {
        return daysBeforeItExpires;
    }

    public static int getDaysBeforeItIsDried() {
        return daysBeforeItIsDried;
    }

    public static class Common {
        public final ForgeConfigSpec.IntValue scanRadius;
        public final ForgeConfigSpec.IntValue maxDays;
        public final ForgeConfigSpec.IntValue dryDays;

        Common(ForgeConfigSpec.Builder builder) {
            builder.push("general");
            scanRadius = builder
                    .comment("Radius (in blocks) around each player to scan for blocks.")
                    .defineInRange("scanRadius", 8, 1, 128);
            // Use IntValue + defineInRange for ints (this is the correct Forge pattern).
            maxDays = builder
                    .comment("Max days before it expires (days that passed - creation date).")
                    .defineInRange("maxDays", 5, 0, 365);
            // optional extra config for drying time:
            dryDays = builder
                    .comment("Days before item is considered dried.")
                    .defineInRange("dryDays", 3, 0, 365);

            builder.pop();
        }
    }

    // -----------------------
    // Config load/reload events
    // -----------------------
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) {
            bakeConfig();
        }
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) {
            bakeConfig();
        }
    }
}