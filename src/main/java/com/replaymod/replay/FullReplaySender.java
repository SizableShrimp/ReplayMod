package com.replaymod.replay;

import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.tcp.io.ByteBufNetOutput;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.mixin.MinecraftAccessor;
import com.replaymod.core.mixin.TimerAccessor;
import com.replaymod.core.utils.Restrictions;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replaystudio.io.ReplayInputStream;
import com.replaymod.replaystudio.replay.ReplayFile;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.versions.callbacks.PreTickCallback;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

//#if MC>=11600
//#else
//$$ import net.minecraft.network.packet.s2c.play.EntitySpawnGlobalS2CPacket;
//#endif

//#if MC>=11400
import com.replaymod.core.versions.MCVer;
//#if MC>=11200
import com.replaymod.core.utils.WrappedTimer;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAddExperienceOrbPacket;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket;
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatHeaderPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;

import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.replaystudio.util.Utils.readInt;

/**
 * Sends replay packets to netty channels.
 * Even though {@link Sharable}, this should never be added to multiple pipes at once, it may however be re-added when
 * the replay restart from the beginning.
 */
@Sharable
public class FullReplaySender extends ChannelDuplexHandler implements ReplaySender {
    /**
     * These packets are ignored completely during replay.
     */
    private static final List<Class> BAD_PACKETS = Arrays.<Class>asList(
            //#if MC>=11404
            ClientboundBlockChangedAckPacket.class,
            //#endif
            //#if MC>=11400
            ClientboundOpenBookPacket.class,
            ClientboundOpenScreenPacket.class,
            //#endif
            //#if MC>=11200
            ClientboundUpdateRecipesPacket.class,
            ClientboundUpdateAdvancementsPacket.class,
            ClientboundSelectAdvancementsTabPacket.class,
            //#endif
            //#if MC>=10800
            ClientboundSetCameraPacket.class,
            ClientboundSetTitleTextPacket.class,
            //#endif
            ClientboundSetHealthPacket.class,
            ClientboundHorseScreenOpenPacket.class,
            ClientboundContainerClosePacket.class,
            ClientboundContainerSetSlotPacket.class,
            ClientboundContainerSetDataPacket.class,
            ClientboundOpenSignEditorPacket.class,
            ClientboundAwardStatsPacket.class,
            ClientboundSetExperiencePacket.class,
            ClientboundPlayerAbilitiesPacket.class
    );

    private static int TP_DISTANCE_LIMIT = 128;

    /**
     * The replay handler responsible for the current replay.
     */
    private final ReplayHandler replayHandler;

    /**
     * Whether to work in async mode.
     *
     * When in async mode, a separate thread send packets and waits according to their delays.
     * This is default in normal playback mode.
     *
     * When in sync mode, no packets will be sent until {@link #sendPacketsTill(int)} is called.
     * This is used during path playback and video rendering.
     */
    protected boolean asyncMode;

    /**
     * Timestamp of the last packet sent in milliseconds since the start.
     */
    protected int lastTimeStamp;

    /**
     * @see #currentTimeStamp()
     */
    protected int currentTimeStamp;

    /**
     * The replay file.
     */
    protected ReplayFile replayFile;

    /**
     * The channel handler context used to send packets to minecraft.
     */
    protected ChannelHandlerContext ctx;

    /**
     * The replay input stream from which new packets are read.
     * When accessing this stream make sure to synchronize on {@code this} as it's used from multiple threads.
     */
    protected ReplayInputStream replayIn;

    /**
     * The next packet that should be sent.
     * This is required as some actions such as jumping to a specified timestamp have to peek at the next packet.
     */
    protected PacketData nextPacket;

    /**
     * Whether we're currently reading packets from the login phase.
     */
    private boolean loginPhase = true;

    /**
     * Whether we need to restart the current replay. E.g. when jumping backwards in time
     */
    protected boolean startFromBeginning = true;

    /**
     * Whether to terminate the replay. This only has an effect on the async mode and is {@code true} during sync mode.
     */
    protected boolean terminate;

    /**
     * The speed of the replay. 1 is normal, 2 is twice as fast, 0.5 is half speed and 0 is frozen
     */
    protected double replaySpeed = 1f;

    /**
     * Whether the world has been loaded and the dirt-screen should go away.
     */
    protected boolean hasWorldLoaded;

    /**
     * The minecraft instance.
     */
    protected Minecraft mc = getMinecraft();

    /**
     * The total length of this replay in milliseconds.
     */
    protected final int replayLength;

    /**
     * Our actual entity id that the server gave to us.
     */
    protected int actualID = -1;

    /**
     * Whether to allow (process) the next player movement packet.
     *
     * Must only be accessed from the main thread.
     */
    protected boolean allowMovement;

    /**
     * Directory to which resource packs are extracted.
     */
    private final File tempResourcePackFolder = Files.createTempDir();

    private final EventHandler events = new EventHandler();

    /**
     * Create a new replay sender.
     * @param file The replay file
     * @param asyncMode {@code true} for async mode, {@code false} otherwise
     * @see #asyncMode
     */
    public FullReplaySender(ReplayHandler replayHandler, ReplayFile file, boolean asyncMode) throws IOException {
        this.replayHandler = replayHandler;
        this.replayFile = file;
        this.asyncMode = asyncMode;
        this.replayLength = file.getMetaData().getDuration();

        events.register();

        if (asyncMode) {
            new Thread(asyncSender, "replaymod-async-sender").start();
        }
    }

    /**
     * Set whether this replay sender operates in async mode.
     * When in async mode, it will send packets timed from a separate thread.
     * When not in async mode, it will send packets when {@link #sendPacketsTill(int)} is called.
     * @param asyncMode {@code true} to enable async mode
     */
    @Override
    public void setAsyncMode(boolean asyncMode) {
        if (this.asyncMode == asyncMode) return;
        this.asyncMode = asyncMode;
        if (asyncMode) {
            this.terminate = false;
            new Thread(asyncSender, "replaymod-async-sender").start();
        } else {
            this.terminate = true;
        }
    }

