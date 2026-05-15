package com.dipilodopilasaurus.leashablecollars.neoforge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class CollarRenderer implements ICurioRenderer {

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource bufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(renderLayerParent.getModel() instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        poseStack.pushPose();
        ModelPart body = humanoidModel.body;
        boolean hasChestplate = !slotContext.entity().getItemBySlot(EquipmentSlot.CHEST).isEmpty();
        poseStack.translate(body.x * 0.0625F, body.y * 0.0625F, body.z * 0.0625F);
        poseStack.mulPose(new Quaternionf().rotateXYZ(body.xRot, body.yRot, body.zRot + (float) Math.PI));
        poseStack.scale((hasChestplate ? 0.7F : 0.85F) * body.xScale, 0.85F * body.yScale, (hasChestplate ? 1.1F : 0.85F) * body.zScale);
        poseStack.translate(0.0F, hasChestplate ? 0.475F : 0.4125F, -0.005F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.HEAD, light, OverlayTexture.NO_OVERLAY, poseStack, bufferSource, slotContext.entity().level(), 0);
        poseStack.popPose();
    }
}