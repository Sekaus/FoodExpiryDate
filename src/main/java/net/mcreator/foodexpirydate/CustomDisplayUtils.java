package net.mcreator.foodexpirydate;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.DoubleTag;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Robust utility for spawning item display entities across mapping sets.
 * - Server-only spawn (ServerLevel.addFreshEntity)
 * - Best-effort: tries direct API then reflection for methods that differ by mappings
 * - Emits debug prints prefixed with [CustomDisplay]
 */
public class CustomDisplayUtils {

    private static final boolean DEBUG = false;

    public static Entity spawnItemDisplay(Level world, double x, double y, double z, Item item, float rotX, float rotY, float rotZ, float scale, boolean showPedestal) {
	    try {
	        if (world == null || item == null) {
	            System.out.println("[CustomDisplay] world or item is null. Aborting spawn.");
	            return null;
	        }
	
	        if (!(world instanceof ServerLevel)) {
	            System.out.println("[CustomDisplay] spawn attempted on non-server world: " + world.getClass().getName());
	            return null;
	        }
	        ServerLevel server = (ServerLevel) world;
	
	        Entity entity = EntityType.ITEM_DISPLAY.create(server);
	        if (entity == null) {
	            System.out.println("[CustomDisplay] EntityType.ITEM_DISPLAY.create(world) returned null");
	            return null;
	        }
	
	        // Position the entity (try moveTo/moveTo with pitch/yaw, setPos fallbacks)
	        boolean moved = tryInvokeMoveTo(entity, x, y, z, rotY, rotX);
	        if (!moved) {
	            invokeBest(entity, new String[]{"setPos", "setPosition", "setLocation"}, new Class<?>[]{double.class, double.class, double.class}, x, y, z);
	        }
	
	        // Build the stack
	        ItemStack stack = new ItemStack(item);
	
	        // Build robust NBT for initial configuration: item, Pos, Rotation, and transformation (scale as doubles)
	        CompoundTag entityNbt = null;
	        try {
	            entityNbt = new CompoundTag();
	
	            // Item NBT
	            CompoundTag itemTag = new CompoundTag();
	            stack.save(itemTag);
	            entityNbt.put("item", itemTag.copy());
	            entityNbt.put("Item", itemTag.copy());
	
	            // Pos (doubles) so Entity.load doesn't reset position
	            ListTag posList = new ListTag();
	            posList.add(net.minecraft.nbt.DoubleTag.valueOf(x));
	            posList.add(net.minecraft.nbt.DoubleTag.valueOf(y));
	            posList.add(net.minecraft.nbt.DoubleTag.valueOf(z));
	            entityNbt.put("Pos", posList);
	
	            // Rotation (yaw,pitch) as floats
	            ListTag rotList = new ListTag();
	            rotList.add(net.minecraft.nbt.FloatTag.valueOf(rotY));
	            rotList.add(net.minecraft.nbt.FloatTag.valueOf(rotX));
	            entityNbt.put("Rotation", rotList);
	
	            // transformation: translation=0, scale=double list
	            CompoundTag transformation = new CompoundTag();
	            ListTag translation = new ListTag();
	            translation.add(net.minecraft.nbt.DoubleTag.valueOf(0.0));
	            translation.add(net.minecraft.nbt.DoubleTag.valueOf(0.0));
	            translation.add(net.minecraft.nbt.DoubleTag.valueOf(0.0));
	            transformation.put("translation", translation);
	
	            ListTag scaleList = new ListTag();
	            scaleList.add(net.minecraft.nbt.DoubleTag.valueOf(scale));
	            scaleList.add(net.minecraft.nbt.DoubleTag.valueOf(scale));
	            scaleList.add(net.minecraft.nbt.DoubleTag.valueOf(scale));
	            transformation.put("scale", scaleList);
	
	            // include the transformation compound
	            entityNbt.put("transformation", transformation);
	        } catch (Throwable t) {
	            if (DEBUG) System.err.println("[CustomDisplay] failed building transformation NBT: " + t);
	        }
	
	        // --- Try: set item reflectively pre-add (fast path) ---
	        boolean itemSet = false;
	        try {
	            itemSet = trySetDisplayItem(entity, stack);
	            if (itemSet && DEBUG) System.out.println("[CustomDisplay] setItem via reflection (pre-add) succeeded.");
	        } catch (Throwable t) {
	            if (DEBUG) System.err.println("[CustomDisplay] reflection setItem (pre-add) failed: " + t);
	        }
	
	        // Try to set the synched scale via EntityDataAccessor pre-add
	        boolean scaleSet = false;
	        try {
	            scaleSet = attemptSetScaleViaEntityData(entity, scale);
	            if (scaleSet && DEBUG) System.out.println("[CustomDisplay] attemptSetScaleViaEntityData succeeded (pre-add).");
	        } catch (Throwable t) {
	            if (DEBUG) System.err.println("[CustomDisplay] attemptSetScaleViaEntityData (pre-add) threw: " + t);
	        }
	
	        // If reflection didn't set the item or scale, try applying NBT pre-add
	        if ((!itemSet || !scaleSet) && entityNbt != null) {
	            if (DEBUG) System.out.println("[CustomDisplay] attempting Entity.load(CompoundTag) pre-add with NBT");
	            applyEntityNbtIfPossible(entity, entityNbt);
	        }
	
	        // Try setting pose reflectively (pre-add)
	        boolean poseSet = false;
	        try {
	            poseSet = attemptSetPose(entity, scale, showPedestal);
	            if (poseSet && DEBUG) System.out.println("[CustomDisplay] attemptSetPose succeeded (pre-add).");
	        } catch (Throwable t) {
	            if (DEBUG) System.err.println("[CustomDisplay] attemptSetPose (pre-add) threw: " + t);
	        }
	
	        // Add to server level so clients get the entity packets
	        server.addFreshEntity(entity);
	
	        // Post-add retries: reflection for item/pose/scale might only work after tracking begins
	        if (!itemSet) {
	            try {
	                itemSet = trySetDisplayItem(entity, stack);
	                if (itemSet && DEBUG) System.out.println("[CustomDisplay] setItem via reflection succeeded after addFreshEntity.");
	            } catch (Throwable t) {
	                if (DEBUG) System.err.println("[CustomDisplay] reflection setItem (post-add) failed: " + t);
	            }
	        }
	
	        if (!poseSet) {
	            try {
	                poseSet = attemptSetPose(entity, scale, showPedestal);
	                if (poseSet && DEBUG) System.out.println("[CustomDisplay] attemptSetPose succeeded after addFreshEntity.");
	            } catch (Throwable t) {
	                if (DEBUG) System.err.println("[CustomDisplay] attemptSetPose (post-add) failed: " + t);
	            }
	        }
	
	        if (!scaleSet) {
	            try {
	                scaleSet = attemptSetScaleViaEntityData(entity, scale);
	                if (scaleSet && DEBUG) System.out.println("[CustomDisplay] attemptSetScaleViaEntityData succeeded after addFreshEntity.");
	            } catch (Throwable t) {
	                if (DEBUG) System.err.println("[CustomDisplay] attemptSetScaleViaEntityData (post-add) failed: " + t);
	            }
	        }
	
	        // Final NBT fallback post-add if needed
	        if ((!itemSet || !poseSet || !scaleSet) && entityNbt != null) {
	            if (DEBUG) System.out.println("[CustomDisplay] reapplying entity NBT post-add as fallback");
	            applyEntityNbtIfPossible(entity, entityNbt);
	        }
	
	        System.out.println("[CustomDisplay] spawned entity type=" + entity.getType() + " class=" + entity.getClass().getName() +
	                " itemSet=" + itemSet + " poseSet=" + poseSet + " scaleSet=" + scaleSet +
	                " pos=" + entity.getX() + "," + entity.getY() + "," + entity.getZ());
	        return entity;
	
	    } catch (Throwable t) {
	        System.out.println("[CustomDisplay] spawnItemDisplay exception: " + t);
	        t.printStackTrace();
	        return null;
	    }
	}
	