    @Override
    public boolean isAsyncMode() {
        return asyncMode;
    }

    /**
     * Set whether this replay sender  to operate in sync mode.
     * When in sync mode, it will send packets when {@link #sendPacketsTill(int)} is called.
     * This call will block until the async worker thread has stopped.
     */
    @Override
    public void setSyncModeAndWait() {
        if (!this.asyncMode) return;
        this.asyncMode = false;
        this.terminate = true;
        synchronized (this) {
            // This will wait for the worker thread to leave the synchronized code part
        }
    }

    /**
     * Return a fake system tile in milliseconds value that respects slowdown/speedup/pause and works in both,
     * sync and async mode.
     * Note: For sync mode this returns the last value passed to {@link #sendPacketsTill(int)}.
     * @return The timestamp in milliseconds since the start of the replay
     */
    @Override
    public int currentTimeStamp() {
        if (asyncMode && !paused()) {
            return (int) ((System.currentTimeMillis() - realTimeStart) * realTimeStartSpeed);
        } else {
            return lastTimeStamp;
        }
    }

    /**
     * Terminate this replay sender.
     */
    public void terminateReplay() {
        if (terminate) {
            return;
        }
        terminate = true;
        syncSender.shutdown();
        events.unregister();
        try {
            channelInactive(ctx);
            ctx.channel().pipeline().close();
            FileUtils.deleteDirectory(tempResourcePackFolder);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private class EventHandler extends EventRegistrations {
        { on(PreTickCallback.EVENT, this::onWorldTick); }
        private void onWorldTick() {
            // Spawning a player into an empty chunk (which we might do with the recording player)
            // prevents it from being moved by teleport packets (it essentially gets stuck) because
            // Entity#addedToChunk is not set and it is therefore not updated every tick.
            // To counteract this, we need to manually update it's position if it hasn't been added
            // to any chunk yet.
            // The `updateNeeded` flag appears to have been removed in 1.17, so this should no longer be an issue.
            //#if MC<11700
            //$$ if (mc.world != null) {
            //$$     for (PlayerEntity playerEntity : mc.world.getPlayers()) {
            //$$         if (!playerEntity.updateNeeded && playerEntity instanceof OtherClientPlayerEntity) {
            //$$             playerEntity.tickMovement();
            //$$         }
            //$$     }
            //$$ }
            //#endif
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        // When in async mode and the replay sender shut down, then don't send packets
        if(terminate && asyncMode) {
            return;
        }

        // When a packet is sent directly, perform no filtering
        if(msg instanceof Packet) {
            super.channelRead(ctx, msg);
        }

        if (msg instanceof byte[]) {
            try {
                Packet p = deserializePacket((byte[]) msg);

                if (p != null) {
                    p = processPacket(p);
                    if (p != null) {
                        super.channelRead(ctx, p);
                    }

                    maybeRemoveDeadEntities(p);

                    //#if MC>=11400
                    if (p instanceof ClientboundLevelChunkWithLightPacket) {
                        Runnable doLightUpdates = () -> {
                            ClientLevel world = mc.level;
                            if (world != null) {
                                //#if MC>=11800
                                while (!world.isLightUpdateQueueEmpty()) {
                                    world.pollLightUpdates();
                                }
                                //#endif
                                LevelLightEngine provider = world.getChunkSource().getLightEngine();
                                while (provider.hasLightWork()) {
                                    provider.runUpdates(Integer.MAX_VALUE, true, true);
                                }
                            }
                        };
                        if (mc.isSameThread()) {
                            doLightUpdates.run();
                        } else {
                            mc.tell(doLightUpdates);
                        }
                    }
                    //#endif
                }
            } catch (Exception e) {
                // We'd rather not have a failure parsing one packet screw up the whole replay process
                e.printStackTrace();
            }
        }

    }

    private Packet deserializePacket(byte[] bytes) throws IOException, IllegalAccessException, InstantiationException {
        ByteBuf bb = Unpooled.wrappedBuffer(bytes);
        FriendlyByteBuf pb = new FriendlyByteBuf(bb);

        int i = pb.readVarInt();

        ConnectionProtocol state = loginPhase ? ConnectionProtocol.LOGIN : ConnectionProtocol.PLAY;
        //#if MC>=11700
        Packet p = state.createPacket(PacketFlow.CLIENTBOUND, i, pb);
        //#else
        //#if MC>=10800
        //$$ Packet p = state.getPacketHandler(NetworkSide.CLIENTBOUND, i);
        //#else
        //$$ Packet p = Packet.generatePacket(state.func_150755_b(), i);
        //#endif
        //$$ p.read(pb);
        //#endif

        return p;
    }

    // If we do not give minecraft time to tick, there will be dead entity artifacts left in the world
    // Therefore we have to remove all loaded, dead entities manually if we are in sync mode.
    // We do this after every SpawnX packet and after the destroy entities packet.
    private void maybeRemoveDeadEntities(Packet packet) {
        if (asyncMode) {
            return; // MC should have enough time to tick
        }

        boolean relevantPacket = packet instanceof ClientboundAddPlayerPacket
                || packet instanceof ClientboundAddEntityPacket
                //#if MC<11900
                //$$ || packet instanceof MobSpawnS2CPacket
                //$$ || packet instanceof PaintingSpawnS2CPacket
                //#endif
                //#if MC<11600
                //$$ || packet instanceof EntitySpawnGlobalS2CPacket
                //#endif
                || packet instanceof ClientboundAddExperienceOrbPacket
                || packet instanceof ClientboundRemoveEntitiesPacket;
        if (!relevantPacket) {
            return; // don't want to do it too often, only when there's likely to be a dead entity
        }

        mc.tell(() -> {
            ClientLevel world = mc.level;
            if (world != null) {
                removeDeadEntities(world);
            }
        });
    }

    private void removeDeadEntities(ClientLevel world) {
        //#if MC>=11700
        // From the looks of it, this has now been resolved (thanks to EntityChangeListener)
        //#elseif MC>=11400
        //$$ // Note: Not sure if it's still required but there's this really handy method anyway
        //$$ world.finishRemovingEntities();
        //#else
        //$$ Iterator<Entity> iter = world.loadedEntityList.iterator();
        //$$ while (iter.hasNext()) {
        //$$     Entity entity = iter.next();
        //$$     if (entity.isDead) {
        //$$         int chunkX = entity.chunkCoordX;
        //$$         int chunkY = entity.chunkCoordZ;
        //$$
                //#if MC>=11400
                //$$ if (entity.addedToChunk && world.getChunkProvider().provideChunk(chunkX, chunkY, false, false) != null) {
                //#else
                //#if MC>=10904
                //$$ if (entity.addedToChunk && world.getChunkProvider().getLoadedChunk(chunkX, chunkY) != null) {
                //#else
                //$$ if (entity.addedToChunk && world.getChunkProvider().chunkExists(chunkX, chunkY)) {
                //#endif
                //#endif
        //$$             world.getChunkFromChunkCoords(chunkX, chunkY).removeEntity(entity);
        //$$         }
        //$$
        //$$         iter.remove();
        //$$         world.onEntityRemoved(entity);
        //$$     }
        //$$
        //$$ }
        //#endif
    }

    /**
     * Process a packet and return the result.
     * @param p The packet to process
     * @return The processed packet or {@code null} if no packet shall be sent
     */
    protected Packet processPacket(Packet p) throws Exception {
        if (p instanceof ClientboundGameProfilePacket) {
            loginPhase = false;
            return p;
        }

        if (p instanceof ClientboundCustomPayloadPacket) {
            ClientboundCustomPayloadPacket packet = (ClientboundCustomPayloadPacket) p;
            if (Restrictions.PLUGIN_CHANNEL.equals(packet.getIdentifier())) {
                final String unknown = replayHandler.getRestrictions().handle(packet);
                if (unknown == null) {
                    return null;
                } else {
                    // Failed to parse options, make sure that under no circumstances further packets are parsed
                    terminateReplay();
                    // Then end replay and show error GUI
                    ReplayMod.instance.runLater(() -> {
                        try {
                            replayHandler.endReplay();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mc.setScreen(new AlertScreen(
                                //#if MC>=11400
                                () -> mc.setScreen(null),
                                Component.translatable("replaymod.error.unknownrestriction1"),
                                Component.translatable("replaymod.error.unknownrestriction2", unknown)
                                //#else
                                //$$ I18n.format("replaymod.error.unknownrestriction1"),
                                //$$ I18n.format("replaymod.error.unknownrestriction2", unknown)
                                //#endif
                        ));
                    });
                }
            }
        }
        if (p instanceof ClientboundDisconnectPacket) {
            Component reason = ((ClientboundDisconnectPacket) p).getReason();
            String message = reason.getString();
            if ("Please update to view this replay.".equals(message)) {
                // This version of the mod supports replay restrictions so we are allowed
                // to remove this packet.
                return null;
            }
        }

        if(BAD_PACKETS.contains(p.getClass())) return null;

        if (p instanceof ClientboundCustomPayloadPacket) {
            ClientboundCustomPayloadPacket packet = (ClientboundCustomPayloadPacket) p;
            //#if MC>=11400
            ResourceLocation channelName = packet.getIdentifier();
            //#else
            //$$ String channelName = packet.getChannelName();
            //#endif
            String channelNameStr = channelName.toString();

            if (channelNameStr.startsWith("fabric-screen-handler-api-v")) {
                return null; // we do not want to show modded screens which got opened for the recording player
            }

            // On 1.14+ there's a dedicated OpenWrittenBookS2CPacket now
            //#if MC<11400
            //#if MC>=11400
            //$$ if (SPacketCustomPayload.BOOK_OPEN.equals(channelName)) {
            //#else
            //$$ if ("MC|BOpen".equals(channelName)) {
            //#endif
            //$$     return null;
            //$$ }
            //#endif
        //#if MC>=10800
        }

        if(p instanceof ClientboundResourcePackPacket) {
            ClientboundResourcePackPacket packet = (ClientboundResourcePackPacket) p;
            String url = packet.getUrl();
            if (url.startsWith("replay://")) {
        //#else
        //$$     String url;
        //$$     if ("MC|RPack".equals(channelName) &&
        //$$             (url = new String(packet.func_149168_d(), Charsets.UTF_8)).startsWith("replay://")) {
        //#endif
                int id = Integer.parseInt(url.substring("replay://".length()));
                Map<Integer, String> index = replayFile.getResourcePackIndex();
                if (index != null) {
                    String hash = index.get(id);
                    if (hash != null) {
                        File file = new File(tempResourcePackFolder, hash + ".zip");
                        if (!file.exists()) {
                            IOUtils.copy(replayFile.getResourcePack(hash).get(), new FileOutputStream(file));
                        }
                        setServerResourcePack(file);
                    }
                }
                return null;
            }
        }

        if(p instanceof ClientboundLoginPacket) {
            ClientboundLoginPacket packet = (ClientboundLoginPacket) p;
            int entId = packet.playerId();
            schedulePacketHandler(() -> allowMovement = true);
            actualID = entId;
            entId = -1789435; // Camera entity id should be negative which is an invalid id and can't be used by servers
            //#if MC>=11400
            p = new ClientboundLoginPacket(
                    entId,
                    //#if MC>=11800
                    packet.hardcore(),
                    //#endif
                    GameType.SPECTATOR,
                    //#if MC>=11600
                    GameType.SPECTATOR,
                    //#endif
                    //#if MC<11800
                    //#if MC>=11500
                    //$$ packet.getSha256Seed(),
                    //#endif
                    //$$ false,
                    //#endif
                    //#if MC>=11600
                    //#if MC>=11603
                    packet.levels(),
                    //#if MC>=11800
                    packet.registryHolder(),
                    //#else
                    //$$ (net.minecraft.util.registry.DynamicRegistryManager.Impl) packet.getRegistryManager(),
                    //#endif
                    packet.dimensionType(),
                    //#else
                    //$$ packet.method_29443(),
                    //$$ (net.minecraft.util.registry.RegistryTracker.Modifiable) packet.getDimension(),
                    //$$ packet.method_29444(),
                    //#endif
                    packet.dimension(),
                    //#else
                    //$$ packet.getDimension(),
                    //#endif
                    //#if MC>=11800
                    packet.seed(),
                    //#endif
                    0, // max players (has no getter -> never actually used)
                    //#if MC<11600
                    //$$ packet.getGeneratorType(),
                    //#endif
                    packet.chunkRadius(),
                    //#if MC>=11800
                    packet.simulationDistance(),
                    //#endif
                    packet.reducedDebugInfo()
                    //#if MC>=11500
                    , packet.showDeathScreen()
                    //#endif
                    //#if MC>=11600
                    , packet.isDebug()
                    , packet.isFlat()
                    //#endif
                    //#if MC>=11900
                    , java.util.Optional.empty()
                    //#endif
            );
            //#else
            //#if MC>=10800
            //#if MC>=11400
            //$$ DimensionType dimension = packet.func_212642_e();
            //#else
            //$$ int dimension = packet.getDimension();
            //#endif
            //$$ EnumDifficulty difficulty = packet.getDifficulty();
            //#if MC>=11400
            //$$ int maxPlayers = 0; // literally never used by vanilla (i.e. no accessor)
            //#else
            //$$ int maxPlayers = packet.getMaxPlayers();
            //#endif
            //$$ WorldType worldType = packet.getWorldType();
            //$$
            //#if MC>=10904
            //$$ p = new SPacketJoinGame(entId, GameType.SPECTATOR, false, dimension,
            //$$         difficulty, maxPlayers, worldType, false);
            //#else
            //$$ p = new S01PacketJoinGame(entId, GameType.SPECTATOR, false, dimension,
            //$$         difficulty, maxPlayers, worldType, false);
            //#endif
            //#else
            //$$ int dimension = packet.func_149194_f();
            //$$ EnumDifficulty difficulty = packet.func_149192_g();
            //$$ int maxPlayers = packet.func_149193_h();
            //$$ WorldType worldType = packet.func_149196_i();
            //$$
            //$$ p = new S01PacketJoinGame(entId, GameType.ADVENTURE, false, dimension,
            //$$         difficulty, maxPlayers, worldType);
            //#endif
            //#endif
        }

        if(p instanceof ClientboundRespawnPacket) {
            ClientboundRespawnPacket respawn = (ClientboundRespawnPacket) p;
            //#if MC>=11400
            p = new ClientboundRespawnPacket(
                    //#if MC>=11600
                    respawn.getDimensionType(),
                    //#endif
                    respawn.getDimension(),
                    //#if MC>=11500
                    respawn.getSeed(),
                    //#endif
                    //#if MC>=11600
                    GameType.SPECTATOR,
                    GameType.SPECTATOR,
                    respawn.isDebug(),
                    respawn.isFlat(),
                    respawn.isSkippable()
                    //#else
                    //$$ respawn.getGeneratorType(),
                    //$$ GameMode.SPECTATOR
                    //#endif
                    //#if MC>=11900
                    , java.util.Optional.empty()
                    //#endif
            );
            //#else
            //#if MC>=10809
            //$$ p = new SPacketRespawn(respawn.getDimensionID(),
            //$$         respawn.getDifficulty(), respawn.getWorldType(), GameType.SPECTATOR);
            //#else
            //$$ p = new S07PacketRespawn(respawn.func_149082_c(),
            //$$         respawn.func_149081_d(), respawn.func_149080_f(),
                    //#if MC>=10800
                    //$$ GameType.SPECTATOR);
                    //#else
                    //$$ GameType.ADVENTURE);
                    //#endif
            //#endif
            //#endif

            schedulePacketHandler(() -> allowMovement = true);
        }

        if(p instanceof ClientboundPlayerPositionPacket) {
            final ClientboundPlayerPositionPacket ppl = (ClientboundPlayerPositionPacket) p;
            if(!hasWorldLoaded) hasWorldLoaded = true;

            ReplayMod.instance.runLater(() -> {
                if (mc.screen instanceof ReceivingLevelScreen) {
                    // Close the world loading screen manually in case we swallow the packet
                    mc.setScreen(null);
                }
            });

            if(replayHandler.shouldSuppressCameraMovements()) return null;

            //#if MC>=10800
            //#if MC>=11400
            for (ClientboundPlayerPositionPacket.RelativeArgument relative : ppl.getRelativeArguments()) {
                if (relative == ClientboundPlayerPositionPacket.RelativeArgument.X
                        || relative == ClientboundPlayerPositionPacket.RelativeArgument.Y
                        || relative == ClientboundPlayerPositionPacket.RelativeArgument.Z) {
            //#else
            //#if MC>=10904
            //$$ for (SPacketPlayerPosLook.EnumFlags relative : ppl.getFlags()) {
            //#else
            //$$ for (Object relative : ppl.func_179834_f()) {
            //#endif
            //$$     if (relative == SPacketPlayerPosLook.EnumFlags.X
            //$$             || relative == SPacketPlayerPosLook.EnumFlags.Y
            //$$             || relative == SPacketPlayerPosLook.EnumFlags.Z) {
            //#endif
                    return null; // At least one of the coordinates is relative, so we don't care
                }
            }
            //#endif

            schedulePacketHandler(new Runnable() {
                @Override
                @SuppressWarnings("unchecked")
                public void run() {
                    // FIXME: world shouldn't ever be null at this point, now that we use the packet queue
                    //        probably fine to remove on the next non-patch version (don't want to break stuff now)
                    if (mc.level == null || !mc.isSameThread()) {
                        ReplayMod.instance.runLater(this);
                        return;
                    }

                    CameraEntity cent = replayHandler.getCameraEntity();
                    if (!allowMovement && !((Math.abs(cent.getX() - ppl.getX()) > TP_DISTANCE_LIMIT) ||
                            (Math.abs(cent.getZ() - ppl.getZ()) > TP_DISTANCE_LIMIT))) {
                        return;
                    } else {
                        allowMovement = false;
                    }
                    cent.setCameraPosition(ppl.getX(), ppl.getY(), ppl.getZ());
                    cent.setCameraRotation(ppl.getYRot(), ppl.getXRot(), cent.roll);
                }
            });

            return null;
        }

        if(p instanceof ClientboundGameEventPacket) {
            ClientboundGameEventPacket pg = (ClientboundGameEventPacket)p;
            // only allow the following packets:
            // 1 - End raining
            // 2 - Begin raining
            //
            // The following values are to control sky color (e.g. if thunderstorm)
            // 7 - Fade value
            // 8 - Fade time
            if (!Arrays.asList(
                    //#if MC>=11600
                    ClientboundGameEventPacket.START_RAINING,
                    ClientboundGameEventPacket.STOP_RAINING,
                    ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                    ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE
                    //#else
                    //$$ 1,
                    //$$ 2,
                    //$$ 7,
                    //$$ 8
                    //#endif
            ).contains(pg.getEvent())) {
                return null;
            }
        }

        //#if MC>=11901
        if (p instanceof ClientboundSystemChatPacket || p instanceof ClientboundPlayerChatPacket || p instanceof ClientboundPlayerChatHeaderPacket) {
        //#elseif MC>=11900
        //$$ if (p instanceof GameMessageS2CPacket || p instanceof ChatMessageS2CPacket) {
        //#else
        //$$ if (p instanceof GameMessageS2CPacket) {
        //#endif
            if (!ReplayModReplay.instance.getCore().getSettingsRegistry().get(Setting.SHOW_CHAT)) {
                return null;
            }
        }

        if (asyncMode) {
            return processPacketAsync(p);
        } else {
            Packet fp = p;
            mc.tell(() -> processPacketSync(fp));
            return p;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.ctx = ctx;
        super.channelActive(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        // The embedded channel's event loop will consider every thread to be in it and as such provides no
        // guarantees that only one thread is using the pipeline at any one time.
        // For reading the replay sender (either sync or async) is the only thread ever writing.
        // For writing it may very well happen that multiple threads want to use the pipline at the same time.
        // It's unclear whether the EmbeddedChannel is supposed to be thread-safe (the behavior of the event loop
        // does suggest that). However it seems like it either isn't (likely) or there is a race condition.
        // See: https://www.replaymod.com/forum/thread/1752#post8045 (https://paste.replaymod.com/lotacatuwo)
        // To work around this issue, we just outright drop all write/flush requests (they aren't needed anyway).
        // This still leaves channel handlers upstream with the threading issue but they all seem to cope well with it.
        promise.setSuccess();
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        // See write method above
    }

    /**
     * Returns the speed of the replay. 1 being normal speed, 0.5 half and 2 twice as fast.
     * If 0 is returned, the replay is paused.
     * @return speed multiplier
     */
    @Override
    public double getReplaySpeed() {
        if(!paused()) return replaySpeed;
        else return 0;
    }

    /**
     * Set the speed of the replay. 1 being normal speed, 0.5 half and 2 twice as fast.
     * The speed may not be set to 0 nor to negative values.
     * @param d Speed multiplier
     */
    @Override
    public void setReplaySpeed(final double d) {
        if (d != 0) {
            this.replaySpeed = d;
            this.realTimeStartSpeed = d;
            this.realTimeStart = System.currentTimeMillis() - (long) (lastTimeStamp / d);
        }
        TimerAccessor timer = (TimerAccessor) ((MinecraftAccessor) mc).getTimer();
        //#if MC>=11200
        timer.setTickLength(WrappedTimer.DEFAULT_MS_PER_TICK / (float) d);
        //#else
        //$$ timer.setTimerSpeed((float) d);
        //#endif
    }

    /////////////////////////////////////////////////////////
    //       Asynchronous packet processing                //
    /////////////////////////////////////////////////////////

    /**
     * Timestamp in milliseconds of when we started (or would have started when taking pauses and speed into account)
     * the playback of the replay.
     * Updated only when replay speed changes or on pause/unpause but definitely not on every packet to prevent gradual
     * drifting.
     */
    private long realTimeStart;

    /**
     * The replay speed used for {@link #realTimeStart}.
     * If the target speed differs from this one, the timestamp is recalculated.
     */
    private double realTimeStartSpeed;

    /**
     * There is no waiting performed until a packet with at least this timestamp is reached (but not yet sent).
     * If this is -1, then timing is normal.
     */
    private long desiredTimeStamp = -1;

    /**
     * Runnable which performs timed dispatching of packets from the input stream.
     */
    private Runnable asyncSender = new Runnable() {
        public void run() {
            try {
                while (ctx == null && !terminate) {
                    Thread.sleep(10);
                }
                REPLAY_LOOP:
                while (!terminate) {
                    synchronized (FullReplaySender.this) {
                        if (replayIn == null) {
                            replayIn = replayFile.getPacketData(getPacketTypeRegistry(true));
                        }
                        // Packet loop
                        while (true) {
                            try {
                                // When playback is paused and the world has loaded (we don't want any dirt-screens) we sleep
                                while (paused() && hasWorldLoaded) {
                                    // Unless we are going to terminate, restart or jump
                                    if (terminate || startFromBeginning || desiredTimeStamp != -1) {
                                        break;
                                    }
                                    Thread.sleep(10);
                                }

                                if (terminate) {
                                    break REPLAY_LOOP;
                                }

                                if (startFromBeginning) {
                                    // In case we need to restart from the beginning
                                    // break out of the loop sending all packets which will
                                    // cause the replay to be restarted by the outer loop
                                    break;
                                }

                                // Read the next packet if we don't already have one
                                if (nextPacket == null) {
                                    nextPacket = new PacketData(replayIn, loginPhase);
                                }

                                int nextTimeStamp = nextPacket.timestamp;

                                // If we aren't jumping and the world has already been loaded (no dirt-screens) then wait
                                // the required amount to get proper packet timing
                                if (!isHurrying() && hasWorldLoaded) {
                                    // Timestamp of when the next packet should be sent
                                    long expectedTime = realTimeStart + (long) (nextTimeStamp / replaySpeed);
                                    long now = System.currentTimeMillis();
                                    // If the packet should not yet be sent, wait a bit
                                    if (expectedTime > now) {
                                        Thread.sleep(expectedTime - now);
                                    }
                                }

                                // Process packet
                                channelRead(ctx, nextPacket.bytes);
                                nextPacket = null;

                                lastTimeStamp = nextTimeStamp;

                                // In case we finished jumping
                                // We need to check that we aren't planing to restart so we don't accidentally run this
                                // code before we actually restarted
                                if (isHurrying() && lastTimeStamp > desiredTimeStamp && !startFromBeginning) {
                                    desiredTimeStamp = -1;

                                    replayHandler.moveCameraToTargetPosition();

                                    // Pause after jumping (this will also reset realTimeStart accordingly)
                                    setReplaySpeed(0);
                                }
                            } catch (EOFException eof) {
                                // Reached end of file
                                // Pause the replay which will cause it to freeze before getting restarted
                                setReplaySpeed(0);
                                // Then wait until the user tells us to continue
                                while (paused() && hasWorldLoaded && desiredTimeStamp == -1 && !terminate) {
                                    Thread.sleep(10);
                                }

                                if (terminate) {
                                    break REPLAY_LOOP;
                                }
                                break;
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        // Restart the replay.
                        hasWorldLoaded = false;
                        lastTimeStamp = 0;
                        loginPhase = true;
                        startFromBeginning = false;
                        nextPacket = null;
                        realTimeStart = System.currentTimeMillis();
                        if (replayIn != null) {
                            replayIn.close();
                            replayIn = null;
                        }
                        ReplayMod.instance.runSync(replayHandler::restartedReplay);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Return whether this replay sender is currently rushing. When rushing, all packets are sent without waiting until
     * a specified timestamp is passed.
     * @return {@code true} if currently rushing, {@code false} otherwise
     */
    public boolean isHurrying() {
        return desiredTimeStamp != -1;
    }

    /**
     * Cancels the hurrying.
     */
    public void stopHurrying() {
        desiredTimeStamp = -1;
    }

    /**
     * Return the timestamp to which this replay sender is currently rushing. All packets with an lower or equal
     * timestamp will be sent out without any sleeping.
     * @return The timestamp in milliseconds since the start of the replay
     */
    public long getDesiredTimestamp() {
        return desiredTimeStamp;
    }

    /**
     * Jumps to the specified timestamp when in async mode by rushing all packets until one with a timestamp greater
     * than the specified timestamp is found.
     * If the timestamp has already passed, this causes the replay to restart and then rush all packets.
     * @param millis Timestamp in milliseconds since the start of the replay
     */
    @Override
    public void jumpToTime(int millis) {
        Preconditions.checkState(asyncMode, "Can only jump in async mode. Use sendPacketsTill(int) instead.");
        if(millis < lastTimeStamp && !isHurrying()) {
            startFromBeginning = true;
        }

        desiredTimeStamp = millis;
    }

    protected Packet processPacketAsync(Packet p) {
        //If hurrying, ignore some packets, except for short durations
        if(desiredTimeStamp - lastTimeStamp > 1000) {
            if(p instanceof ClientboundLevelParticlesPacket) return null;

            if(p instanceof ClientboundAddEntityPacket) {
                ClientboundAddEntityPacket pso = (ClientboundAddEntityPacket)p;
                //#if MC>=11400
                if (pso.getType() == EntityType.FIREWORK_ROCKET) return null;
                //#else
                //$$ int type = pso.getType();
                //$$ if(type == 76) { // Firework rocket
                //$$     return null;
                //$$ }
                //#endif
            }
        }
        return p;
    }

    /////////////////////////////////////////////////////////
    //        Synchronous packet processing                //
    /////////////////////////////////////////////////////////

    // Even in sync mode, we send from another thread because mods may rely on that
    private final ExecutorService syncSender = Executors.newSingleThreadExecutor(runnable ->
            new Thread(runnable, "replaymod-sync-sender"));

    /**
     * Sends all packets until the specified timestamp is reached (inclusive).
     * If the timestamp is smaller than the last packet sent, the replay is restarted from the beginning.
     * @param timestamp The timestamp in milliseconds since the beginning of this replay
     */
    @Override
    public void sendPacketsTill(int timestamp) {
        Preconditions.checkState(!asyncMode, "This method cannot be used in async mode. Use jumpToTime(int) instead.");

        // Submit our target to the sender thread and track its progress
        AtomicBoolean doneSending = new AtomicBoolean();
        syncSender.submit(() -> {
            try {
                doSendPacketsTill(timestamp);
            } finally {
                doneSending.set(true);
            }
        });

        // Drain the task queue while we are sending (in case a mod blocks the io thread waiting for the main thread)
        while (!doneSending.get()) {
            executeTaskQueue();

            // Wait until the sender thread has made progress
            try {
                //noinspection BusyWait
                Thread.sleep(0, 100_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Everything has been sent, drain the queue one last time
        executeTaskQueue();
    }

    private void doSendPacketsTill(int timestamp) {
        try {
            while (ctx == null && !terminate) { // Make sure channel is ready
                Thread.sleep(10);
            }

            synchronized (this) {
                if (timestamp == lastTimeStamp) { // Do nothing if we're already there
                    return;
                }
                if (timestamp < lastTimeStamp) { // Restart the replay if we need to go backwards in time
                    hasWorldLoaded = false;
                    lastTimeStamp = 0;
                    if (replayIn != null) {
                        replayIn.close();
                        replayIn = null;
                    }
                    loginPhase = true;
                    startFromBeginning = false;
                    nextPacket = null;
                    ReplayMod.instance.runSync(replayHandler::restartedReplay);
                }

                if (replayIn == null) {
                    replayIn = replayFile.getPacketData(getPacketTypeRegistry(true));
                }

                while (true) { // Send packets
                    try {
                        PacketData pd;
                        if (nextPacket != null) {
                            // If there is still a packet left from before, use it first
                            pd = nextPacket;
                            nextPacket = null;
                        } else {
                            // Otherwise read one from the input stream
                            pd = new PacketData(replayIn, loginPhase);
                        }

                        int nextTimeStamp = pd.timestamp;
                        if (nextTimeStamp > timestamp) {
                            // We are done sending all packets
                            nextPacket = pd;
                            break;
                        }

                        // Process packet
                        channelRead(ctx, pd.bytes);
                    } catch (EOFException eof) {
                        // Shit! We hit the end before finishing our job! What shall we do now?
                        // well, let's just pretend we're done...
                        replayIn = null;
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // This might be required if we change to async mode anytime soon
                realTimeStart = System.currentTimeMillis() - (long) (timestamp / replaySpeed);
                lastTimeStamp = timestamp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void executeTaskQueue() {
        //#if MC>=11400
        ((MinecraftMethodAccessor) mc).replayModExecuteTaskQueue();
        //#else
        //$$ java.util.Queue<java.util.concurrent.FutureTask<?>> scheduledTasks = ((MinecraftAccessor) mc).getScheduledTasks();
        //$$
        //$$ // Live-lock detection: if we already hold the lock, then the sender thread will never be able to queue its
        //$$ // tasks
        //$$ if (Thread.holdsLock(scheduledTasks)) {
        //$$     throw new IllegalStateException("Task queue already locked. " +
        //$$             "You may want to use `Scheduler.runLaterWithoutLock` to run while the lock is not taken.");
        //$$ }
        //$$
        //$$ //noinspection SynchronizationOnLocalVariableOrMethodParameter
        //$$ synchronized (scheduledTasks) {
        //$$     while (!scheduledTasks.isEmpty()) {
        //$$         scheduledTasks.poll().run();
        //$$     }
        //$$ }
        //#endif
        ReplayMod.instance.runTasks();
    }

    /**
     * Runs the given runnable on the main thread as if it was a packet handler.
     * Note that the packet handler queue has different behavior than the standard ReplayMod queue.
     */
    private void schedulePacketHandler(Runnable runnable) {
        if (mc.isSameThread()) {
            runnable.run();
        } else {
            //#if MC>=11400
            mc.execute(runnable);
            //#else
            //$$ mc.addScheduledTask(runnable);
            //#endif
        }
    }

    protected void processPacketSync(Packet p) {
        //#if MC>=10904
        if (p instanceof ClientboundForgetLevelChunkPacket) {
            ClientboundForgetLevelChunkPacket packet = (ClientboundForgetLevelChunkPacket) p;
            int x = packet.getX();
            int z = packet.getZ();
        //#else
        //$$ if (p instanceof S21PacketChunkData && ((S21PacketChunkData) p).getExtractedSize() == 0) {
        //$$     S21PacketChunkData packet = (S21PacketChunkData) p;
        //$$     int x = packet.getChunkX();
        //$$     int z = packet.getChunkZ();
        //#endif
            // If the chunk is getting unloaded, we will have to forcefully update the position of all entities
            // within. Otherwise, if there wasn't a game tick recently, there may be entities that have moved
            // out of the chunk by now but are still registered in it. If we do not update those, they will get
            // unloaded even though they shouldn't.
            // Note: This is only half of the truth. Entities may be removed by chunk-unloading, see else-case below.
            // To make things worse, it seems like players were never supposed to be unloaded this way because
            // they will remain glitched in the World#playerEntities list.
            // 1.14+: The update issue remains but only for non-players and the unloading list bug appears to have been
            //        fixed (chunk unloading no longer removes the entities).
            // Get the chunk that will be unloaded
            //#if MC>=11400
            ClientLevel world = mc.level;
            ChunkSource chunkProvider = world.getChunkSource();
            LevelChunk chunk = chunkProvider.getChunkNow(x, z
                    //#if MC<11500
                    //$$ , false
                    //#endif
            );
            if (chunk != null) {
            //#else
            //$$ World world = mc.world;
            //$$ IChunkProvider chunkProvider = world.getChunkProvider();
            //$$ Chunk chunk = chunkProvider.provideChunk(x, z);
            //$$ if (!chunk.isEmpty()) {
            //#endif
                List<Entity> entitiesInChunk = new ArrayList<>();
                // Gather all entities in that chunk
                //#if MC>=11700
                for (Entity entity : mc.level.entitiesForRendering()) {
                    if (entity.chunkPosition().equals(chunk.getPos())) {
                        entitiesInChunk.add(entity);
                    }
                }
                //#else
                //$$ for (Collection<Entity> entityList : chunk.getEntitySectionArray()) {
                //$$     entitiesInChunk.addAll(entityList);
                //$$ }
                //#endif
                for (Entity entity : entitiesInChunk) {
                    // Skip interpolation of position updates coming from server
                    // (See: newX in EntityLivingBase or otherPlayerMPX in EntityOtherPlayerMP)
                    forcePositionForVehicleAndSelf(entity);

                    // Check whether the entity has left the chunk
                    //#if MC>=11700
                    // This is now handled automatically in Entity.setPos (called from tick())
                    //#elseif MC>=11404
                    //$$ int chunkX = MathHelper.floor(entity.getX() / 16);
                    //$$ int chunkY = MathHelper.floor(entity.getY() / 16);
                    //$$ int chunkZ = MathHelper.floor(entity.getZ() / 16);
                    //$$ if (entity.chunkX != chunkX || entity.chunkY != chunkY || entity.chunkZ != chunkZ) {
                    //$$     if (entity.updateNeeded) {
                    //$$         // Entity has left the chunk
                    //$$         chunk.remove(entity, entity.chunkY);
                    //$$     }
                    //$$     WorldChunk newChunk = chunkProvider.getWorldChunk(chunkX, chunkZ
                                //#if MC<11500
                                //$$ , false
                                //#endif
                    //$$     );
                    //$$     if (newChunk != null) {
                    //$$         newChunk.addEntity(entity);
                    //$$     } else {
                    //$$         // Entity has left all loaded chunks
                    //$$         entity.updateNeeded = false;
                    //$$     }
                    //$$ }
                    //#else
                    //$$ int chunkX = MathHelper.floor(entity.posX / 16);
                    //$$ int chunkZ = MathHelper.floor(entity.posZ / 16);
                    //$$ if (entity.chunkCoordX != chunkX || entity.chunkCoordZ != chunkZ) {
                    //$$     // Entity has left the chunk
                    //$$     chunk.removeEntityAtIndex(entity, entity.chunkCoordY);
                        //#if MC>=10904
                        //$$ Chunk newChunk = chunkProvider.getLoadedChunk(chunkX, chunkZ);
                        //#else
                        //$$ Chunk newChunk = chunkProvider.chunkExists(chunkX, chunkZ)
                        //$$         ? chunkProvider.provideChunk(chunkX, chunkZ) : null;
                        //#endif
                    //$$     if (newChunk != null) {
                    //$$         newChunk.addEntity(entity);
                    //$$     } else {
                    //$$         // Entity has left all loaded chunks
                    //$$         entity.addedToChunk = false;
                    //$$     }
                    //$$ } else {
                    //$$     // When entities remain in a chunk that's to be unloaded, they'll only be added to a unload
                    //$$     // queue and remain loaded as before until the next tick (which during jumping is way off).
                    //$$     // So, if they are re-spawned with the same entity id, MC actually cleans up the old entity and
                    //$$     // then adds the new one but leaves the unload queue as is.
                    //$$     // Finally, on the next tick the legitimate entity will be unloaded because it's part of the
                    //$$     // unload queue (entities .equals based purely on their id). However, the old entity object
                    //$$     // is used to determine the chunk the entity is removed from and in this case that'll allow the
                    //$$     // legitimate entity to remain registered in a loaded chunk, causing them to still be rendered.
                    //$$     //
                    //$$     // The usual removal-due-to-chunk-unload process will, without touching the entityList, call
                    //$$     // onEntityRemoved. In that method WorldClient checks to see whether the entity is still in the
                    //$$     // entityList (which it is) and then adds it to the entitySpawnQueue.
                    //$$     // As the final result the entity will remain loaded.
                    //$$     // To get the same result without ticking, we just remove the entity from the to-be-unloaded
                    //$$     // chunk but keep it loaded otherwise. They won't be rendered because they're not part of any
                    //$$     // chunk and will be removed properly if the server decides to re-spawn the entity.
                    //$$     chunk.removeEntityAtIndex(entity, entity.chunkCoordY);
                    //$$     entity.addedToChunk = false;
                    //$$ }
                    //#endif
                }
            }
        }
    }

    private void forcePositionForVehicleAndSelf(Entity entity) {
        Entity vehicle = entity.getVehicle();
        if (vehicle != null) {
            forcePositionForVehicleAndSelf(vehicle);
        }

        // Skip interpolation of position updates coming from server
        // (See: newX in EntityLivingBase or otherPlayerMPX in EntityOtherPlayerMP)
        int ticks = 0;
        Vec3 prevPos;
        do {
            prevPos = entity.position();
            if (vehicle != null) {
                entity.rideTick();
            } else {
                entity.tick();
            }
        } while (prevPos.distanceToSqr(entity.position()) > 0.0001 && ticks++ < 100);
    }

    private static final class PacketData {
        private static final com.github.steveice10.netty.buffer.ByteBuf byteBuf = com.github.steveice10.netty.buffer.Unpooled.buffer();
        private static final NetOutput netOutput = new ByteBufNetOutput(byteBuf);

        private final int timestamp;
        private final byte[] bytes;

        PacketData(ReplayInputStream in, boolean loginPhase) throws IOException {
            if (ReplayMod.isMinimalMode()) {
                // Minimal mode, we can only read our exact protocol version and cannot use ReplayStudio
                timestamp = readInt(in);
                int length = readInt(in);
                if (timestamp == -1 || length == -1) {
                    throw new EOFException();
                }
                bytes = new byte[length];
                IOUtils.readFully(in, bytes);
            } else {
                com.replaymod.replaystudio.PacketData data = in.readPacket();
                if (data == null) {
                    throw new EOFException();
                }
                timestamp = (int) data.getTime();
                com.replaymod.replaystudio.protocol.Packet packet = data.getPacket();
                // We need to re-encode ReplayStudio packets, so we can later decode them as NMS packets
                // The main reason we aren't reading them as NMS packets is that we want ReplayStudio to be able
                // to apply ViaVersion (and potentially other magic) to it.
                synchronized (byteBuf) {
                    byteBuf.markReaderIndex(); // Mark the current reader and writer index (should be at start)
                    byteBuf.markWriterIndex();

                    netOutput.writeVarInt(packet.getId());
                    int idSize = byteBuf.readableBytes();
                    int contentSize = packet.getBuf().readableBytes();
                    bytes = new byte[idSize + contentSize]; // Create bytes array of sufficient size
                    byteBuf.readBytes(bytes, 0, idSize);
                    packet.getBuf().readBytes(bytes, idSize, contentSize);

                    byteBuf.resetReaderIndex(); // Reset reader & writer index for next use
                    byteBuf.resetWriterIndex();
                }
                packet.getBuf().release();
            }
        }
    }
}
