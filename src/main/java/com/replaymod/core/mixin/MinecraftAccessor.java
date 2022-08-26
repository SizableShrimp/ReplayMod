package com.replaymod.core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Queue;

//#if MC>=11800
import java.util.function.Supplier;
//#endif
import net.minecraft.CrashReport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Timer;
import net.minecraft.network.Connection;
//#if MC>=11400
import java.util.concurrent.CompletableFuture;
//#endif

//#if MC<11400
//$$ import java.util.concurrent.FutureTask;
//#endif

//#if MC<11400
//$$ import net.minecraft.client.resources.IResourcePack;
//$$ import java.util.List;
//#endif

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("renderTickCounter")
    Timer getTimer();
    @Accessor("renderTickCounter")
    //#if MC>=11200
    @Mutable
    //#endif
    void setTimer(Timer value);

    //#if MC>=11400
    @Accessor
    CompletableFuture<Void> getResourceReloadFuture();
    @Accessor
    void setResourceReloadFuture(CompletableFuture<Void> value);
    //#endif

    //#if MC>=11400
    @Accessor
    Queue<Runnable> getRenderTaskQueue();
    //#else
    //$$ @Accessor
    //$$ Queue<FutureTask<?>> getScheduledTasks();
    //#endif

    @Accessor("crashReportSupplier")
    //#if MC>=11800
    Supplier<CrashReport> getCrashReporter();
    //#else
    //$$ CrashReport getCrashReporter();
    //#endif

    //#if MC<11400
    //$$ @Accessor
    //$$ List<IResourcePack> getDefaultResourcePacks();
    //#endif

    //#if MC>=11400
    @Accessor("integratedServerConnection")
    void setConnection(Connection connection);
    //#endif
}
