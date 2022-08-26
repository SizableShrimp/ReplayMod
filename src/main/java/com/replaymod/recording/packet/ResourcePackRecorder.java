package com.replaymod.recording.packet;

import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.replaymod.replaystudio.replay.ReplayFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//#if MC>=11900
import java.net.MalformedURLException;
import java.net.URL;
//#endif
//#if MC>=10800
import de.johni0702.minecraft.gui.utils.Consumer;
//#else
//$$ import net.minecraft.client.gui.GuiScreenWorking;
//$$ import net.minecraft.util.HttpUtil;
//#endif
//#if MC>=10800
//#if MC>=11400
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.game.ServerboundResourcePackPacket.Action;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static com.replaymod.core.versions.MCVer.*;

/**
 * Records resource packs and handles incoming resource pack packets during recording.
 */
public class ResourcePackRecorder {
    private static final Logger logger = LogManager.getLogger();
    private static final Minecraft mc = getMinecraft();

    private final ReplayFile replayFile;

    private int nextRequestId;

    public ResourcePackRecorder(ReplayFile replayFile) {
        this.replayFile = replayFile;
    }

    public void recordResourcePack(File file, int requestId) {
        try {
            // Read in resource pack file
            byte[] bytes = Files.toByteArray(file);
            // Check whether it is already known
            String hash = Hashing.sha1().hashBytes(bytes).toString();
            boolean doWrite = false; // Whether we are the first and have to write it
            synchronized (replayFile) { // Need to read, modify and write the resource pack index atomically
                Map<Integer, String> index = replayFile.getResourcePackIndex();
                if (index == null) {
                    index = new HashMap<>();
                }
                if (!index.containsValue(hash)) {
                    // Hash is unknown, we have to write the resource pack ourselves
                    doWrite = true;
                }
                // Save this request
                index.put(requestId, hash);
                replayFile.writeResourcePackIndex(index);
            }
            if (doWrite) {
                try (OutputStream out = replayFile.writeResourcePack(hash)) {
                    out.write(bytes);
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to save resource pack.", e);
        }
    }

    //#if MC>=10800
    public ServerboundResourcePackPacket makeStatusPacket(String hash, Action action) {
        //#if MC>=11002
        return new ServerboundResourcePackPacket(action);
        //#else
        //$$ return new CPacketResourcePackStatus(hash, action);
        //#endif
    }


    public synchronized ClientboundResourcePackPacket handleResourcePack(Connection netManager, ClientboundResourcePackPacket packet) {
        final int requestId = nextRequestId++;
        final String url = packet.getUrl();
        final String hash = packet.getHash();

        if (url.startsWith("level://")) {
            String levelName = url.substring("level://".length());
            File savesDir = new File(mc.gameDirectory, "saves");
            final File levelDir = new File(savesDir, levelName);

            if (levelDir.isFile()) {
                netManager.send(makeStatusPacket(hash, Action.ACCEPTED));
                addCallback(setServerResourcePack(levelDir), result -> {
                    recordResourcePack(levelDir, requestId);
                    netManager.send(makeStatusPacket(hash, Action.SUCCESSFULLY_LOADED));
                }, throwable -> {
                    netManager.send(makeStatusPacket(hash, Action.FAILED_DOWNLOAD));
                });
            } else {
                netManager.send(makeStatusPacket(hash, Action.FAILED_DOWNLOAD));
            }
        } else {
            final ServerData serverData = mc.getCurrentServer();
            if (serverData != null && serverData.getResourcePackStatus() == ServerData.ServerPackStatus.ENABLED) {
                netManager.send(makeStatusPacket(hash, Action.ACCEPTED));
                downloadResourcePackFuture(netManager, requestId, url, hash);
            } else if (serverData != null && serverData.getResourcePackStatus() != ServerData.ServerPackStatus.PROMPT) {
                netManager.send(makeStatusPacket(hash, Action.DECLINED));
            } else {
                // Lambdas MUST NOT be used with methods that need re-obfuscation in FG prior to 2.2 (will result in AbstractMethodError)
                //#if MC>=11400
                mc.execute(() -> mc.setScreen(new ConfirmScreen(result -> {
                //#else
                //$$ //noinspection Convert2Lambda
                //$$ mc.addScheduledTask(() -> mc.displayGuiScreen(new GuiYesNo(new GuiYesNoCallback() {
                //$$     @Override
                //$$     public void confirmClicked(boolean result, int id) {
                //#endif
                        if (serverData != null) {
                            serverData.setResourcePackStatus(result ? ServerData.ServerPackStatus.ENABLED : ServerData.ServerPackStatus.DISABLED);
                        }
                        if (result) {
                            netManager.send(makeStatusPacket(hash, Action.ACCEPTED));
                            downloadResourcePackFuture(netManager, requestId, url, hash);
                        } else {
                            netManager.send(makeStatusPacket(hash, Action.DECLINED));
                        }

                        ServerList.saveSingleServer(serverData);
                        mc.setScreen(null);
                    }
                //#if MC>=11400
                , net.minecraft.network.chat.Component.translatable("multiplayer.texturePrompt.line1"), net.minecraft.network.chat.Component.translatable("multiplayer.texturePrompt.line2"))));
                //#else
                //$$ }, I18n.format("multiplayer.texturePrompt.line1"), I18n.format("multiplayer.texturePrompt.line2"), 0)));
                //#endif
            }
        }

        return new ClientboundResourcePackPacket(
                "replay://" + requestId, ""
                //#if MC>=11700
                , packet.isRequired(), packet.getPrompt()
                //#endif
        );
    }

    private void downloadResourcePackFuture(Connection connection, int requestId, String url, final String hash) {
        addCallback(downloadResourcePack(requestId, url, hash),
                result -> connection.send(makeStatusPacket(hash, Action.SUCCESSFULLY_LOADED)),
                throwable -> connection.send(makeStatusPacket(hash, Action.FAILED_DOWNLOAD)));
    }

    private
    //#if MC>=11400
    CompletableFuture<?>
    //#else
    //$$ ListenableFuture<?>
    //#endif
    downloadResourcePack(final int requestId, String url, String hash) {
        ClientPackSource packFinder = mc.getClientPackSource();
        ((IDownloadingPackFinder) packFinder).setRequestCallback(file -> recordResourcePack(file, requestId));
        //#if MC>=11900
        try {
            URL theUrl = new URL(url);
            String protocol = theUrl.getProtocol();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new MalformedURLException("Unsupported protocol.");
            }
            return packFinder.downloadAndSelectResourcePack(theUrl, hash, true);
        } catch (MalformedURLException e) {
            return CompletableFuture.failedFuture(e);
        }
        //#elseif MC>=11700
        //$$ return packFinder.download(url, hash, true);
        //#else
        //$$ return packFinder.download(url, hash);
        //#endif
    }

    public interface IDownloadingPackFinder {
        void setRequestCallback(Consumer<File> callback);
    }
    //#else
    //$$ public synchronized S3FPacketCustomPayload handleResourcePack(S3FPacketCustomPayload packet) {
    //$$     final int requestId = nextRequestId++;
    //$$     final String url = new String(packet.func_149168_d(), Charsets.UTF_8);
    //$$
    //$$     ServerData serverData = mc.getCurrentServerData();
    //$$     ServerResourceMode resourceMode = serverData == null ? ServerResourceMode.PROMPT : serverData.getResourceMode();
    //$$     if (resourceMode == ServerResourceMode.ENABLED) {
    //$$         downloadResourcePack(requestId, url);
    //$$         mc.getResourcePackRepository().obtainResourcePack(url);
    //$$     } else if (resourceMode == ServerResourceMode.PROMPT) {
    //$$         // Lambdas MUST NOT be used with methods that need re-obfuscation in FG prior to 2.2 (will result in AbstractMethodError)
    //$$         //noinspection Convert2Lambda
    //$$         mc.displayGuiScreen(new GuiYesNo(new GuiYesNoCallback() {
    //$$             @Override
    //$$             public void confirmClicked(boolean result, int id) {
    //$$                 if (serverData != null) {
    //$$                     serverData.setResourceMode(ServerResourceMode.ENABLED);
    //$$                     ServerList.func_147414_b(serverData);
    //$$                 }
    //$$
    //$$                 mc.displayGuiScreen(null);
    //$$
    //$$                 if (result) {
    //$$                     downloadResourcePack(requestId, url);
    //$$                 }
    //$$             }
    //$$         }, I18n.format("multiplayer.texturePrompt.line1"), I18n.format("multiplayer.texturePrompt.line2"), 0));
    //$$     }
    //$$
    //$$     return new S3FPacketCustomPayload(packet.func_149169_c(), ("replay://" + requestId).getBytes(Charsets.UTF_8));
    //$$ }
    //$$
    //$$ private void downloadResourcePack(final int requestId, String url) {
    //$$     final ResourcePackRepository repo = mc.getResourcePackRepository();
    //$$     final ResourcePackRepositoryAccessor acc = (ResourcePackRepositoryAccessor) repo;
    //$$
    //$$     String fileName = url.substring(url.lastIndexOf("/") + 1);
    //$$
    //$$     if (fileName.contains("?")) {
    //$$         fileName = fileName.substring(0, fileName.indexOf("?"));
    //$$     }
    //$$
    //$$     if (!fileName.endsWith(".zip")) {
    //$$         return;
    //$$     }
    //$$
    //$$     fileName = fileName.replaceAll("\\W", "");
    //$$
    //$$     File file = new File(acc.getCacheDir(), fileName);
    //$$
    //$$     HashMap<String, String> hashmap = new HashMap<>();
    //$$     hashmap.put("X-Minecraft-Username", mc.getSession().getUsername());
    //$$     hashmap.put("X-Minecraft-UUID", mc.getSession().getPlayerID());
    //$$     hashmap.put("X-Minecraft-Version", "1.7.10");
    //$$
    //$$     GuiScreenWorking guiScreen = new GuiScreenWorking();
    //$$     Minecraft.getMinecraft().displayGuiScreen(guiScreen);
    //$$     repo.func_148529_f();
    //$$     acc.setActive(true);
    //$$     // Lambdas MUST NOT be used with methods that need re-obfuscation in FG prior to 2.2 (will result in AbstractMethodError)
    //$$     //noinspection Convert2Lambda
    //$$     HttpUtil.downloadResourcePack(file, url, new HttpUtil.DownloadListener() {
    //$$         public void onDownloadComplete(File file) {
    //$$             if (acc.isActive()) {
    //$$                 acc.setActive(false);
    //$$                 acc.setPack(new FileResourcePack(file));
    //$$                 Minecraft.getMinecraft().scheduleResourcesRefresh();
    //$$                 recordResourcePack(file, requestId);
    //$$             }
    //$$         }
    //$$     }, hashmap, 50*1024*1024, guiScreen, Minecraft.getMinecraft().getProxy());
    //$$ }
    //#endif

}
