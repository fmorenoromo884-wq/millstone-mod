package com.fer.encasedfan.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.fer.encasedfan.EncasedFan;
import com.fer.encasedfan.block.FanBlock;
import com.fer.encasedfan.block.FanBlockEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Aspas PROVISIONALES: una cruz de 8 brazos que gira dentro del marco.
 * Cuando tengas tu modelo de Blockbench solo hay que cambiar createLayer() / las texturas;
 * la logica de giro (angulo, velocidad) vive en FanBlockEntity y no cambia.
 */
public class FanRenderer implements BlockEntityRenderer<FanBlockEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(EncasedFan.id("fan_blades"), "main");
    private static final ResourceLocation TEX = EncasedFan.id("textures/entity/fan_blades.png");
    private static final ResourceLocation TEX_GOLD = EncasedFan.id("textures/entity/golden_fan_blades.png");

    private final ModelPart root;

    public FanRenderer(BlockEntityRendererProvider.Context ctx) {
        this.root = ctx.bakeLayer(LAYER);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // brazos en cruz (+)
        root.addOrReplaceChild("cross", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.5F, -1.0F, -0.5F, 11.0F, 2.0F, 1.0F)
                .texOffs(0, 4).addBox(-1.0F, -5.5F, -0.5F, 2.0F, 11.0F, 1.0F)
                .texOffs(8, 4).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F),
                PartPose.ZERO);
        // brazos en diagonal (x), un poco mas finos para evitar z-fighting
        root.addOrReplaceChild("diag", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.5F, -1.0F, -0.4F, 11.0F, 2.0F, 0.8F)
                .texOffs(0, 4).addBox(-1.0F, -5.5F, -0.4F, 2.0F, 11.0F, 0.8F),
                PartPose.rotation(0F, 0F, (float) (Math.PI / 4.0)));
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void render(FanBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof FanBlock fan) || be.getLevel() == null) return;
        Direction dir = state.getValue(FanBlock.FACING);
        float angle = Mth.lerp(partialTick, be.prevAngle, be.angle);
        int light = LevelRenderer.getLightColor(be.getLevel(), be.getBlockPos().relative(dir));

        ps.pushPose();
        // centro del bloque, desplazado 4px hacia el frente (el marco tiene 8px de fondo)
        ps.translate(0.5 + dir.getStepX() * 0.25, 0.5 + dir.getStepY() * 0.25, 0.5 + dir.getStepZ() * 0.25);
        // el modelo es plano en XY; alinear su normal (Z) con el eje del ventilador
        switch (dir.getAxis()) {
            case X -> ps.mulPose(Axis.YP.rotationDegrees(90));
            case Y -> ps.mulPose(Axis.XP.rotationDegrees(90));
            default -> { }
        }
        ps.mulPose(Axis.ZP.rotationDegrees(angle));

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(fan.isGolden() ? TEX_GOLD : TEX));
        root.render(ps, vc, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }
}
