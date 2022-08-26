package com.replaymod.core.versions;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.gradle.remap.Pattern;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.CrashReportDetail;
import net.minecraft.ReportedException;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

class Patterns {
    //#if MC>=10904
    @Pattern
    private static void addCrashCallable(CrashReportCategory category, String name, CrashReportDetail<String> callable) {
        //#if MC>=11200
        category.setDetail(name, callable);
        //#else
        //$$ category.setDetail(name, callable);
        //#endif
    }
    //#else
    //$$ @Pattern
    //$$ private static void addCrashCallable(CrashReportCategory category, String name, Callable<String> callable) {
    //$$     category.addCrashSectionCallable(name, callable);
    //$$ }
    //#endif

    @Pattern
    private static double Entity_getX(Entity entity) {
        //#if MC>=11500
        return entity.getX();
        //#else
        //$$ return entity.x;
        //#endif
    }

    @Pattern
    private static double Entity_getY(Entity entity) {
        //#if MC>=11500
        return entity.getY();
        //#else
        //$$ return entity.y;
        //#endif
    }

    @Pattern
    private static double Entity_getZ(Entity entity) {
        //#if MC>=11500
        return entity.getZ();
        //#else
        //$$ return entity.z;
        //#endif
    }

    @Pattern
    private static void Entity_setYaw(Entity entity, float value) {
        //#if MC>=11700
        entity.setYRot(value);
        //#else
        //$$ entity.yaw = value;
        //#endif
    }

    @Pattern
    private static float Entity_getYaw(Entity entity) {
        //#if MC>=11700
        return entity.getYRot();
        //#else
        //$$ return entity.yaw;
        //#endif
    }

    @Pattern
    private static void Entity_setPitch(Entity entity, float value) {
        //#if MC>=11700
        entity.setXRot(value);
        //#else
        //$$ entity.pitch = value;
        //#endif
    }

    @Pattern
    private static float Entity_getPitch(Entity entity) {
        //#if MC>=11700
        return entity.getXRot();
        //#else
        //$$ return entity.pitch;
        //#endif
    }

    @Pattern
    private static void Entity_setPos(Entity entity, double x, double y, double z) {
        //#if MC>=11500
        entity.setPosRaw(x, y, z);
        //#else
        //$$ { net.minecraft.entity.Entity self = entity; self.x = x; self.y = y; self.z = z; }
        //#endif
    }

    //#if MC>=11400
    @Pattern
    private static void setWidth(AbstractWidget button, int value) {
        button.setWidth(value);
    }

    @Pattern
    private static int getWidth(AbstractWidget button) {
        return button.getWidth();
    }

    @Pattern
    private static int getHeight(AbstractWidget button) {
        //#if MC>=11600
        return button.getHeight();
        //#else
        //$$ return ((com.replaymod.core.mixin.AbstractButtonWidgetAccessor) button).getHeight();
        //#endif
    }
    //#else
    //$$ @Pattern
    //$$ private static void setWidth(GuiButton button, int value) {
    //$$     button.width = value;
    //$$ }
    //$$
    //$$ @Pattern
    //$$ private static int getWidth(GuiButton button) {
    //$$     return button.width;
    //$$ }
    //$$
    //$$ @Pattern
    //$$ private static int getHeight(GuiButton button) {
    //$$     return button.height;
    //$$ }
    //#endif

    @Pattern
    private static String readString(FriendlyByteBuf buffer, int max) {
        //#if MC>=10800
        return buffer.readUtf(max);
        //#else
        //$$ return com.replaymod.core.versions.MCVer.tryReadString(buffer, max);
        //#endif
    }

    @Pattern
    //#if MC>=10800
    private static Entity getRenderViewEntity(Minecraft mc) {
        return mc.getCameraEntity();
    }
    //#else
    //$$ private static EntityLivingBase getRenderViewEntity(Minecraft mc) {
    //$$     return mc.renderViewEntity;
    //$$ }
    //#endif

    @Pattern
    //#if MC>=10800
    private static void setRenderViewEntity(Minecraft mc, Entity entity) {
        mc.setCameraEntity(entity);
    }
    //#else
    //$$ private static void setRenderViewEntity(Minecraft mc, EntityLivingBase entity) {
    //$$     mc.renderViewEntity = entity;
    //$$ }
    //#endif

    @Pattern
    private static Entity getVehicle(Entity passenger) {
        //#if MC>=10904
        return passenger.getVehicle();
        //#else
        //$$ return passenger.ridingEntity;
        //#endif
    }

    @Pattern
    private static Inventory getInventory(Player entity) {
        //#if MC>=11700
        return entity.getInventory();
        //#else
        //$$ return entity.inventory;
        //#endif
    }

