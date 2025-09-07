package net.mcreator.foodexpirydate;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.mcreator.foodexpirydate.block.entity.FoodDryingRackBlockEntity;

/** Keeps track of all loaded FoodDryingRackBlockEntity instances so we can clean them up on server stop. */
public class DisplayRegistry {
    private static final Set<FoodDryingRackBlockEntity> INSTANCES =
            ConcurrentHashMap.newKeySet();

    public static void register(FoodDryingRackBlockEntity be) {
        if (be != null) INSTANCES.add(be);
    }

    public static void unregister(FoodDryingRackBlockEntity be) {
        if (be != null) INSTANCES.remove(be);
    }

    /** Called by ServerStoppingEvent to remove all displays from every registered BE. */
    public static void cleanupAll() {
        for (FoodDryingRackBlockEntity be : INSTANCES) {
            try {
                be.removeAlldisplayedItems();
            } catch (Throwable t) {
                System.err.println("[CustomDisplay] Error cleaning displays for " + be + ": " + t);
            }
        }
        INSTANCES.clear();
    }
}
