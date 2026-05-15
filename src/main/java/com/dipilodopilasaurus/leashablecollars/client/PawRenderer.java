package com.dipilodopilasaurus.leashablecollars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class PawRenderer implements ICurioRenderer {
    private static void renderForArm(ItemStack stack, PoseStack poseStack, ModelPart arm, boolean left, MultiBufferSource bufferSource, int light) {
        poseStack.pushPose();
        AccessoryTransformHelper.transformToBottomFace(poseStack, arm);
        boolean thinArms = AccessoryTransformHelper.hasSlimArm(arm);
        poseStack.mulPose(new Quaternionf().rotateXYZ((float) Math.PI, (float) (left ? Math.PI : -Math.PI) / 2.0F, 0.0F));
        poseStack.translate(0.0F, -0.1875F, -0.125F);
        poseStack.scale(0.75F, 0.625F, thinArms ? 0.875F : 1.03125F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, null, 0);
        poseStack.popPose();
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource bufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        ItemStack renderStack = stack.copy();
        renderForArm(renderStack, poseStack, humanoidModel.rightArm, false, bufferSource, light);
        renderForArm(renderStack, poseStack, humanoidModel.leftArm, true, bufferSource, light);
    }
}