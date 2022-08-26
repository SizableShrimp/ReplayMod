package com.replaymod.render.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Quaternion;
//#if MC>=10904
import com.replaymod.core.versions.MCVer;
import com.replaymod.render.blend.BlendState;
import com.replaymod.render.hooks.EntityRendererHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleManager {
    //#if MC>=11500
    @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;buildGeometry(Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/Camera;F)V"))
    private void buildOrientedGeometry(Particle particle, VertexConsumer vertexConsumer, Camera camera, float partialTicks) {
        EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
        if (handler == null || !handler.omnidirectional) {
            buildGeometry(particle, vertexConsumer, camera, partialTicks);
        } else {
            Quaternion rotation = camera.rotation();
            Quaternion org = rotation.copy();
            try {
                Vec3 from = new Vec3(0, 0, 1);
                Vec3 to = MCVer.getPosition(particle, partialTicks).subtract(camera.getPosition()).normalize();
                Vec3 axis = from.cross(to);
                rotation.set((float) axis.x, (float) axis.y, (float) axis.z, (float) (1 + from.dot(to)));
                rotation.normalize();

                buildGeometry(particle, vertexConsumer, camera, partialTicks);
            } finally {
                rotation.set(org.r(), org.i(), org.j(), org.k());
            }
        }
    }

    private void buildGeometry(Particle particle, VertexConsumer vertexConsumer, Camera camera, float partialTicks) {
        //#if MC<11900
        //$$ BlendState blendState = BlendState.getState();
        //$$ if (blendState != null) {
        //$$     blendState.get(ParticlesExporter.class).onRender(particle, partialTicks);
        //$$ }
        //#endif
        particle.render(vertexConsumer, camera, partialTicks);
    }
    //#else
    //#if MC>=11200
    //#if MC>=11400
    //$$ @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;buildGeometry(Lnet/minecraft/client/render/BufferBuilder;Lnet/minecraft/client/render/Camera;FFFFFF)V"))
    //$$ private void renderNormalParticle(Particle particle, BufferBuilder vertexBuffer, Camera view, float partialTicks,
    //#else
    //$$ @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //$$ private void renderNormalParticle(Particle particle, BufferBuilder vertexBuffer, Entity view, float partialTicks,
    //#endif
    //#else
    //$$ @Redirect(method = "renderParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/VertexBuffer;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //$$ private void renderNormalParticle(Particle particle, VertexBuffer vertexBuffer, Entity view, float partialTicks,
    //#endif
    //$$                                   float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
    //$$     renderParticle(particle, vertexBuffer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    //$$ }
    //$$
    //$$ // Seems to be gone by 1.14
    //#if MC<11400
    //#if MC>=11200
    //$$ @Redirect(method = "renderLitParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/BufferBuilder;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //$$ private void renderLitParticle(Particle particle, BufferBuilder vertexBuffer, Entity view, float partialTicks,
    //#else
    //$$ @Redirect(method = "renderLitParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;renderParticle(Lnet/minecraft/client/renderer/VertexBuffer;Lnet/minecraft/entity/Entity;FFFFFF)V"))
    //$$ private void renderLitParticle(Particle particle, VertexBuffer vertexBuffer, Entity view, float partialTicks,
    //#endif
    //$$                              float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
    //$$     renderParticle(particle, vertexBuffer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    //$$ }
    //#endif
    //$$
    //$$ private void renderParticle(Particle particle,
    //$$                             BufferBuilder vertexBuffer,
                                //#if MC>=11400
                                //$$ Camera view,
                                //#else
                                //$$ Entity view,
                                //#endif
    //$$                             float partialTicks,
    //$$                             float rotX, float rotXZ, float rotZ, float rotYZ, float rotXY) {
    //$$     EntityRendererHandler handler = ((EntityRendererHandler.IEntityRenderer) MCVer.getMinecraft().gameRenderer).replayModRender_getHandler();
    //$$     if (handler != null && handler.omnidirectional) {
    //$$         // Align all particles towards the camera
            //#if MC>=11400
            //$$ Vec3d pos = view.getPos();
            //#else
            //$$ Vec3d pos = new Vec3d(view.posX, view.posY, view.posZ);
            //#endif
    //$$         Vec3d d = MCVer.getPosition(particle, partialTicks).subtract(pos);
    //$$         double pitch = -Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z));
    //$$         double yaw = -Math.atan2(d.x, d.z);
    //$$
    //$$         rotX = (float) Math.cos(yaw);
    //$$         rotZ = (float) Math.sin(yaw);
    //$$         rotXZ = (float) Math.cos(pitch);
    //$$
    //$$         rotYZ = (float) (-rotZ * Math.sin(pitch));
    //$$         rotXY = (float) (rotX * Math.sin(pitch));
    //$$     }
    //$$     BlendState blendState = BlendState.getState();
    //$$             if (blendState != null) {
    //$$             blendState.get(ParticlesExporter.class).onRender(particle, partialTicks);
    //$$     }
    //$$     particle.buildGeometry(vertexBuffer, view, partialTicks, rotX, rotXZ, rotZ, rotYZ, rotXY);
    //$$ }
    //#endif
}
//#endif
