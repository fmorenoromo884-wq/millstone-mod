package com.fer.millstone;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

/** Dibuja la rueda de molino (models/block/millstone_wheel.json) girando sobre el eje vertical. */
public class MillstoneRenderer implements BlockEntityRenderer<MillstoneBlockEntity> {
    public static final ModelResourceLocation WHEEL_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(MillstoneMod.MODID, "block/millstone_wheel"));

    public MillstoneRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MillstoneBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getModelManager().getModel(WHEEL_MODEL);

        boolean lit = be.getBlockState().getValue(MillstoneBlock.LIT);
        float angle = be.wheelAngle + (lit ? partialTick * MillstoneBlockEntity.WHEEL_SPEED : 0f);

        pose.pushPose();
        // La rueda gira sobre el centro del bloque (8, y, 8) en el eje Y
        pose.translate(0.5, 0.0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-0.5, 0.0, -0.5);
        mc.getBlockRenderer().getModelRenderer().renderModel(
                pose.last(), buffer.getBuffer(RenderType.cutout()), null, model,
                1f, 1f, 1f, packedLight, packedOverlay);
        pose.popPose();
    }
}
