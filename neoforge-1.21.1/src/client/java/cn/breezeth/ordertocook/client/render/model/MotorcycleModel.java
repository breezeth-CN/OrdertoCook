package cn.breezeth.ordertocook.client.render.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.Entity;

public class MotorcycleModel<T extends Entity> extends EntityModel<T> {
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

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(45, 0).addBox(-3.8333F, 2.9547F, -6.7935F, 8.0F, 5.0F, 8.0F, new CubeDeformation(0.01F))
                .texOffs(43, 40).addBox(-3.8333F, 2.9547F, 1.2065F, 8.0F, 5.0F, 12.0F, new CubeDeformation(0.0F))
                .texOffs(0, 0).addBox(-4.8333F, -6.0453F, 1.2065F, 10.0F, 9.0F, 12.0F, new CubeDeformation(0.005F))
                .texOffs(43, 58).addBox(-3.8333F, -6.0453F, 13.2065F, 8.0F, 6.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(45, 32).addBox(-3.8333F, -12.0453F, -13.7935F, 8.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)), PartPose.offset(-0.1667F, 11.0453F, 1.7935F));

        body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(66, 71).addBox(2.0F, 14.0F, 0.0F, 1.0F, 8.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(57, 71).addBox(-3.0F, 14.0F, 0.0F, 1.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1667F, -14.0453F, -11.7935F, -0.384F, 0.0F, 0.0F));

        body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(9, 88).addBox(-3.0F, -7.0F, -1.0F, 1.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1667F, 5.9547F, 12.2065F, 0.7854F, 0.0F, 0.0F));

        body.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(0, 88).addBox(2.0F, -7.0F, -1.0F, 1.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.1667F, 5.7047F, 11.9565F, 0.7854F, 0.0F, 0.0F));

        body.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(0, 40).addBox(-4.0F, -3.0F, -5.0F, 8.0F, 5.0F, 13.0F, new CubeDeformation(0.005F)), PartPose.offsetAndRotation(0.1667F, 1.2047F, -10.7935F, -0.7854F, 0.0F, 0.0F));

        body.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(45, 14).addBox(-4.0F, -9.0F, -9.0F, 8.0F, 12.0F, 5.0F, new CubeDeformation(0.01F)), PartPose.offsetAndRotation(0.1667F, -0.0453F, -8.7935F, -0.384F, 0.0F, 0.0F));

        body.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(88, 7).addBox(-2.0F, -1.0F, -1.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.015F))
                .texOffs(78, 10).addBox(0.5F, -7.0F, 1.25F, 3.0F, 3.0F, 1.0F, new CubeDeformation(0.0F))
                .texOffs(0, 22).addBox(-4.0F, -9.0F, -8.0F, 12.0F, 8.0F, 10.0F, new CubeDeformation(0.0F))
                .texOffs(93, 62).addBox(4.0F, -1.0F, -1.0F, 2.0F, 1.0F, 3.0F, new CubeDeformation(0.015F))
                .texOffs(77, 7).addBox(0.0F, -1.0F, -0.25F, 4.0F, 1.0F, 2.0F, new CubeDeformation(0.015F)), PartPose.offset(-1.8333F, -5.0453F, 20.2065F));

        PartDefinition handle = partdefinition.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(0, 59).addBox(-2.5F, -2.5F, -3.5318F, 5.0F, 5.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -3.5F, -9.4682F));

        handle.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(72, 19).addBox(-11.0F, -1.0F, -2.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -0.5318F, 0.0F, 0.384F, 0.0F));

        handle.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(72, 24).addBox(-6.0F, -2.0F, -3.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, 0.5F, -0.5318F, 0.0F, 0.384F, 0.0F));

        handle.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(0, 72).addBox(1.0F, -2.0F, -3.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.5F, 0.5F, -0.5318F, 0.0F, -0.384F, 0.0F));

        handle.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(72, 14).addBox(1.0F, -1.0F, -2.0F, 10.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, -0.5318F, 0.0F, -0.384F, 0.0F));

        PartDefinition front_t = partdefinition.addOrReplaceChild("front_t", CubeListBuilder.create().texOffs(93, 66).addBox(0.0F, -3.95F, -4.0371F, 0.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(38, 88).addBox(-2.0F, -2.45F, -6.0371F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(72, 58).addBox(-2.0F, 4.05F, -2.5371F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.001F))
                .texOffs(88, 90).addBox(-2.0F, -2.7F, 3.9629F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(75, 74).addBox(-2.0F, -5.95F, -2.5371F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.001F)), PartPose.offset(0.0F, 17.95F, -16.9629F));

        front_t.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(18, 91).addBox(-2.0F, -2.6464F, -6.0F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.002F))
                .texOffs(75, 66).addBox(-2.0F, -6.1768F, -2.5303F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.002F))
                .texOffs(75, 90).addBox(-2.0F, -2.6464F, 4.0F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.002F))
                .texOffs(72, 32).addBox(-2.0F, 4.0F, -2.3535F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.002F))
                .texOffs(19, 84).addBox(-3.0F, -1.5F, -1.5F, 6.0F, 3.0F, 3.0F, new CubeDeformation(0.002F)), PartPose.offsetAndRotation(0.0F, 0.05F, -0.0371F, -0.7854F, 0.0F, 0.0F));

        PartDefinition rear_t = partdefinition.addOrReplaceChild("rear_t", CubeListBuilder.create().texOffs(40, 71).addBox(0.0F, -3.9043F, -3.9914F, 0.0F, 8.0F, 8.0F, new CubeDeformation(0.0F))
                .texOffs(91, 24).addBox(-2.0F, -2.6543F, -5.9914F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(78, 0).addBox(-2.0F, 4.0957F, -2.4914F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.001F))
                .texOffs(91, 55).addBox(-2.0F, -2.6543F, 4.0086F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(75, 82).addBox(-2.0F, -5.9043F, -2.4914F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.001F)), PartPose.offset(0.0F, 17.9043F, 15.9914F));

        rear_t.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(51, 94).addBox(-2.0F, -2.4697F, -6.1768F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.002F))
                .texOffs(0, 80).addBox(-2.0F, -6.1768F, -2.5303F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.002F))
                .texOffs(91, 32).addBox(-2.0F, -2.6464F, 4.0F, 4.0F, 5.0F, 2.0F, new CubeDeformation(0.002F))
                .texOffs(19, 76).addBox(-2.0F, 4.0F, -2.7071F, 4.0F, 2.0F, 5.0F, new CubeDeformation(0.002F))
                .texOffs(84, 40).addBox(-3.0F, -1.5F, -1.5F, 6.0F, 3.0F, 3.0F, new CubeDeformation(0.002F)), PartPose.offsetAndRotation(0.0F, 0.0957F, 0.0086F, -0.7854F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        if (entity instanceof cn.breezeth.ordertocook.vehicle.motorcycle.MotorcycleEntity moto) {
            float tickDelta = animationProgress - (float)moto.tickCount;
            float steeringYaw = moto.getHandleAngle(tickDelta) * ((float)Math.PI / 180F);
            
            // 闂傚倷绀侀幉锟犳嚌妤ｅ啫瀚夋い鎺戝閺佸棝鏌ｉ幇顒傛憼濠殿喗绮撻弻銊╁籍閸ヮ煈妫勯柟顖滃枛濮婃椽宕ㄦ繝鍕厐闂佸吋妞块崹閬嶅疾閸洘鍋愰柧蹇ｅ亜椤€愁渻閵堝棙鈷掗柛瀣崌閹箖鏌嗗鍛獓闂佺粯鍨靛Λ妤佹櫠椤曗偓閺岀喖顢欓悾宀€鐓夐梺杞扮缁夌懓鐣锋搴ｇ煔濠?闂備礁鎼ˇ閬嶅磻閻樿绠垫い蹇撴椤?
            this.handle.yRot = steeringYaw;
            this.front_t.yRot = steeringYaw;

            // 婵犵數濮伴崹鐓庘枖濞戞埃鍋撳鐓庢珝妤犵偛鍟换婵嬪礃閳轰緡娼梻浣瑰濞叉垿鎳楅崼鏇炲嚑闁稿本绋撶弧鈧繝鐢靛Т閸熻櫕绔熷Ο琛℃斀妞ゆ棁顫夊▍濠囨煛娴ｅ摜效鐎规洜鍏樼瘬闁汇儳顬坔 闂備礁鎼ˇ閬嶅磻閻樿绠垫い蹇撴椤?
            float wheelRot = moto.getWheelAngle(tickDelta) * ((float)Math.PI / 180F);
            this.front_t.xRot = wheelRot;
            this.rear_t.xRot = wheelRot;
        }
    }

    @Override
    public void renderToBuffer(PoseStack matrices, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        body.render(matrices, vertexConsumer, light, overlay, color);
        handle.render(matrices, vertexConsumer, light, overlay, color);
        front_t.render(matrices, vertexConsumer, light, overlay, color);
        rear_t.render(matrices, vertexConsumer, light, overlay, color);
    }

    public ModelPart getHandle() { return handle; }
    public ModelPart getFrontWheel() { return front_t; }
    public ModelPart getRearWheel() { return rear_t; }
}


