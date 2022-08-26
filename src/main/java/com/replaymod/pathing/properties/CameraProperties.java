package com.replaymod.pathing.properties;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.camera.CameraEntity;
import com.replaymod.replaystudio.pathing.change.Change;
import com.replaymod.replaystudio.pathing.property.AbstractProperty;
import com.replaymod.replaystudio.pathing.property.AbstractPropertyGroup;
import com.replaymod.replaystudio.pathing.property.PropertyPart;
import com.replaymod.replaystudio.pathing.property.PropertyParts;
import de.johni0702.minecraft.gui.utils.NonNull;
import org.apache.commons.lang3.tuple.Triple;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Properties for camera positioning.
 */
public class CameraProperties extends AbstractPropertyGroup {
    public static final CameraProperties GROUP = new CameraProperties();
    public static final Position POSITION = new Position();
    public static final Rotation ROTATION = new Rotation();
    private CameraProperties() {
        super("camera", "replaymod.gui.camera");
    }

    @Override
    public Optional<Callable<Change>> getSetter() {
        return Optional.empty();
    }

    public static class Position extends AbstractProperty<Triple<Double, Double, Double>> {
        public final PropertyPart<Triple<Double, Double, Double>>
                X = new PropertyParts.ForDoubleTriple(this, true, PropertyParts.TripleElement.LEFT),
                Y = new PropertyParts.ForDoubleTriple(this, true, PropertyParts.TripleElement.MIDDLE),
                Z = new PropertyParts.ForDoubleTriple(this, true, PropertyParts.TripleElement.RIGHT);

        private Position() {
            super("position", "replaymod.gui.position", GROUP, Triple.of(0d, 0d, 0d));
        }

        @Override
        public Collection<PropertyPart<Triple<Double, Double, Double>>> getParts() {
            return Arrays.asList(X, Y, Z);
        }

        @Override
        public void applyToGame(Triple<Double, Double, Double> value, @NonNull Object replayHandler) {
            ReplayHandler handler = ((ReplayHandler) replayHandler);
            handler.spectateCamera();
            CameraEntity cameraEntity = handler.getCameraEntity();
            if (cameraEntity != null) {
                cameraEntity.setCameraPosition(value.getLeft(), value.getMiddle(), value.getRight());
            }
        }

        @Override
        public void toJson(JsonWriter writer, Triple<Double, Double, Double> value) throws IOException {
            writer.beginArray().value(value.getLeft()).value(value.getMiddle()).value(value.getRight()).endArray();
        }

        @Override
        public Triple<Double, Double, Double> fromJson(JsonReader reader) throws IOException {
            reader.beginArray();
            try {
                return Triple.of(reader.nextDouble(), reader.nextDouble(), reader.nextDouble());
            } finally {
                reader.endArray();
            }
        }
    }

    public static class Rotation extends AbstractProperty<Triple<Float, Float, Float>> {
        public final PropertyPart<Triple<Float, Float, Float>>
                YAW = new PropertyParts.ForFloatTriple(this, true, 360, PropertyParts.TripleElement.LEFT),
                PITCH = new PropertyParts.ForFloatTriple(this, true, 360, PropertyParts.TripleElement.MIDDLE),
                ROLL = new PropertyParts.ForFloatTriple(this, true, 360, PropertyParts.TripleElement.RIGHT);

        private Rotation() {
            super("rotation", "replaymod.gui.rotation", GROUP, Triple.of(0f, 0f, 0f));
        }

        @Override
        public Collection<PropertyPart<Triple<Float, Float, Float>>> getParts() {
            return Arrays.asList(YAW, PITCH, ROLL);
        }

        @Override
        public void applyToGame(Triple<Float, Float, Float> value, @NonNull Object replayHandler) {
            ReplayHandler handler = ((ReplayHandler) replayHandler);
            handler.spectateCamera();
            CameraEntity cameraEntity = handler.getCameraEntity();
            if (cameraEntity != null) {
                cameraEntity.setCameraRotation(value.getLeft(), value.getMiddle(), value.getRight());
            }
        }

        @Override
        public void toJson(JsonWriter writer, Triple<Float, Float, Float> value) throws IOException {
            writer.beginArray().value(value.getLeft()).value(value.getMiddle()).value(value.getRight()).endArray();
        }

        @Override
        public Triple<Float, Float, Float> fromJson(JsonReader reader) throws IOException {
            reader.beginArray();
            try {
                return Triple.of((float) reader.nextDouble(), (float) reader.nextDouble(), (float) reader.nextDouble());
            } finally {
                reader.endArray();
            }
        }
    }
}
