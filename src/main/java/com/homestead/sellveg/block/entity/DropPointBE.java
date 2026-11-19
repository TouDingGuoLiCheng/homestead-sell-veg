package com.homestead.sellveg.block.entity;

import com.homestead.sellveg.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DropPointBE extends BlockEntity implements Nameable {
    private String dropPointName = "Harvest Point";
    @Nullable
    private UUID ownerId;
    @Nullable
    private UUID pointId;

    public DropPointBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DROP_POINT.get(), pos, state);
    }

    public UUID getOrCreatePointId() {
        if (pointId == null) {
            pointId = UUID.randomUUID();
            setChanged();
        }
        return pointId;
    }

    @Nullable
    public UUID getPointId() {
        return pointId;
    }

    public void setPointId(UUID pointId) {
        this.pointId = pointId;
        setChanged();
    }

    public String getDropPointName() {
        return dropPointName;
    }

    public void setDropPointName(String name) {
        this.dropPointName = name == null || name.isBlank() ? "Harvest Point" : name.trim();
        setChanged();
    }

    @Nullable
    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(@Nullable UUID ownerId) {
        this.ownerId = ownerId;
        setChanged();
    }

    @Override
    public Component getName() {
        return Component.literal(dropPointName);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("DropPointName", dropPointName);
        if (ownerId != null) {
            tag.putUUID("Owner", ownerId);
        }
        if (pointId != null) {
            tag.putUUID("PointId", pointId);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("DropPointName")) {
            dropPointName = tag.getString("DropPointName");
        }
        if (tag.hasUUID("Owner")) {
            ownerId = tag.getUUID("Owner");
        }
        if (tag.hasUUID("PointId")) {
            pointId = tag.getUUID("PointId");
        }
    }
}
