package com.replaymod.replay;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.replaymod.core.ReplayMod;
import de.johni0702.minecraft.gui.versions.Image;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import static com.replaymod.core.versions.MCVer.popMatrix;
import static com.replaymod.core.versions.MCVer.pushMatrix;

//#if MC<11400
//$$ import com.google.common.io.Files;
//$$ import org.apache.commons.io.FileUtils;
//$$ import java.io.File;
//#endif

public class NoGuiScreenshot {
    private final Image image;
    private final int width;
    private final int height;

    private NoGuiScreenshot(Image image, int width, int height) {
        this.image = image;
        this.width = width;
        this.height = height;
    }

    public Image getImage() {
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static ListenableFuture<NoGuiScreenshot> take(final Minecraft mc, final int width, final int height) {
        final SettableFuture<NoGuiScreenshot> future = SettableFuture.create();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (future.isCancelled()) {
                    return;
                }

                int frameWidth = mc.getWindow().getWidth();
                int frameHeight = mc.getWindow().getHeight();

                final boolean guiHidden = mc.options.hideGui;
                try {
                    mc.options.hideGui = true;

                    // Render frame without GUI
                    pushMatrix();
                    RenderSystem.clear(
                            16640
                            //#if MC>=11400
                            , true
                            //#endif
                    );
                    mc.getMainRenderTarget().bindWrite(true);
                    RenderSystem.enableTexture();

                    float tickDelta = mc.getFrameTime();
                    //#if MC>=11500
                    mc.gameRenderer.renderLevel(tickDelta, System.nanoTime(), new PoseStack());
                    //#else
                    //#if MC>=11400
                    //$$ mc.gameRenderer.renderWorld(tickDelta, System.nanoTime());
                    //#else
                    //#if MC>=10809
                    //$$ mc.entityRenderer.updateCameraAndRender(tickDelta, System.nanoTime());
                    //#else
                    //$$ mc.entityRenderer.updateCameraAndRender(tickDelta);
                    //#endif
                    //#endif
                    //#endif

                    mc.getMainRenderTarget().unbindWrite();
                    popMatrix();
                    pushMatrix();
                    mc.getMainRenderTarget().blitToScreen(frameWidth, frameHeight);
                    popMatrix();
                } catch (Throwable t) {
                    future.setException(t);
                    return;
                } finally {
                    // Reset GUI settings
                    mc.options.hideGui = guiHidden;
                }

                // The frame without GUI has been rendered
                // Read it, create the screenshot and finish the future
                try {
                    //#if MC>=11400
                    Image image = new Image(Screenshot.takeScreenshot(
                            //#if MC<11701
                            //$$ frameWidth, frameHeight,
                            //#endif
                            mc.getMainRenderTarget()
                    ));
                    //#else
                    //$$ // We're using Minecraft's ScreenShotHelper even though it writes the screenshot to
                    //$$ // disk for better maintainability
                    //$$ File tmpFolder = Files.createTempDir();
                    //$$ Image image;
                    //$$ try {
                    //$$     ScreenShotHelper.saveScreenshot(tmpFolder, "tmp", frameWidth, frameHeight, mc.getFramebuffer());
                    //$$     File screenshotFile = new File(tmpFolder, "screenshots/tmp");
                    //$$     image = Image.read(screenshotFile.toPath());
                    //$$ } finally {
                    //$$     FileUtils.deleteQuietly(tmpFolder);
                    //$$ }
                    //#endif
                    int imageWidth = image.getWidth();
                    int imageHeight = image.getHeight();

                    // Scale & crop
                    float scaleFactor = Math.max((float) width / imageWidth, (float) height / imageHeight);
                    int croppedWidth = Math.min(Math.max(0, (int) (width / scaleFactor)), imageWidth);
                    int croppedHeight = Math.min(Math.max(0, (int) (height / scaleFactor)), imageHeight);
                    int offsetX = (imageWidth - croppedWidth) / 2;
                    int offsetY = (imageHeight - croppedHeight) / 2;
                    image = image.scaledSubRect(offsetX, offsetY, croppedWidth, croppedHeight, width, height);

                    // Finish
                    future.set(new NoGuiScreenshot(image, width, height));
                } catch (Throwable t) {
                    future.setException(t);
                }
            }
        };

        // Make sure we are not somewhere in the middle of the rendering process but always at the beginning
        // of the game loop. We cannot use the addScheduledTask method as it'll run the task if called
        // from the minecraft thread which is exactly what we want to avoid.
        ReplayMod.instance.runLater(runnable);
        return future;
    }
}
