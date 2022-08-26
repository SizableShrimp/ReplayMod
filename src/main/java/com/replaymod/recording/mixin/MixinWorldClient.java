package com.replaymod.recording.mixin;

//#if MC>=10904
import com.replaymod.recording.handler.RecordingEventHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Supplier;
//#else
//$$ import net.minecraft.world.level.LevelProperties;
//#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import java.util.function.BiFunction;
//#else
//$$ import net.minecraft.world.storage.ISaveHandler;
//#if MC>=11400
//$$ import net.minecraft.world.dimension.Dimension;
//$$ import net.minecraft.world.storage.WorldSavedDataStorage;
//#else
//$$ import net.minecraft.world.WorldProvider;
//#endif
//#endif


@Mixin(ClientLevel.class)
public abstract class MixinWorldClient extends Level implements RecordingEventHandler.RecordingEventSender {
    @Shadow
    private Minecraft client;

    //#if MC>=11600
    protected MixinWorldClient(WritableLevelData mutableWorldProperties, ResourceKey<Level> registryKey,
                               //#if MC<11602
                               //$$ RegistryKey<DimensionType> registryKey2,
                               //#endif
                               //#if MC>=11802
                               Holder<DimensionType> dimensionType,
                               //#else
                               //$$ DimensionType dimensionType,
                               //#endif
                               Supplier<ProfilerFiller> profiler, boolean bl, boolean bl2, long l
                               //#if MC>=11900
                               , int maxChainedNeighborUpdates
                               //#endif
    ) {
        super(mutableWorldProperties, registryKey,
                //#if MC<11602
                //$$ registryKey2,
                //#endif
                dimensionType, profiler, bl, bl2, l
                //#if MC>=11900
                , maxChainedNeighborUpdates
                //#endif
        );
    }
    //#else
    //#if MC>=11400
    //$$ protected MixinWorldClient(LevelProperties levelProperties_1, DimensionType dimensionType_1, BiFunction<World, Dimension, ChunkManager> biFunction_1, Profiler profiler_1, boolean boolean_1) {
    //$$     super(levelProperties_1, dimensionType_1, biFunction_1, profiler_1, boolean_1);
    //$$ }
    //#else
    //$$ protected MixinWorldClient(ISaveHandler saveHandlerIn,
                               //#if MC>=11400
                               //$$ WorldSavedDataStorage mapStorage,
                               //#endif
    //$$                            WorldInfo info,
                               //#if MC>=11400
                               //$$ Dimension providerIn,
                               //#else
                               //$$ WorldProvider providerIn,
                               //#endif
    //$$                            Profiler profilerIn, boolean client) {
    //$$     super(saveHandlerIn,
                //#if MC>=11400
                //$$ mapStorage,
                //#endif
    //$$             info, providerIn, profilerIn, client);
    //$$ }
    //#endif
    //#endif

    private RecordingEventHandler replayModRecording_getRecordingEventHandler() {
        return ((RecordingEventHandler.RecordingEventSender) this.client.levelRenderer).getRecordingEventHandler();
    }

    // Sounds that are emitted by thePlayer no longer take the long way over the server
    // but are instead played directly by the client. The server only sends these sounds to
    // other clients so we have to record them manually.
    // E.g. Block place sounds
    //#if MC>=11900
    @Inject(method = "playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FFJ)V",
            at = @At("HEAD"))
    //#elseif MC>=11400
    //#if FABRIC>=1
    //$$ @Inject(method = "playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/sound/SoundEvent;Lnet/minecraft/sound/SoundCategory;FF)V",
    //$$         at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "playSound(Lnet/minecraft/entity/player/PlayerEntity;DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V",
    //$$         at = @At("HEAD"))
    //#endif
    //#else
    //$$ @Inject(method = "playSound(Lnet/minecraft/entity/player/EntityPlayer;DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V",
    //$$         at = @At("HEAD"))
    //#endif
    public void replayModRecording_recordClientSound(
            Player player, double x, double y, double z, SoundEvent sound, SoundSource category,
            float volume, float pitch,
            //#if MC>=11900
            long seed,
            //#endif
            CallbackInfo ci) {
        if (player == this.client.player) {
            RecordingEventHandler handler = replayModRecording_getRecordingEventHandler();
            if (handler != null) {
                // Sent to all other players in ServerWorldEventHandler#playSoundToAllNearExcept
                handler.onPacket(new ClientboundSoundPacket(
                        sound, category, x, y, z, volume, pitch
                        //#if MC>=11900
                        , seed
                        //#endif
                ));
            }
        }
    }

    // Same goes for level events (also called effects). E.g. door open, block break, etc.
    //#if MC>=11400
    //#if MC>=11600
    @Inject(method = "syncWorldEvent", at = @At("HEAD"))
    //#else
    //$$ @Inject(method = "playLevelEvent", at = @At("HEAD"))
    //#endif
    private void playLevelEvent (Player player, int type, BlockPos pos, int data, CallbackInfo ci) {
    //#else
    //$$ // These are handled in the World class, so we override the method in WorldClient and add our special handling.
    //$$ @Override
    //$$ public void playEvent (EntityPlayer player, int type, BlockPos pos, int data) {
    //#endif
        if (player == this.client.player) {
            // We caused this event, the server won't send it to us
            RecordingEventHandler handler = replayModRecording_getRecordingEventHandler();
            if (handler != null) {
                handler.onClientEffect(type, pos, data);
            }
        }
        //#if MC<11400
        //$$ super.playEvent(player, type, pos, data);
        //#endif
    }
}
//#endif