    @Pattern
    private static Iterable<Entity> loadedEntityList(ClientLevel world) {
        //#if MC>=11400
        return world.entitiesForRendering();
        //#else
        //#if MC>=10809
        //$$ return world.loadedEntityList;
        //#else
        //$$ return ((java.util.List<net.minecraft.entity.Entity>) world.loadedEntityList);
        //#endif
        //#endif
    }

    @Pattern
    //#if MC>=11700
    private static void getEntitySectionArray() {}
    //#else
    //$$ private static Collection<Entity>[] getEntitySectionArray(WorldChunk chunk) {
        //#if MC>=11700
        //$$ return obsolete(chunk);
        //#elseif MC>=10800
        //$$ return chunk.getEntitySectionArray();
        //#else
        //$$ return chunk.entityLists;
        //#endif
    //$$ }
    //#endif

    @Pattern
    private static List<? extends Player> playerEntities(Level world) {
        //#if MC>=11400
        return world.players();
        //#elseif MC>=10809
        //$$ return world.playerEntities;
        //#else
        //$$ return ((List<? extends net.minecraft.entity.player.EntityPlayer>) world.playerEntities);
        //#endif
    }

    @Pattern
    private static boolean isOnMainThread(Minecraft mc) {
        //#if MC>=11400
        return mc.isSameThread();
        //#else
        //$$ return mc.isCallingFromMinecraftThread();
        //#endif
    }

    @Pattern
    private static void scheduleOnMainThread(Minecraft mc, Runnable runnable) {
        //#if MC>=11400
        mc.tell(runnable);
        //#else
        //$$ mc.addScheduledTask(runnable);
        //#endif
    }

    @Pattern
    private static Window getWindow(Minecraft mc) {
        //#if MC>=11500
        return mc.getWindow();
        //#elseif MC>=11400
        //$$ return mc.window;
        //#else
        //$$ return new com.replaymod.core.versions.Window(mc);
        //#endif
    }

    @Pattern
    private static BufferBuilder Tessellator_getBuffer(Tesselator tessellator) {
        //#if MC>=10800
        return tessellator.getBuilder();
        //#else
        //$$ return new BufferBuilder(tessellator);
        //#endif
    }

    //#if MC<11700
    //$$ @Pattern
    //$$ private static void BufferBuilder_beginPosCol(BufferBuilder buffer, int mode) {
        //#if MC>=10809
        //$$ buffer.begin(mode, VertexFormats.POSITION_COLOR);
        //#else
        //$$ buffer.startDrawing(mode /* POSITION_COLOR */);
        //#endif
    //$$ }
    //$$
    //$$ @Pattern
    //$$ private static void BufferBuilder_addPosCol(BufferBuilder buffer, double x, double y, double z, int r, int g, int b, int a) {
        //#if MC>=10809
        //$$ buffer.vertex(x, y, z).color(r, g, b, a).next();
        //#else
        //$$ { WorldRenderer $buffer = buffer; double $x = x; double $y = y; double $z = z; $buffer.setColorRGBA(r, g, b, a); $buffer.addVertex($x, $y, $z); }
        //#endif
    //$$ }
    //$$
    //$$ @Pattern
    //$$ private static void BufferBuilder_beginPosTex(BufferBuilder buffer, int mode) {
        //#if MC>=10809
        //$$ buffer.begin(mode, VertexFormats.POSITION_TEXTURE);
        //#else
        //$$ buffer.startDrawing(mode /* POSITION_TEXTURE */);
        //#endif
    //$$ }
    //$$
    //$$ @Pattern
    //$$ private static void BufferBuilder_addPosTex(BufferBuilder buffer, double x, double y, double z, float u, float v) {
        //#if MC>=10809
        //$$ buffer.vertex(x, y, z).texture(u, v).next();
        //#else
        //$$ buffer.addVertexWithUV(x, y, z, u, v);
        //#endif
    //$$ }
    //$$
    //$$ @Pattern
    //$$ private static void BufferBuilder_beginPosTexCol(BufferBuilder buffer, int mode) {
        //#if MC>=10809
        //$$ buffer.begin(mode, VertexFormats.POSITION_TEXTURE_COLOR);
        //#else
        //$$ buffer.startDrawing(mode /* POSITION_TEXTURE_COLOR */);
        //#endif
    //$$ }
    //$$
    //$$ @Pattern
    //$$ private static void BufferBuilder_addPosTexCol(BufferBuilder buffer, double x, double y, double z, float u, float v, int r, int g, int b, int a) {
        //#if MC>=10809
        //$$ buffer.vertex(x, y, z).texture(u, v).color(r, g, b, a).next();
        //#else
        //$$ { WorldRenderer $buffer = buffer; double $x = x; double $y = y; double $z = z; float $u = u; float $v = v; $buffer.setColorRGBA(r, g, b, a); $buffer.addVertexWithUV($x, $y, $z, $u, $v); }
        //#endif
    //$$ }
    //#else
    @Pattern private static void BufferBuilder_beginPosCol() {}
    @Pattern private static void BufferBuilder_addPosCol() {}
    @Pattern private static void BufferBuilder_beginPosTex() {}
    @Pattern private static void BufferBuilder_addPosTex() {}
    @Pattern private static void BufferBuilder_beginPosTexCol() {}
    @Pattern private static void BufferBuilder_addPosTexCol() {}
    //#endif

