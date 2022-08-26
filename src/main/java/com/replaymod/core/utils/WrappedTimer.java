package com.replaymod.core.utils;

import com.replaymod.core.mixin.TimerAccessor;
import net.minecraft.client.Timer;

public class WrappedTimer extends Timer {
    public static final float DEFAULT_MS_PER_TICK = 1000 / 20;

    protected final Timer wrapped;

    public WrappedTimer(Timer wrapped) {
        //#if MC>=11400
        super(0, 0);
        //#else
        //$$ super(0);
        //#endif
        this.wrapped = wrapped;
        copy(wrapped, this);
    }

    @Override
    public
    //#if MC>=11600
    int
    //#else
    //$$ void
    //#endif
    advanceTime(
            //#if MC>=11400
            long sysClock
            //#endif
    ) {
        copy(this, wrapped);
        try {
            //#if MC>=11600
            return
            //#endif
            wrapped.advanceTime(
                    //#if MC>=11400
                    sysClock
                    //#endif
            );
        } finally {
            copy(wrapped, this);
        }
    }

    protected void copy(Timer from, Timer to) {
        TimerAccessor fromA = (TimerAccessor) from;
        TimerAccessor toA = (TimerAccessor) to;

        //#if MC<11600
        //$$ to.ticksThisFrame = from.ticksThisFrame;
        //#endif
        to.partialTick = from.partialTick;
        toA.setLastSyncSysClock(fromA.getLastSyncSysClock());
        to.tickDelta = from.tickDelta;
        //#if MC>=11200
        toA.setTickLength(fromA.getTickLength());
        //#else
        //$$ toA.setTicksPerSecond(fromA.getTicksPerSecond());
        //$$ toA.setLastHRTime(fromA.getLastHRTime());
        //$$ toA.setTimerSpeed(fromA.getTimerSpeed());
        //$$ toA.setLastSyncHRClock(fromA.getLastSyncHRClock());
        //$$ toA.setCounter(fromA.getCounter());
        //$$ toA.setTimeSyncAdjustment(fromA.getTimeSyncAdjustment());
        //#endif
    }
}
