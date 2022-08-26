package com.replaymod.replay.mixin;

import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.camera.CameraEntity;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.stats.StatsCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinPlayerControllerMP {

    @Shadow
    private Minecraft client;

    @Shadow
    //#if MC>=10904
    private ClientPacketListener networkHandler;
    //#else
    //$$ private NetHandlerPlayClient netClientHandler;
    //#endif

    //#if MC>=11400
    //#if MC>=11602
    @Inject(method = "createPlayer(Lnet/minecraft/client/world/ClientWorld;Lnet/minecraft/stat/StatHandler;Lnet/minecraft/client/recipebook/ClientRecipeBook;ZZ)Lnet/minecraft/client/network/ClientPlayerEntity;", at=@At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "createPlayer", at=@At("HEAD"), cancellable = true)
    //#endif
    private void replayModReplay_createReplayCamera(
            //#if MC>=11400
            ClientLevel worldIn,
            //#else
            //$$ World worldIn,
            //#endif
            StatsCounter statisticsManager,
            ClientRecipeBook recipeBookClient,
            //#if MC>=11600
            boolean lastIsHoldingSneakKey,
            boolean lastSprinting,
            //#endif
            CallbackInfoReturnable<LocalPlayer> ci
    ) {
        if (ReplayModReplay.instance.getReplayHandler() != null) {
            ci.setReturnValue(new CameraEntity(this.client, worldIn, this.networkHandler, statisticsManager, recipeBookClient));
    //#else
    //#if MC>=11200
    //$$ @Inject(method = "func_192830_a", at=@At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_createReplayCamera(World worldIn, StatisticsManager statisticsManager, RecipeBook recipeBook, CallbackInfoReturnable<EntityPlayerSP> ci) {
    //$$     if (ReplayModReplay.instance.getReplayHandler() != null) {
    //$$         ci.setReturnValue(new CameraEntity(this.mc, worldIn, this.connection, statisticsManager, recipeBook));
    //#else
    //#if MC>=10904
    //$$ @Inject(method = "createClientPlayer", at=@At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_createReplayCamera(World worldIn, StatisticsManager statisticsManager, CallbackInfoReturnable<EntityPlayerSP> ci) {
    //$$     if (ReplayModReplay.instance.getReplayHandler() != null) {
    //$$         ci.setReturnValue(new CameraEntity(this.mc, worldIn, this.connection, statisticsManager));
    //#else
    //#if MC>=10800
    //$$ @Inject(method = "func_178892_a", at=@At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_createReplayCamera(World worldIn, StatFileWriter statFileWriter, CallbackInfoReturnable<EntityPlayerSP> ci) {
    //$$     if (ReplayModReplay.instance.getReplayHandler() != null) {
    //$$         ci.setReturnValue(new CameraEntity(this.mc, worldIn, this.netClientHandler, statFileWriter));
    //#else
    //$$ @Inject(method = "createPlayer", at=@At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_createReplayCamera(World worldIn, StatFileWriter statFileWriter, CallbackInfoReturnable<EntityClientPlayerMP> ci) {
    //$$     if (ReplayModReplay.instance.getReplayHandler() != null) {
    //$$         ci.setReturnValue(new CameraEntity(this.mc, worldIn, this.mc.getSession(), this.netClientHandler, statFileWriter));
    //#endif
    //#endif
    //#endif
    //#endif
            ci.cancel();
        }
    }

    //#if MC>=10800
    //#if MC>=11400
    @Inject(method = "isFlyingLocked", at=@At("HEAD"), cancellable = true)
    //#else
    //$$ @Inject(method = "isSpectator", at=@At("HEAD"), cancellable = true)
    //#endif
    private void replayModReplay_isSpectator(CallbackInfoReturnable<Boolean> ci) {
        if (this.client.player instanceof CameraEntity) { // this check should in theory not be required
            ci.setReturnValue(this.client.player.isSpectator());
        }
    }
    //#endif

    //#if MC<=10710
    //$$ // Prevent the disconnect GUI from being opened during the short time when the replay is restarted
    //$$ // at which the old network manager is closed but still getting ticked (hence the disconnect GUI opening).
    //$$ @Inject(method = "updateController", at = @At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_onlyTickNeverDisconnect(CallbackInfo ci) {
    //$$     if (ReplayModReplay.instance.getReplayHandler() != null) {
    //$$         if (netClientHandler.getNetworkManager().isChannelOpen()) {
    //$$             netClientHandler.getNetworkManager().processReceivedPackets();
    //$$         }
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //$$
    //$$ @Inject(method = "resetBlockRemoving", at = @At("HEAD"), cancellable = true)
    //$$ private void replayModReplay_skipWorldTick(CallbackInfo ci) {
    //$$     if (this.mc.theWorld == null) {
    //$$         ci.cancel();
    //$$     }
    //$$ }
    //#endif
}
