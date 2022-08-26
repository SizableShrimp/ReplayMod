package com.replaymod.replay.camera;

import com.replaymod.replay.ReplayModReplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;

//#if MC>=11400
//#else
//$$ import org.lwjgl.input.Mouse;
//#endif

import java.util.Arrays;

import static com.replaymod.core.versions.MCVer.*;

public class SpectatorCameraController implements CameraController {
    private final CameraEntity camera;

    public SpectatorCameraController(CameraEntity camera) {
        this.camera = camera;
    }

    @Override
    public void update(float partialTicksPassed) {
        MinecraftClient mc = getMinecraft();
        if (mc.options.sneakKey.wasPressed()) {
            ReplayModReplay.instance.getReplayHandler().spectateCamera();
        }

        // Soak up all remaining key presses
        for (KeyBinding binding : Arrays.asList(mc.options.attackKey, mc.options.useKey,
                mc.options.jumpKey, mc.options.sneakKey, mc.options.forwardKey,
                mc.options.backKey, mc.options.leftKey, mc.options.rightKey)) {
            //noinspection StatementWithEmptyBody
            while (binding.wasPressed());
        }

        // Prevent mouse movement
        //#if MC>=11400
        // No longer needed
        //#else
        //$$ Mouse.updateCursor();
        //#endif

        // Always make sure the camera is in the exact same spot as the spectated entity
        // This is necessary as some rendering code for the hand doesn't respect the view entity
        // and always uses mc.thePlayer
        Entity view = mc.getCameraEntity();
        if (view != null && view != camera) {
            camera.setCameraPosRot(mc.getCameraEntity());
        }
    }

    @Override
    public void increaseSpeed() {

    }

    @Override
    public void decreaseSpeed() {

    }
}
