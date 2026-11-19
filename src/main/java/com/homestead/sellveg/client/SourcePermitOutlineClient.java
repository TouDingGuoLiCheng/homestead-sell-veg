package com.homestead.sellveg.client;

import com.homestead.sellveg.NetShopMod;
import com.homestead.sellveg.item.SourcePermitItem;
import com.homestead.sellveg.stock.SourcePermitData;
import com.homestead.sellveg.stock.SourcePermitMark;
import com.homestead.sellveg.stock.StockIndex;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = NetShopMod.MOD_ID, value = Dist.CLIENT)
public final class SourcePermitOutlineClient {
    private static final float R = 0.85F;
    private static final float G = 0.55F;
    private static final float B = 0.2F;
    private static final float A = 0.9F;

    private SourcePermitOutlineClient() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        ItemStack main = player.getMainHandItem();
        if (!(main.getItem() instanceof SourcePermitItem)) {
            return;
        }
        List<SourcePermitMark> marks = SourcePermitData.read(main);
        if (marks.isEmpty()) {
            return;
        }

        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffer.getBuffer(RenderType.lines());
        Vec3 cam = event.getCamera().getPosition();

        pose.pushPose();
        pose.translate(-cam.x, -cam.y, -cam.z);
        for (SourcePermitMark mark : marks) {
            if (!SourcePermitData.matchesDimension(mark, mc.level)) {
                continue;
            }
            BlockPos pos = SourcePermitData.toPos(mark);
            if (!mc.level.isLoaded(pos)) {
                continue;
            }
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be == null || !StockIndex.isMarkable(be)) {
                continue;
            }
            AABB box = new AABB(pos).inflate(0.002D);
            LevelRenderer.renderLineBox(pose, lines, box, R, G, B, A);
        }
        pose.popPose();
        buffer.endBatch(RenderType.lines());
    }
}