	/**
	 * Attempt to set the display scale by finding the EntityDataAccessor field on the runtime Display class
	 * and setting the value to a org.joml.Vector3f(scale,scale,scale) (or a fallback vector type).
	 * Returns true if a set was performed successfully.
	 */
	private static boolean attemptSetScaleViaEntityData(Entity entity, float scale) {
	    if (entity == null) return false;
	    try {
	        Class<?> cls = entity.getClass();
	        // The accessor type
	        Class<?> accessorClass = null;
	        try {
	            accessorClass = Class.forName("net.minecraft.network.syncher.EntityDataAccessor");
	        } catch (ClassNotFoundException ignored) {
	            try { accessorClass = Class.forName("net.minecraft.network.syncher.DataParameter"); } catch (ClassNotFoundException e) {}
	        }
	        if (accessorClass == null) return false;
	
	        // Walk class + superclasses for candidate static fields
	        Class<?> cur = cls;
	        while (cur != null) {
	            for (Field f : cur.getDeclaredFields()) {
	                if (!accessorClass.isAssignableFrom(f.getType())) continue;
	                String fname = f.getName().toLowerCase();
	                if (!(fname.contains("scale") || fname.contains("transformation") || fname.contains("data_scale") || fname.contains("scale_id")))
	                    continue;
	
	                f.setAccessible(true);
	                Object accessor = null;
	                try {
	                    accessor = f.get(null); // static field
	                } catch (IllegalAccessException ignored) {
	                }
	                if (accessor == null) continue;
	
	                // Obtain the synched data object
	                Method getEntityData = null;
	                try { getEntityData = cls.getMethod("getEntityData"); } catch (NoSuchMethodException ignored) {}
	                if (getEntityData == null) {
	                    try { getEntityData = cls.getMethod("getSynchedEntityData"); } catch (NoSuchMethodException ignored) {}
	                }
	                if (getEntityData == null) continue;
	                Object synched = getEntityData.invoke(entity);
	                if (synched == null) continue;
	
	                // Build a vector instance reflectively (try common vector classes)
	                Class<?> vectorCls = null;
	                try { vectorCls = Class.forName("org.joml.Vector3f"); } catch (ClassNotFoundException ignored) {}
	                if (vectorCls == null) {
	                    try { vectorCls = Class.forName("org.joml.Vector3d"); } catch (ClassNotFoundException ignored) {}
	                }
	                if (vectorCls == null) {
	                    // some runtimes might wrap floats in custom vector; try float array fallback by setting via synched.set(accessor, floatArray)
	                    // We'll try to find a 'set' method that accepts (EntityDataAccessor, Object)
	                }
	
	                Object vecInstance = null;
	                if (vectorCls != null) {
	                    try {
	                        Constructor<?> ctor = vectorCls.getConstructor(float.class, float.class, float.class);
	                        vecInstance = ctor.newInstance(scale, scale, scale);
	                    } catch (NoSuchMethodException nsme) {
	                        try {
	                            Constructor<?> ctor2 = vectorCls.getConstructor(double.class, double.class, double.class);
	                            vecInstance = ctor2.newInstance((double) scale, (double) scale, (double) scale);
	                        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
	                            vecInstance = null;
	                        }
	                    }
	                }
	
	                // Find synched.set(accessor, value) method
	                Method setMethod = null;
	                for (Method m : synched.getClass().getMethods()) {
	                    if (m.getName().equals("set") && m.getParameterTypes().length == 2) {
	                        setMethod = m;
	                        break;
	                    }
	                }
	                if (setMethod != null && vecInstance != null) {
	                    setMethod.invoke(synched, accessor, vecInstance);
	                    if (DEBUG) System.out.println("[CustomDisplay] set scale via EntityDataAccessor field " + f.getName());
	                    return true;
	                } else if (setMethod != null && vecInstance == null) {
	                    // try to set float[] as fallback
	                    try {
	                        float[] arr = new float[]{scale, scale, scale};
	                        setMethod.invoke(synched, accessor, arr);
	                        if (DEBUG) System.out.println("[CustomDisplay] set scale via float[] fallback on " + f.getName());
	                        return true;
	                    } catch (Throwable ignored) {}
	                }
	            }
	            cur = cur.getSuperclass();
	        }
	    } catch (Throwable t) {
	        if (DEBUG) {
	            System.err.println("[CustomDisplay] attemptSetScaleViaEntityData failure: " + t);
	            t.printStackTrace();
	        }
	    }
	    return false;
	}
	
