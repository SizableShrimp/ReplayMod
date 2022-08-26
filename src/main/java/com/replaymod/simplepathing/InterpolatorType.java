package com.replaymod.simplepathing;

import com.replaymod.replaystudio.pathing.interpolation.CatmullRomSplineInterpolator;
import com.replaymod.replaystudio.pathing.interpolation.CubicSplineInterpolator;
import com.replaymod.replaystudio.pathing.interpolation.Interpolator;
import com.replaymod.replaystudio.pathing.interpolation.LinearInterpolator;

import java.util.function.Supplier;

public enum InterpolatorType {
    DEFAULT("default", null, null),
    CATMULL_ROM("catmullrom", CatmullRomSplineInterpolator.class, () -> new CatmullRomSplineInterpolator(0.5)),
    CUBIC("cubic", CubicSplineInterpolator.class, CubicSplineInterpolator::new),
    LINEAR("linear", LinearInterpolator.class, LinearInterpolator::new);

    private String localizationKey;

    private Class<? extends Interpolator> interpolatorClass;

    private Supplier<Interpolator> interpolatorConstructor;

    InterpolatorType(String localizationKey, Class<? extends Interpolator> interpolatorClass, Supplier<Interpolator> interpolatorConstructor) {
        this.localizationKey = localizationKey;
        this.interpolatorClass = interpolatorClass;
        this.interpolatorConstructor = interpolatorConstructor;
    }

    public String getLocalizationKey() {
        return localizationKey;
    }

    public String getI18nName() {
        return String.format("replaymod.gui.editkeyframe.interpolator.%1$s.name", localizationKey);
    }

    public String getI18nDescription() {
        return String.format("replaymod.gui.editkeyframe.interpolator.%1$s.desc", localizationKey);
    }

    public Class<? extends Interpolator> getInterpolatorClass() {
        return interpolatorClass;
    }

    public static InterpolatorType fromString(String string) {
        for (InterpolatorType t : values()) {
            if (t.getI18nName().equals(string)) return t;
        }
        return CATMULL_ROM; //the default
    }

    public static InterpolatorType fromClass(Class<? extends Interpolator> cls) {
        for (InterpolatorType type : values()) {
            if (cls.equals(type.getInterpolatorClass())) {
                return type;
            }
        }
        return DEFAULT;
    }

    public Interpolator newInstance() {
        return interpolatorConstructor.get();
    }
}
