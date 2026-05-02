package cn.breezeth.ordertocook.client.render.model;

import cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

public class MotorcycleModel extends EntityModel<MotorcycleEntity> {
    private final ModelPart body;
    private final ModelPart handle;
    private final ModelPart front_t;
    private final ModelPart rear_t;

    public MotorcycleModel(ModelPart root) {
        this.body = root.getChild("body");
        this.handle = root.getChild("handle");
        this.front_t = root.getChild("front_t");
        this.rear_t = root.getChild("rear_t");
    }

    public static net.minecraft.client.model.TexturedModelData getTexturedModelData() {
        ModelData meshdefinition = new ModelData();
        ModelPartData partdefinition = meshdefinition.getRoot();

        ModelPartData body = partdefinition.addChild("body", ModelPartBuilder.create().uv(45, 0).cuboid(-3.8333F, 2.9547F, -6.7935F, 8.0F, 5.0F, 8.0F, new Dilation(0.01F))
                .uv(43, 40).cuboid(-3.8333F, 2.9547F, 1.2065F, 8.0F, 5.0F, 12.0F, new Dilation(0.0F))
                .uv(0, 0).cuboid(-4.8333F, -6.0453F, 1.2065F, 10.0F, 9.0F, 12.0F, new Dilation(0.005F))
                .uv(43, 58).cuboid(-3.8333F, -6.0453F, 13.2065F, 8.0F, 6.0F, 6.0F, new Dilation(0.0F))
                .uv(45, 32).cuboid(-3.8333F, -12.0453F, -13.7935F, 8.0F, 2.0F, 5.0F, new Dilation(0.0F)), ModelTransform.pivot(-0.1667F, 11.0453F, 1.7935F));

        body.addChild("cube_r1", ModelPartBuilder.create().uv(66, 71).cuboid(2.0F, 14.0F, 0.0F, 1.0F, 8.0F, 3.0F, new Dilation(0.0F))
                .uv(57, 71).cuboid(-3.0F, 14.0F, 0.0F, 1.0F, 8.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.1667F, -14.0453F, -11.7935F, -0.384F, 0.0F, 0.0F));

        body.addChild("cube_r2", ModelPartBuilder.create().uv(9, 88).cuboid(-3.0F, -7.0F, -1.0F, 1.0F, 8.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.1667F, 5.9547F, 12.2065F, 0.7854F, 0.0F, 0.0F));

        body.addChild("cube_r3", ModelPartBuilder.create().uv(0, 88).cuboid(2.0F, -7.0F, -1.0F, 1.0F, 8.0F, 3.0F, new Dilation(0.0F)), ModelTransform.of(0.1667F, 5.7047F, 11.9565F, 0.7854F, 0.0F, 0.0F));

        body.addChild("cube_r4", ModelPartBuilder.create().uv(0, 40).cuboid(-4.0F, -3.0F, -5.0F, 8.0F, 5.0F, 13.0F, new Dilation(0.005F)), ModelTransform.of(0.1667F, 1.2047F, -10.7935F, -0.7854F, 0.0F, 0.0F));

        body.addChild("cube_r5", ModelPartBuilder.create().uv(45, 14).cuboid(-4.0F, -9.0F, -9.0F, 8.0F, 12.0F, 5.0F, new Dilation(0.01F)), ModelTransform.of(0.1667F, -0.0453F, -8.7935F, -0.384F, 0.0F, 0.0F));

        body.addChild("chest", ModelPartBuilder.create().uv(88, 7).cuboid(-2.0F, -1.0F, -1.0F, 2.0F, 1.0F, 3.0F, new Dilation(0.015F))
                .uv(78, 10).cuboid(0.5F, -7.0F, 1.25F, 3.0F, 3.0F, 1.0F, new Dilation(0.0F))
                .uv(0, 22).cuboid(-4.0F, -9.0F, -8.0F, 12.0F, 8.0F, 10.0F, new Dilation(0.0F))
                .uv(93, 62).cuboid(4.0F, -1.0F, -1.0F, 2.0F, 1.0F, 3.0F, new Dilation(0.015F))
                .uv(77, 7).cuboid(0.0F, -1.0F, -0.25F, 4.0F, 1.0F, 2.0F, new Dilation(0.015F)), ModelTransform.pivot(-1.8333F, -5.0453F, 20.2065F));

        ModelPartData handle = partdefinition.addChild("handle", ModelPartBuilder.create().uv(0, 59).cuboid(-2.5F, -2.5F, -3.5318F, 5.0F, 5.0F, 7.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, -3.5F, -9.4682F));

        handle.addChild("cube_r6", ModelPartBuilder.create().uv(72, 19).cuboid(-11.0F, -1.0F, -2.0F, 10.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, -0.5318F, 0.0F, 0.384F, 0.0F));

        handle.addChild("cube_r7", ModelPartBuilder.create().uv(72, 24).cuboid(-6.0F, -2.0F, -3.0F, 5.0F, 3.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(-0.5F, 0.5F, -0.5318F, 0.0F, 0.384F, 0.0F));

        handle.addChild("cube_r8", ModelPartBuilder.create().uv(0, 72).cuboid(1.0F, -2.0F, -3.0F, 5.0F, 3.0F, 4.0F, new Dilation(0.0F)), ModelTransform.of(0.5F, 0.5F, -0.5318F, 0.0F, -0.384F, 0.0F));

        handle.addChild("cube_r9", ModelPartBuilder.create().uv(72, 14).cuboid(1.0F, -1.0F, -2.0F, 10.0F, 2.0F, 2.0F, new Dilation(0.0F)), ModelTransform.of(0.0F, 0.0F, -0.5318F, 0.0F, -0.384F, 0.0F));

        ModelPartData front_t = partdefinition.addChild("front_t", ModelPartBuilder.create().uv(93, 66).cuboid(0.0F, -3.95F, -4.0371F, 0.0F, 8.0F, 8.0F, new Dilation(0.0F))
                .uv(38, 88).cuboid(-2.0F, -2.45F, -6.0371F, 4.0F, 5.0F, 2.0F, new Dilation(0.0F))
                .uv(72, 58).cuboid(-2.0F, 4.05F, -2.5371F, 4.0F, 2.0F, 5.0F, new Dilation(0.001F))
                .uv(88, 90).cuboid(-2.0F, -2.7F, 3.9629F, 4.0F, 5.0F, 2.0F, new Dilation(0.0F))
                .uv(75, 74).cuboid(-2.0F, -5.95F, -2.5371F, 4.0F, 2.0F, 5.0F, new Dilation(0.001F)), ModelTransform.pivot(0.0F, 17.95F, -16.9629F));

        front_t.addChild("cube_r10", ModelPartBuilder.create().uv(18, 91).cuboid(-2.0F, -2.6464F, -6.0F, 4.0F, 5.0F, 2.0F, new Dilation(0.002F))
                .uv(75, 66).cuboid(-2.0F, -6.1768F, -2.5303F, 4.0F, 2.0F, 5.0F, new Dilation(0.002F))
                .uv(75, 90).cuboid(-2.0F, -2.6464F, 4.0F, 4.0F, 5.0F, 2.0F, new Dilation(0.002F))
                .uv(72, 32).cuboid(-2.0F, 4.0F, -2.3535F, 4.0F, 2.0F, 5.0F, new Dilation(0.002F))
                .uv(19, 84).cuboid(-3.0F, -1.5F, -1.5F, 6.0F, 3.0F, 3.0F, new Dilation(0.002F)), ModelTransform.of(0.0F, 0.05F, -0.0371F, -0.7854F, 0.0F, 0.0F));

        ModelPartData rear_t = partdefinition.addChild("rear_t", ModelPartBuilder.create().uv(40, 71).cuboid(0.0F, -3.9043F, -3.9914F, 0.0F, 8.0F, 8.0F, new Dilation(0.0F))
                .uv(91, 24).cuboid(-2.0F, -2.6543F, -5.9914F, 4.0F, 5.0F, 2.0F, new Dilation(0.0F))
                .uv(78, 0).cuboid(-2.0F, 4.0957F, -2.4914F, 4.0F, 2.0F, 5.0F, new Dilation(0.001F))
                .uv(91, 55).cuboid(-2.0F, -2.6543F, 4.0086F, 4.0F, 5.0F, 2.0F, new Dilation(0.0F))
                .uv(75, 82).cuboid(-2.0F, -5.9043F, -2.4914F, 4.0F, 2.0F, 5.0F, new Dilation(0.001F)), ModelTransform.pivot(0.0F, 17.9043F, 15.9914F));

        rear_t.addChild("cube_r11", ModelPartBuilder.create().uv(51, 94).cuboid(-2.0F, -2.4697F, -6.1768F, 4.0F, 5.0F, 2.0F, new Dilation(0.002F))
                .uv(0, 80).cuboid(-2.0F, -6.1768F, -2.5303F, 4.0F, 2.0F, 5.0F, new Dilation(0.002F))
                .uv(91, 32).cuboid(-2.0F, -2.6464F, 4.0F, 4.0F, 5.0F, 2.0F, new Dilation(0.002F))
                .uv(19, 76).cuboid(-2.0F, 4.0F, -2.7071F, 4.0F, 2.0F, 5.0F, new Dilation(0.002F))
                .uv(84, 40).cuboid(-3.0F, -1.5F, -1.5F, 6.0F, 3.0F, 3.0F, new Dilation(0.002F)), ModelTransform.of(0.0F, 0.0957F, 0.0086F, -0.7854F, 0.0F, 0.0F));

        return net.minecraft.client.model.TexturedModelData.of(meshdefinition, 128, 128);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, float red, float green, float blue, float alpha) {
        body.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        handle.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        front_t.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
        rear_t.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
    }

    @Override
    public void setAngles(MotorcycleEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        float tickDelta = animationProgress - (float) entity.age;
        float steeringYaw = entity.getHandleAngle(tickDelta) * ((float) Math.PI / 180F);
        this.handle.yaw = steeringYaw;
        this.front_t.yaw = steeringYaw;

        float wheelRot = entity.getWheelAngle(tickDelta) * ((float) Math.PI / 180F);
        this.front_t.pitch = wheelRot;
        this.rear_t.pitch = wheelRot;
    }

    /**
     * 1.20.1 {@link ModelPart} uses float RGBA; unpack ARGB like vanilla tinted rendering.
     */
    public void renderWithColor(MatrixStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        float a = (float) (color >> 24 & 0xFF) / 255.0F;
        float r = (float) (color >> 16 & 0xFF) / 255.0F;
        float g = (float) (color >> 8 & 0xFF) / 255.0F;
        float b = (float) (color & 0xFF) / 255.0F;
        if (a == 0.0F) {
            a = 1.0F;
        }
        body.render(matrices, vertexConsumer, light, overlay, r, g, b, a);
        handle.render(matrices, vertexConsumer, light, overlay, r, g, b, a);
        front_t.render(matrices, vertexConsumer, light, overlay, r, g, b, a);
        rear_t.render(matrices, vertexConsumer, light, overlay, r, g, b, a);
    }

    public ModelPart getHandle() {
        return handle;
    }

    public ModelPart getFrontWheel() {
        return front_t;
    }

    public ModelPart getRearWheel() {
        return rear_t;
    }
}