	/** Helper: tries several common entity-load method names to apply NBT to an entity instance. */
	private static void applyEntityNbtIfPossible(Entity entity, CompoundTag nbt) {
	    if (entity == null || nbt == null) return;
	    Class<?> cls = entity.getClass();
	
	    // Preferred: public/protected load(CompoundTag)
	    try {
	        Method load = null;
	        try {
	            load = cls.getMethod("load", CompoundTag.class);
	        } catch (NoSuchMethodException ignored) {}
	        if (load == null) {
	            // search declared methods up class chain
	            Class<?> cur = cls;
	            while (cur != null && load == null) {
	                try {
	                    load = cur.getDeclaredMethod("load", CompoundTag.class);
	                } catch (NoSuchMethodException ignored) {
	                }
	                cur = cur.getSuperclass();
	            }
	        }
	
	        // fallback common names used in mappings
	        if (load == null) {
	            try {
	                load = cls.getMethod("readAdditionalSaveData", CompoundTag.class);
	            } catch (NoSuchMethodException ignored) {}
	        }
	        if (load == null) {
	            try {
	                load = cls.getMethod("read", CompoundTag.class);
	            } catch (NoSuchMethodException ignored) {}
	        }
	
	        if (load != null) {
	            load.setAccessible(true);
	            try {
	                load.invoke(entity, nbt);
	                if (DEBUG) System.out.println("[CustomDisplay] applied NBT via " + load + " to " + cls.getName());
	                return;
	            } catch (IllegalAccessException | InvocationTargetException e) {
	                if (DEBUG) System.err.println("[CustomDisplay] applying NBT via " + load + " failed: " + e);
	            }
	        } else {
	            if (DEBUG) System.out.println("[CustomDisplay] no Entity.load-like method found on " + cls.getName());
	        }
	    } catch (Throwable t) {
	        if (DEBUG) System.err.println("[CustomDisplay] unexpected error trying to apply NBT: " + t);
	    }
	
	    // Last-resort: try to set synched data fields that look like an item holder (may not change scale but may set item)
	    try {
	        trySetDisplayItem(entity, new ItemStack(net.minecraft.world.item.Items.AIR)); // harmless probe
	    } catch (Throwable ignored) {}
	}

