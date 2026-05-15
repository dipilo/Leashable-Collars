package com.dipilodopilasaurus.leashablecollars.neoforge.client;

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

public class FootPawRenderer implements ICurioRenderer {
    private static void renderForLeg(ItemStack stack, PoseStack poseStack, ModelPart leg, MultiBufferSource bufferSource, int light) {
        poseStack.pushPose();
        AccessoryTransformHelper.transformToBottomFace(poseStack, leg);
        poseStack.mulPose(new Quaternionf().rotateXYZ((float) -Math.PI / 2.0F, 0.0F, 0.0F));
        poseStack.translate(0.0F, 0.0F, 0.125F);
        poseStack.scale(0.75F, 0.75F, 0.75F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, null, 0);
        poseStack.popPose();
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource bufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        ItemStack renderStack = stack.copy();
        renderForLeg(renderStack, poseStack, humanoidModel.rightLeg, bufferSource, light);
        renderForLeg(renderStack, poseStack, humanoidModel.leftLeg, bufferSource, light);
    }
}
