
package net.mcreator.foodexpirydate.block.entity;

import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.Capability;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.mcreator.foodexpirydate.world.inventory.FoodDryingRackGUIMenu;
import net.mcreator.foodexpirydate.init.FoodExpiryDateModBlockEntities;
import net.mcreator.foodexpirydate.network.ClientSpawnHelper;
import net.mcreator.foodexpirydate.DisplayRegistry;
import net.mcreator.foodexpirydate.ThingsThatCanExpire;
import net.mcreator.foodexpirydate.CustomDisplayUtils;

import javax.annotation.Nullable;

import java.util.stream.IntStream;

import io.netty.buffer.Unpooled;

public class FoodDryingRackBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	private NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(9, ItemStack.EMPTY);
	private final LazyOptional<? extends IItemHandler>[] handlers = SidedInvWrapper.create(this, Direction.values());

	public final Entity[] displayedItems;

	public void removeAlldisplayedItems() {
		if(displayedItems.length <= 0) return;
		
		for (Entity display : displayedItems) {
	        if (display != null && !display.isRemoved()) {
	           display.remove(Entity.RemovalReason.DISCARDED);
	        }
	    }
	}

	public void displayAllItems() {
	    if (level == null) return;
	
	    BlockPos pos = this.getBlockPos();
	    BlockState state = getBlockState();
	
	    // Default: south-facing offset
	    double baseX = pos.getX() + 0.5;
	    double baseY = pos.getY() + 1.0;
	    double baseZ = pos.getZ() + 0.5;
	
	    // Read horizontal facing property
	    Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
	            ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
	            : Direction.SOUTH;
	
	    for (int i = 0; i < this.stacks.size(); i++) {
	        ItemStack stack = this.stacks.get(i);
	
	        // cleanup old
	        if (displayedItems[i] != null && !displayedItems[i].isRemoved()) {
	            displayedItems[i].discard();
	        }
	
	        if (!stack.isEmpty() && level instanceof ServerLevel server) {
	            // local offset along rack
	            double offset = (i * 0.085) - 0.34; // spread items across
	
	            double dx = 0, dz = 0;
	            float rotY = 0;
	
	            switch (facing) {
	                case NORTH -> { dx = -offset; dz = 0; rotY = -90; }
	                case SOUTH -> { dx = offset;  dz = 0; rotY = 90; }
	                case WEST  -> { dx = 0;    dz = offset; rotY = 180; }
	                case EAST  -> { dx = 0;    dz = -offset; rotY = 0; }
	            }
	
	            Entity e = CustomDisplayUtils.spawnItemDisplay(
	                server,
	                baseX + dx,
	                baseY,
	                baseZ + dz,
	                stack.getItem(),
	                0, rotY, 0, // apply rotation
	                0.5f, 
	                true
	            );
	
	            displayedItems[i] = e;
	        } else {
	            displayedItems[i] = null;
	        }
	    }
	}

	public FoodDryingRackBlockEntity(BlockPos position, BlockState state) {
		super(FoodExpiryDateModBlockEntities.FOOD_DRYING_RACK.get(), position, state);
		this.displayedItems = new Entity[stacks.size()];
	}

	// ========= Persist UUIDs =========
    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        
        ContainerHelper.saveAllItems(tag, this.stacks);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        
        this.stacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
    	ContainerHelper.loadAllItems(tag, this.stacks);
    }

    @Override
	public void onLoad() {
	    super.onLoad();
	    
	    // register this instance so it can be cleaned up on server stop
	    if (!level.isClientSide) {
	        DisplayRegistry.register(this);

	        removeAlldisplayedItems();
	    
	    	// spawn displays when server-side chunk loads
	        displayAllItems();
	    }
	}

	@Override
	public void setChanged() {
	    super.setChanged();
	    if (!level.isClientSide) {
	        displayAllItems();
	    }
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}
	
    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithFullMetadata();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        this.load(tag);
    }

    public void syncToClient() {
        if (level instanceof ServerLevel server) {
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

	@Override
	public int getContainerSize() {
		return stacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.stacks)
			if (!itemstack.isEmpty())
				return false;
		return true;
	}

	@Override
	public Component getDefaultName() {
		return Component.literal("food_drying_rack");
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new FoodDryingRackGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Food Drying Rack");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return stack.isEdible();
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return IntStream.range(0, this.getContainerSize()).toArray();
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack itemstack, @Nullable Direction direction) {
		return this.canPlaceItem(index, itemstack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack itemstack, Direction direction) {
		return true;
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
		if (!this.remove && facing != null && capability == ForgeCapabilities.ITEM_HANDLER)
			return handlers[facing.ordinal()].cast();
		return super.getCapability(capability, facing);
	}

	@Override
	public void setRemoved() {
	    // perform existing cleanup and unregister
	    try {
	        removeAlldisplayedItems();
	    } catch (Throwable t) {
	        System.err.println("[CustomDisplay] error removing displays in setRemoved: " + t);
	    }
	    DisplayRegistry.unregister(this);
	    super.setRemoved();
	    for (LazyOptional<? extends IItemHandler> handler : handlers) {
	        handler.invalidate();
	    }
	}
	
	@Override
	public void onChunkUnloaded() {
	    super.onChunkUnloaded();
	    try {
	        removeAlldisplayedItems();
	    } catch (Throwable t) {
	        System.err.println("[CustomDisplay] error removing displays in onChunkUnloaded: " + t);
	    }
	    DisplayRegistry.unregister(this);
	}
}