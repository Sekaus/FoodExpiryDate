package net.mcreator.foodexpirydate;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class Settings {

    // Config holder and spec
    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    // Cached runtime values
    private static volatile int radiusOfTheAreaToScanForBlocks = 8;
    private static volatile int daysBeforeItExpires = 5;
    private static volatile int daysBeforeItIsDried = 3;

    static {
        Pair<Common, ForgeConfigSpec> specPair =
                new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = specPair.getLeft();
        COMMON_SPEC = specPair.getRight();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

    /** Copy values from config holders into runtime fields. */
    private static void bakeConfig() {
        radiusOfTheAreaToScanForBlocks = COMMON.scanRadius.get();
        daysBeforeItExpires = COMMON.maxDays.get();
        daysBeforeItIsDried = COMMON.dryDays.get();
    }

    // -----------------------
    // Runtime getters
    // -----------------------

    public static int getRadiusOfTheAreaToScanForBlocks() {
        return radiusOfTheAreaToScanForBlocks;
    }

    public static int getDaysBeforeItExpires() {
        return daysBeforeItExpires;
    }

    public static int getDaysBeforeItIsDried() {
        return daysBeforeItIsDried;
    }

    public static List<? extends String> getExtraItems() {
        return COMMON.extraItems.get();
    }

    public static List<? extends String> getExtraBlocks() {
        return COMMON.extraBlocks.get();
    }

    // -----------------------
    // Config definition
    // -----------------------

    public static class Common {
        public final ForgeConfigSpec.IntValue scanRadius;
        public final ForgeConfigSpec.IntValue maxDays;
        public final ForgeConfigSpec.IntValue dryDays;

        public final ForgeConfigSpec.ConfigValue<List<? extends String>> extraItems;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> extraBlocks;

        Common(ForgeConfigSpec.Builder builder) {

            builder.push("general");

            scanRadius = builder
                    .comment("Radius (in blocks) around each player to scan for blocks.")
                    .defineInRange("scanRadius", 8, 1, 128);

            maxDays = builder
                    .comment("Max days before it expires.")
                    .defineInRange("maxDays", 5, 0, 365);

            dryDays = builder
                    .comment("Days before item is considered dried.")
                    .defineInRange("dryDays", 3, 0, 365);

            builder.pop();

            builder.push("expiry_overrides");

            extraItems = builder
                    .comment(
                        "Extra expirable ITEMS",
                        "Format: input_item=rotten_item",
                        "Example: minecraft:apple=food_expiry_date:moldy_food"
                    )
                    .defineListAllowEmpty(
                        List.of("extraItems"),
                        List.of(),
                        o -> o instanceof String
                    );

            extraBlocks = builder
                    .comment(
                        "Extra expirable BLOCKS",
                        "Format: input_block=rotten_block",
                        "Example: minecraft:hay_block=food_expiry_date:moldy_block"
                    )
                    .defineListAllowEmpty(
                        List.of("extraBlocks"),
                        List.of(),
                        o -> o instanceof String
                    );

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
            ThingsThatCanExpire.rebuildRegistry();
        }
    }

    @SubscribeEvent
    public static void onReload(final ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) {
            bakeConfig();
            ThingsThatCanExpire.rebuildRegistry();
        }
    }
}