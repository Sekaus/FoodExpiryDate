package net.mcreator.foodexpirydate;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.ai.behavior.UpdateActivityFromSchedule;

public class ExpiryData extends SavedData {

    private final Map<BlockPos, CompoundTag> expiryMap = new HashMap<>();

    public ExpiryData() {
        super();
    }

    public static ExpiryData create(CompoundTag tag) {
        ExpiryData data = new ExpiryData();
        data.loadData(tag);
        return data;
    }

    public static ExpiryData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ExpiryData::create, ExpiryData::new, "expiry_data");
    }

    public CompoundTag getExpiryData(BlockPos pos) {
        return this.expiryMap.getOrDefault(pos, new CompoundTag());
    }

    public CompoundTag getExpiryDataOrNull(BlockPos pos) {
    	return this.expiryMap.get(pos); // returns null if absent
	}
	
	public boolean hasExpiryData(BlockPos pos) {
	    return this.expiryMap.containsKey(pos);
	}

    public void setExpiryData(BlockPos pos, CompoundTag data) {
	    this.expiryMap.put(pos.immutable(), data);
	    setDirty();
	}
	
	public void removeExpiryData(BlockPos pos) {
	    this.expiryMap.remove(pos.immutable());
	    setDirty();
	}

	public void updateExpiryData(BlockPos pos, CompoundTag data) {
		if(hasExpiryData(pos.immutable()))
			this.expiryMap.remove(pos.immutable());
		setExpiryData(pos.immutable(), data);
	}

    public void loadData(CompoundTag nbt) {
        expiryMap.clear();
        ListTag listTag = nbt.getList("expiryDates", 10);
        for (int i = 0; i < listTag.size(); ++i) {
            CompoundTag entryTag = listTag.getCompound(i);
            BlockPos pos = NbtUtils.readBlockPos(entryTag.getCompound("pos"));
            CompoundTag data = entryTag.getCompound("data");
            expiryMap.put(pos, data);
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag listTag = new ListTag();
        for (Map.Entry<BlockPos, CompoundTag> entry : expiryMap.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.put("pos", NbtUtils.writeBlockPos(entry.getKey()));
            entryTag.put("data", entry.getValue());
            listTag.add(entryTag);
        }
        nbt.put("expiryDates", listTag);
        return nbt;
    }
}