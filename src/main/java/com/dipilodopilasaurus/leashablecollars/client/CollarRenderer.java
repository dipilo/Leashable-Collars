package com.dipilodopilasaurus.leashablecollars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import org.joml.Quaternionf;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class CollarRenderer implements ICurioRenderer {
    private final BakedModel model;
    private final Ingredient chestplateIngredient = Ingredient.of(Tags.Items.ARMORS_CHESTPLATES);

    public CollarRenderer(BakedModel model) {
        this.model = model;
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slotContext, PoseStack poseStack, RenderLayerParent<T, M> renderLayerParent, MultiBufferSource bufferSource, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        poseStack.pushPose();
        try {
            ModelPart body = ((HumanoidModel<T>) renderLayerParent.getModel()).body;
            boolean hasChestplate = false;
            for (ItemStack armorStack : slotContext.entity().getArmorSlots()) {
                if (chestplateIngredient.test(armorStack)) {
                    hasChestplate = true;
                    break;
                }
            }
            poseStack.translate(body.x * 0.0625f, body.y * 0.0625f, body.z * 0.0625f);
            poseStack.mulPose(new Quaternionf().rotateXYZ(body.xRot, body.yRot, body.zRot + (float) Math.PI));
            poseStack.scale((hasChestplate ? 0.7f : 0.85f) * body.xScale, 0.85f * body.yScale, (hasChestplate ? 1.1f : 0.85f) * body.zScale);
            poseStack.translate(0, hasChestplate ? 0.475 : 0.4125, -0.005);
            Minecraft.getInstance().getItemRenderer().render(stack, ItemDisplayContext.HEAD, false, poseStack, bufferSource, light, OverlayTexture.NO_OVERLAY, model);
        } catch (ClassCastException ignored) {
            // Non-humanoid render layers do not expose a compatible body part for the collar transform.
        }
        poseStack.popPose();
    }
}