    // try direct setter that some mappings expose on ItemDisplay for a pose; returns true if successful
    private static boolean attemptSetPoseDirect(Display.ItemDisplay display, float scale, boolean showPedestal) {
        try {
            // try known direct method names in various mappings
            Class<?> transformClass = null;
            try {
                transformClass = Class.forName("net.minecraft.world.entity.Display$DisplayTransform");
            } catch (ClassNotFoundException ignored) {
                try {
                    transformClass = Class.forName("net.minecraft.world.entity.display.Display$DisplayTransform");
                } catch (ClassNotFoundException ignored2) {
                }
            }
            if (transformClass != null) {
                Constructor<?> ctor = null;
                for (Constructor<?> c : transformClass.getConstructors()) {
                    if (c.getParameterTypes().length == 7) {
                        ctor = c;
                        break;
                    }
                }
                if (ctor != null) {
                    Object transform = ctor.newInstance(scale, scale, scale, 0f, 0f, 0f, showPedestal);
                    // try setter names
                    for (String name : new String[]{"setPose", "setTransformation", "setDisplayTransform"}) {
                        try {
                            Method m = display.getClass().getMethod(name, transformClass);
                            m.invoke(display, transform);
                            return true;
                        } catch (NoSuchMethodException ignored) {
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Attempts to set pose/transform using reflection trying multiple possible
     * class names for the DisplayTransform type and method names for setter.
     */
    private static boolean attemptSetPose(Object entity, float scale, boolean showPedestal) {
        String[] displayTransformNames = new String[]{
                "net.minecraft.world.entity.Display$DisplayTransform",
                "net.minecraft.entity.decoration.DisplayEntity$DisplayTransform",
                "net.minecraft.world.entity.display.Display$DisplayTransform",
                "net.minecraft.entity.display.DisplayEntity$DisplayTransform"
        };
        String[] transformSetterNames = new String[]{"setPose", "setTransformation", "setDisplayTransform", "setPoseTransform", "setTransformationNbt"};
        for (String clsName : displayTransformNames) {
            try {
                Class<?> transformClass = Class.forName(clsName);
                Constructor<?> ctor = null;
                for (Constructor<?> c : transformClass.getConstructors()) {
                    Class<?>[] p = c.getParameterTypes();
                    if (p.length == 7) {
                        ctor = c;
                        break;
                    }
                }
                if (ctor == null) continue;
                Object transform = ctor.newInstance(scale, scale, scale, 0f, 0f, 0f, showPedestal);
                for (String setter : transformSetterNames) {
                    try {
                        Method m = entity.getClass().getMethod(setter, transformClass);
                        m.invoke(entity, transform);
                        return true;
                    } catch (NoSuchMethodException ignored) {
                    }
                }
            } catch (ClassNotFoundException ignored) {
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException ignored) {
            }
        }
        return false;
    }

    /**
     * Best-effort reflective invocation: tries the provided method names in order,
     * returns true if any invocation succeeded.
     */
    private static boolean invokeBest(Object target, String[] methodNames, Class<?>[] paramTypes, Object... args) {
        Class<?> cls = target.getClass();
        for (String name : methodNames) {
            try {
                Method m = cls.getMethod(name, paramTypes);
                m.setAccessible(true);
                m.invoke(target, args);
                return true;
            } catch (NoSuchMethodException ignored) {
            } catch (IllegalAccessException | InvocationTargetException e) {
                System.err.println("[CustomDisplay] invocation failed for " + name + " on " + cls.getSimpleName() + " : " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    /**
     * Try moveTo with yaw/pitch signature, fall back to moveTo(x,y,z) or setPos variants.
     */
    private static boolean tryInvokeMoveTo(Object entity, double x, double y, double z, float yaw, float pitch) {
        Class<?> cls = entity.getClass();
        // try moveTo(double,double,double,float,float)
        try {
            Method m = cls.getMethod("moveTo", double.class, double.class, double.class, float.class, float.class);
            m.invoke(entity, x, y, z, yaw, pitch);
            return true;
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.err.println("[CustomDisplay] moveTo(yaw/pitch) failed: " + e.getMessage());
        }
        // try moveTo(double,double,double)
        try {
            Method m2 = cls.getMethod("moveTo", double.class, double.class, double.class);
            m2.invoke(entity, x, y, z);
            return true;
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.err.println("[CustomDisplay] moveTo(xyz) failed: " + e.getMessage());
        }
        // fallback: setPos(x,y,z)
        try {
            Method s = cls.getMethod("setPos", double.class, double.class, double.class);
            s.invoke(entity, x, y, z);
            return true;
        } catch (NoSuchMethodException ignored) {
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.err.println("[CustomDisplay] setPos failed: " + e.getMessage());
        }
        return false;
    }

    // convenience helpers to change spawned displays later
    public static void setItemDisplayItem(Entity entity, Item item) {
        if (entity == null || item == null) return;
        ItemStack stack = new ItemStack(item);
        boolean ok = trySetDisplayItem(entity, stack);
        if (!ok) System.out.println("[CustomDisplay] setItemDisplayItem: failed to set item for " + entity.getClass().getName());
    }

    public static void setItemDisplayRotation(Entity entity, float rotX, float rotY, float rotZ) {
        if (entity == null) return;
        boolean ok = invokeBest(entity, new String[]{"setRotation", "setRotationVec", "setRotationVec3d"}, new Class<?>[]{float.class, float.class, float.class}, rotX, rotY, rotZ);
        if (!ok) {
            invokeBest(entity, new String[]{"setYRot", "setYaw"}, new Class<?>[]{float.class}, rotY);
            invokeBest(entity, new String[]{"setXRot", "setPitch"}, new Class<?>[]{float.class}, rotX);
        }
    }

    public static void setItemDisplayScaleAndPedestal(Entity entity, float scale, boolean showPedestal) {
        if (entity == null) return;
        if (!attemptSetPose(entity, scale, showPedestal)) {
            System.out.println("[CustomDisplay] setItemDisplayScaleAndPedestal: failed to set pose on " + entity.getClass().getName());
        }
    }

    /**
     * Try many reflective ways to set an ItemStack on a display-like entity.
     * It searches declared methods (including non-public) on the class + superclasses, preferring names that contain item/stack/display/set.
     * If no method is found, it falls back to searching for a static EntityDataAccessor field and uses the entity's synched data 'set' method.
     */
    public static boolean trySetDisplayItem(Entity entity, ItemStack stack) {
        if (entity == null || stack == null) return false;

        Class<?> cls = entity.getClass();

        // 1) Search declared methods in class + superclasses for any single-param method accepting ItemStack
        Class<?> cur = cls;
        while (cur != null) {
            for (Method m : cur.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1 && ItemStack.class.isAssignableFrom(params[0])) {
                    String name = m.getName().toLowerCase();
                    // prefer methods whose name suggests item/stack/display/set
                    if (name.contains("item") || name.contains("stack") || name.contains("display") || name.contains("set")) {
                        try {
                            m.setAccessible(true);
                            m.invoke(entity, stack);
                            System.out.println("[CustomDisplay] invoked method " + m + " to set item");
                            return true;
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            System.err.println("[CustomDisplay] invoking " + m + " failed: " + e);
                            // continue searching
                        }
                    }
                }
            }
            cur = cur.getSuperclass();
        }

        // 2) If not found, try to set via SynchedEntityData accessor field:
        //    getEntityData().set(<static accessor field>, stack)
        try {
            Method getEntityData = null;
            // common names: getEntityData(), getSynchedEntityData()
            try { getEntityData = cls.getMethod("getEntityData"); } catch (NoSuchMethodException ignored) {}
            if (getEntityData == null) {
                try { getEntityData = cls.getMethod("getSynchedEntityData"); } catch (NoSuchMethodException ignored) {}
            }
            if (getEntityData != null) {
                Object synched = getEntityData.invoke(entity);
                if (synched != null) {
                    Class<?> synchedClass = synched.getClass();
                    Class<?> accessorClass = null;
                    try {
                        accessorClass = Class.forName("net.minecraft.network.syncher.EntityDataAccessor");
                    } catch (ClassNotFoundException ignored) {
                        try { accessorClass = Class.forName("net.minecraft.network.syncher.DataParameter"); } catch (ClassNotFoundException ignored2) {}
                    }

                    if (accessorClass != null) {
                        // walk fields (class + superclasses) for static EntityDataAccessor fields whose name suggests item/stack/display
                        cur = cls;
                        while (cur != null) {
                            for (Field f : cur.getDeclaredFields()) {
                                if (accessorClass.isAssignableFrom(f.getType())) {
                                    String fname = f.getName().toLowerCase();
                                    if (fname.contains("item") || fname.contains("stack") || fname.contains("display") || fname.contains("data")) {
                                        try {
                                            f.setAccessible(true);
                                            Object accessor = f.get(null); // should be static
                                            if (accessor != null) {
                                                // synched.set(accessor, stack)
                                                Method setMethod = null;
                                                for (Method m : synchedClass.getMethods()) {
                                                    if (m.getName().equals("set") && m.getParameterTypes().length == 2) {
                                                        setMethod = m;
                                                        break;
                                                    }
                                                }
                                                if (setMethod != null) {
                                                    setMethod.invoke(synched, accessor, stack);
                                                    System.out.println("[CustomDisplay] set item via SynchedEntityData field " + f.getName());
                                                    return true;
                                                }
                                            }
                                        } catch (IllegalAccessException | InvocationTargetException e) {
                                            System.err.println("[CustomDisplay] failed setting synched field " + f.getName() + " : " + e);
                                        }
                                    }
                                }
                            }
                            cur = cur.getSuperclass();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[CustomDisplay] synched-data fallback failure: " + e);
        }

        // nothing worked
        if (DEBUG) System.out.println("[CustomDisplay] trySetDisplayItem: no suitable setter/accessor found on " + entity.getClass().getName());
        return false;
    }

    private static void dumpClassSignature(Entity entity) {
        if (!DEBUG || entity == null) return;
        Class<?> cls = entity.getClass();
        System.out.println("[CustomDisplay-DUMP] Class: " + cls.getName());
        System.out.println("[CustomDisplay-DUMP] Declared methods:");
        for (Method m : cls.getDeclaredMethods()) System.out.println("  " + m);
        System.out.println("[CustomDisplay-DUMP] Declared fields:");
        for (Field f : cls.getDeclaredFields()) System.out.println("  " + f + " (type=" + f.getType().getName() + ")");
    }
}