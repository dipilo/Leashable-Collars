package com.dipilodopilasaurus.leashablecollars.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;

final class AccessoryTransformHelper {
    private static final Field CUBES_FIELD;
    private static final Field CUBE_MIN_X_FIELD;
    private static final Field CUBE_MAX_X_FIELD;
    private static final Field CUBE_MIN_Y_FIELD;
    private static final Field CUBE_MAX_Y_FIELD;
    private static final Field CUBE_MIN_Z_FIELD;
    private static final Field CUBE_MAX_Z_FIELD;

    static {
        try {
            CUBES_FIELD = findField(ModelPart.class, "cubes", "f_104212_");

            Class<?> cubeClass = Class.forName("net.minecraft.client.model.geom.ModelPart$Cube");
            CUBE_MIN_X_FIELD = findField(cubeClass, "minX", "f_104335_");
            CUBE_MIN_Y_FIELD = findField(cubeClass, "minY", "f_104336_");
            CUBE_MIN_Z_FIELD = findField(cubeClass, "minZ", "f_104337_");
            CUBE_MAX_X_FIELD = findField(cubeClass, "maxX", "f_104338_");
            CUBE_MAX_Y_FIELD = findField(cubeClass, "maxY", "f_104339_");
            CUBE_MAX_Z_FIELD = findField(cubeClass, "maxZ", "f_104340_");
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Field findField(Class<?> owner, String... names) throws NoSuchFieldException {
        Exception failure = null;
        for (String name : names) {
            try {
                return ObfuscationReflectionHelper.findField(owner, name);
            } catch (ObfuscationReflectionHelper.UnableToFindFieldException exception) {
                failure = exception;
            }
        }
        NoSuchFieldException exception = new NoSuchFieldException(owner.getName() + " " + String.join(", ", names));
        if (failure != null) {
            exception.initCause(failure);
        }
        throw exception;
    }

    private AccessoryTransformHelper() {
    }

    static void transformToBottomFace(PoseStack poseStack, ModelPart part) {
        part.translateAndRotate(poseStack);

        Bounds bounds = getBounds(part);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        poseStack.translate((bounds.minX + bounds.maxX) / 2.0D, bounds.maxY, (bounds.minZ + bounds.maxZ) / 2.0D);
        poseStack.scale(8.0F, 8.0F, 8.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
    }

    static boolean hasSlimArm(ModelPart arm) {
        Bounds bounds = getBounds(arm);
        return (bounds.maxX - bounds.minX) < 4.0F;
    }

    private static Bounds getBounds(ModelPart part) {
        try {
            @SuppressWarnings("unchecked")
            List<Object> cubes = (List<Object>) CUBES_FIELD.get(part);
            float minX = 0.0F;
            float minY = 0.0F;
            float minZ = 0.0F;
            float maxX = 0.0F;
            float maxY = 0.0F;
            float maxZ = 0.0F;

            for (Object cube : cubes) {
                float cubeMinX = Math.min(CUBE_MIN_X_FIELD.getFloat(cube), CUBE_MAX_X_FIELD.getFloat(cube));
                float cubeMaxX = Math.max(CUBE_MIN_X_FIELD.getFloat(cube), CUBE_MAX_X_FIELD.getFloat(cube));
                float cubeMinY = Math.min(CUBE_MIN_Y_FIELD.getFloat(cube), CUBE_MAX_Y_FIELD.getFloat(cube));
                float cubeMaxY = Math.max(CUBE_MIN_Y_FIELD.getFloat(cube), CUBE_MAX_Y_FIELD.getFloat(cube));
                float cubeMinZ = Math.min(CUBE_MIN_Z_FIELD.getFloat(cube), CUBE_MAX_Z_FIELD.getFloat(cube));
                float cubeMaxZ = Math.max(CUBE_MIN_Z_FIELD.getFloat(cube), CUBE_MAX_Z_FIELD.getFloat(cube));

                minX = Math.min(minX, cubeMinX);
                minY = Math.min(minY, cubeMinY);
                minZ = Math.min(minZ, cubeMinZ);
                maxX = Math.max(maxX, cubeMaxX);
                maxY = Math.max(maxY, cubeMaxY);
                maxZ = Math.max(maxZ, cubeMaxZ);
            }
            return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
        } catch (IllegalAccessException exception) {
            throw new ModelPartBoundsException("Failed to read model part bounds", exception);
        }
    }

    private record Bounds(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
    }

    private static final class ModelPartBoundsException extends IllegalStateException {
        private ModelPartBoundsException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}