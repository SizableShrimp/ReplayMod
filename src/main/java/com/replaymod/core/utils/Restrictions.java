package com.replaymod.core.utils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;

//#if MC<=10710
//$$ import io.netty.buffer.Unpooled;
//#endif

/**
 * Restrictions set by the server,
 * @see <a href="https://gist.github.com/Johni0702/2547c463e51f65f312cb">Replay Restrictions Gist</a>
 */
public class Restrictions {
    //#if MC>=11400
    public static final ResourceLocation PLUGIN_CHANNEL = new ResourceLocation("replaymod", "restrict");
    //#else
    //$$ public static final String PLUGIN_CHANNEL = "Replay|Restrict";
    //#endif
    private boolean noXray;
    private boolean noNoclip;
    private boolean onlyFirstPerson;
    private boolean onlyRecordingPlayer;

    public String handle(ClientboundCustomPayloadPacket packet) {
        //#if MC>=10800
        FriendlyByteBuf buffer = packet.getData();
        //#else
        //$$ PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(packet.func_149168_d()));
        //#endif
        while (buffer.isReadable()) {
            String name = buffer.readUtf(64);
            boolean active = buffer.readBoolean();
//            if ("no_xray".equals(name)) {
//                noXray = active;
//            } else if ("no_noclip".equals(name)) {
//                noNoclip = active;
//            } else if ("only_first_person".equals(name)) {
//                onlyFirstPerson = active;
//            } else if ("only_recording_player".equals(name)) {
//                onlyRecordingPlayer = active;
//            } else {
                return name;
//            }
        }
        return null;
    }

    public boolean isNoXray() {
        return noXray;
    }

    public boolean isNoNoclip() {
        return noNoclip;
    }

    public boolean isOnlyFirstPerson() {
        return onlyFirstPerson;
    }

    public boolean isOnlyRecordingPlayer() {
        return onlyRecordingPlayer;
    }
}