    @Pattern
    private static Tesselator Tessellator_getInstance() {
        //#if MC>=10800
        return Tesselator.getInstance();
        //#else
        //$$ return Tessellator.instance;
        //#endif
    }

    @Pattern
    private static EntityRenderDispatcher getEntityRenderDispatcher(Minecraft mc) {
        //#if MC>=10800
        return mc.getEntityRenderDispatcher();
        //#else
        //$$ return com.replaymod.core.versions.MCVer.getRenderManager(mc);
        //#endif
    }

    @Pattern
    private static float getCameraYaw(EntityRenderDispatcher dispatcher) {
        //#if MC>=11500
        return dispatcher.camera.getYRot();
        //#else
        //$$ return dispatcher.cameraYaw;
        //#endif
    }

    @Pattern
    private static float getCameraPitch(EntityRenderDispatcher dispatcher) {
        //#if MC>=11500
        return dispatcher.camera.getXRot();
        //#else
        //$$ return dispatcher.cameraPitch;
        //#endif
    }

    @Pattern
    private static float getRenderPartialTicks(Minecraft mc) {
        //#if MC>=10900
        return mc.getFrameTime();
        //#else
        //$$ return ((com.replaymod.core.mixin.MinecraftAccessor) mc).getTimer().renderPartialTicks;
        //#endif
    }

    @Pattern
    private static TextureManager getTextureManager(Minecraft mc) {
        //#if MC>=11400
        return mc.getTextureManager();
        //#else
        //$$ return mc.renderEngine;
        //#endif
    }

    @Pattern
    private static String getBoundKeyName(KeyMapping keyBinding) {
        //#if MC>=11600
        return keyBinding.getTranslatedKeyMessage().getString();
        //#elseif MC>=11400
        //$$ return keyBinding.getLocalizedName();
        //#else
        //$$ return org.lwjgl.input.Keyboard.getKeyName(keyBinding.getKeyCode());
        //#endif
    }

    @Pattern
    private static SimpleSoundInstance master(ResourceLocation sound, float pitch) {
        //#if MC>=10900
        return SimpleSoundInstance.forUI(new SoundEvent(sound), pitch);
        //#elseif MC>=10800
        //$$ return PositionedSoundRecord.create(sound, pitch);
        //#else
        //$$ return PositionedSoundRecord.createPositionedSoundRecord(sound, pitch);
        //#endif
    }

    @Pattern
    private static boolean isKeyBindingConflicting(KeyMapping a, KeyMapping b) {
        //#if MC>=10900
        return a.same(b);
        //#else
        //$$ return (a.getKeyCode() == b.getKeyCode());
        //#endif
    }

