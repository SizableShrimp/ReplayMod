package com.replaymod.render.blend;

import com.replaymod.render.blend.data.DScene;
import com.replaymod.render.blend.data.Serializer;
import org.apache.commons.io.output.NullOutputStream;
import org.blender.utils.BlenderFactory;
import org.cakelab.blender.io.BlenderFile;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;

import static com.replaymod.core.versions.MCVer.*;
import static com.replaymod.render.ReplayModRender.LOGGER;

// Note:
// - Chunk exporter currently assumes VBO are enabled and in use
public class BlendState implements Exporter {
    private static BlendState currentState;

    public static void setState(BlendState state) {
        currentState = state;
    }

    public static BlendState getState() {
        return currentState;
    }

    private final List<Exporter> exporters = new ArrayList<>();
    private final BlenderFile blenderFile;
    private final BlenderFactory factory;
    private final DScene scene = new DScene();
    private final BlendMaterials materials = new BlendMaterials();

    public BlendState(File file) throws IOException {
        this.blenderFile = BlenderFactory.newBlenderFile(file);
        this.factory = new BlenderFactory(blenderFile);

        //#if MC>=10800 && MC<11900
        //$$ RenderState renderState = new RenderState(this);
        //$$ register(renderState);
        //$$ // FIXME 1.15
        //#if MC<11500
        //$$ register(new ChunkExporter());
        //#endif
        //$$ register(new EntityExporter(renderState));
        //$$ register(new TileEntityExporter(renderState));
        //$$ register(new ParticlesExporter(renderState));
        //$$ register(new ItemExporter(renderState));
        //#endif
    }

    public void register(Exporter exporter) {
        exporters.add(exporter);
    }

    public <T extends Exporter> T get(Class<T> clazz) {
        for (Exporter exporter : exporters) {
            if (clazz.isInstance(exporter)) {
                return clazz.cast(exporter);
            }
        }
        throw new NoSuchElementException("No exporter of type " + clazz);
    }

    @Override
    public void setup() {
        for (Exporter exporter : exporters) {
            try {
                exporter.setup();
            } catch (IOException e) {
                CrashReport report = CrashReport.forThrowable(e, "Setup of blend exporter");
                CrashReportCategory category = report.addCategory("Exporter");
                category.setDetail("Exporter", exporter::toString);
                throw new ReportedException(report);
            }
        }
    }

    @Override
    public void tearDown() {
        for (Exporter exporter : exporters) {
            try {
                exporter.tearDown();
            } catch (IOException e) {
                CrashReport report = CrashReport.forThrowable(e, "Tear down of blend exporter");
                CrashReportCategory category = report.addCategory("Exporter");
                category.setDetail("Exporter", exporter::toString);
                throw new ReportedException(report);
            }
        }

        try {
            Serializer serializer = new Serializer();
            this.scene.serialize(serializer);
            // TODO pre-configure render settings serializer.writeMajor(new Object(), new DId(BlockCodes.ID_REND))

            //noinspection UseOfSystemOutOrSystemErr
            PrintStream sysout = System.out;
            try {
                System.setOut(new PrintStream(new NullOutputStream()));
                blenderFile.write();
            } finally {
                System.setOut(sysout);
            }
        } catch (IOException e) {
            LOGGER.error("Exception writing blend file: ", e);
        } finally {
            try {
                blenderFile.close();
            } catch (IOException e) {
                LOGGER.error("Exception closing blend file: ", e);
            }
        }
    }

    @Override
    public void preFrame(int frame) {
        for (Exporter exporter : exporters) {
            try {
            exporter.preFrame(frame);
            } catch (IOException e) {
                CrashReport report = CrashReport.forThrowable(e, "Pre frame of blend exporter");
                CrashReportCategory category = report.addCategory("Exporter");
                category.setDetail("Exporter", exporter::toString);
                category.setDetail("Frame", () -> String.valueOf(frame));
                throw new ReportedException(report);
            }
        }
    }

    @Override
    public void postFrame(int frame) {
        for (Exporter exporter : exporters) {
            try {
                exporter.postFrame(frame);
            } catch (IOException e) {
                CrashReport report = CrashReport.forThrowable(e, "Post frame of blend exporter");
                CrashReportCategory category = report.addCategory("Exporter");
                category.setDetail("Exporter", exporter::toString);
                category.setDetail("Frame", () -> String.valueOf(frame));
                throw new ReportedException(report);
            }
        }

        scene.endFrame = frame;
    }

    public BlenderFile getBlenderFile() {
        return blenderFile;
    }

    public BlenderFactory getFactory() {
        return factory;
    }

    public DScene getScene() {
        return scene;
    }

    public BlendMaterials getMaterials() {
        return materials;
    }
}