    //#if MC>=11600
    @Pattern
    private static void BufferBuilder_beginLineStrip(BufferBuilder buffer, VertexFormat vertexFormat) {
        //#if MC>=11700
        buffer.begin(VertexFormat.Mode.LINE_STRIP, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        //#else
        //$$ buffer.begin(GL11.GL_LINE_STRIP, VertexFormats.POSITION_COLOR);
        //#endif
    }

    @Pattern
    private static void BufferBuilder_beginLines(BufferBuilder buffer) {
        //#if MC>=11700
        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        //#else
        //$$ buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
        //#endif
    }

    @Pattern
    private static void BufferBuilder_beginQuads(BufferBuilder buffer, VertexFormat vertexFormat) {
        //#if MC>=11700
        buffer.begin(VertexFormat.Mode.QUADS, vertexFormat);
        //#else
        //$$ buffer.begin(GL11.GL_QUADS, vertexFormat);
        //#endif
    }
    //#else
    //$$ @Pattern private static void BufferBuilder_beginLineStrip() {}
    //$$ @Pattern private static void BufferBuilder_beginLines() {}
    //$$ @Pattern private static void BufferBuilder_beginQuads() {}
    //#endif

    @Pattern
    private static void GL11_glLineWidth(float width) {
        //#if MC>=11700
        com.mojang.blaze3d.systems.RenderSystem.lineWidth(width);
        //#else
        //$$ GL11.glLineWidth(width);
        //#endif
    }

    @Pattern
    private static void GL11_glTranslatef(float x, float y, float z) {
        //#if MC>=11700
        com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().translate(x, y, z);
        //#else
        //$$ GL11.glTranslatef(x, y, z);
        //#endif
    }

    @Pattern
    private static void GL11_glRotatef(float angle, float x, float y, float z) {
        //#if MC>=11700
        { float $angle = angle; com.mojang.blaze3d.systems.RenderSystem.getModelViewStack().mulPose(new com.mojang.math.Quaternion(new com.mojang.math.Vector3f(x, y, z), $angle, true)); }
        //#else
        //$$ GL11.glRotatef(angle, x, y, z);
        //#endif
    }

    // FIXME preprocessor bug: there are mappings for this, not sure why it doesn't remap by itself
    //#if MC>=11600
    @Pattern
    private static Matrix4f getPositionMatrix(PoseStack.Pose stack) {
        //#if MC>=11800
        return stack.pose();
        //#else
        //$$ return stack.getModel();
        //#endif
    }
    //#else
    //$$ private static void getPositionMatrix() {}
    //#endif

    @SuppressWarnings("rawtypes") // preprocessor bug: doesn't work with generics
    @Pattern
    private static void Futures_addCallback(ListenableFuture future, FutureCallback callback) {
        //#if MC>=11800
        Futures.addCallback(future, callback, Runnable::run);
        //#else
        //$$ Futures.addCallback(future, callback);
        //#endif
    }

    @Pattern
    private static void setCrashReport(Minecraft mc, CrashReport report) {
        //#if MC>=11900
        mc.delayCrashRaw(report);
        //#elseif MC>=11800
        //$$ mc.setCrashReportSupplier(() -> report);
        //#else
        //$$ mc.setCrashReport(report);
        //#endif
    }

    @Pattern
    private static ReportedException crashReportToException(Minecraft mc) {
        //#if MC>=11800
        return new ReportedException(((MinecraftAccessor) mc).getCrashReporter().get());
        //#else
        //$$ return new CrashException(((MinecraftAccessor) mc).getCrashReporter());
        //#endif
    }

    @Pattern
    private static Vec3 getTrackedPosition(Entity entity) {
        //#if MC>=11604
        return entity.getPositionCodec().decode(0, 0, 0);
        //#else
        //$$ return com.replaymod.core.versions.MCVer.getTrackedPosition(entity);
        //#endif
    }

    @Pattern
    private static Component newTextLiteral(String str) {
        //#if MC>=11900
        return Component.literal(str);
        //#else
        //$$ return new LiteralText(str);
        //#endif
    }

    @Pattern
    private static Component newTextTranslatable(String key, Object...args) {
        //#if MC>=11900
        return Component.translatable(key, args);
        //#else
        //$$ return new TranslatableText(key, args);
        //#endif
    }

    //#if MC>=11500
    @Pattern
    private static Vec3 getTrackedPos(Entity entity) {
        //#if MC>=11900
        return entity.getPositionCodec().decode(0, 0, 0);
        //#else
        //$$ return entity.getTrackedPosition();
        //#endif
    }
    //#else
    //$$ @Pattern private static void getTrackedPos() {}
    //#endif

    @Pattern
    private static void setGamma(Options options, double value) {
        //#if MC>=11900
        ((com.replaymod.core.mixin.SimpleOptionAccessor<Double>) (Object) options.gamma()).setRawValue(value);
        //#elseif MC>=11400
        //$$ options.gamma = value;
        //#else
        //$$ options.gammaSetting = (float) value;
        //#endif
    }

    @Pattern
    private static double getGamma(Options options) {
        //#if MC>=11900
        return options.gamma().get();
        //#else
        //$$ return options.gamma;
        //#endif
    }

    @Pattern
    private static int getViewDistance(Options options) {
        //#if MC>=11900
        return options.renderDistance().get();
        //#else
        //$$ return options.viewDistance;
        //#endif
    }

    @Pattern
    private static double getFov(Options options) {
        //#if MC>=11900
        return options.fov().get();
        //#else
        //$$ return options.fov;
        //#endif
    }

    @Pattern
    private static int getGuiScale(Options options) {
        //#if MC>=11900
        return options.guiScale().get();
        //#else
        //$$ return options.guiScale;
        //#endif
    }

    @Pattern
    private static Resource getResource(ResourceManager manager, ResourceLocation id) throws IOException {
        //#if MC>=11900
        return manager.getResourceOrThrow(id);
        //#else
        //$$ return manager.getResource(id);
        //#endif
    }

    @Pattern
    private static List<ItemStack> DefaultedList_ofSize_ItemStack_Empty(int size) {
        //#if MC>=11100
        return NonNullList.withSize(size, ItemStack.EMPTY);
        //#else
        //$$ return java.util.Arrays.asList(new ItemStack[size]);
        //#endif
    }
}
