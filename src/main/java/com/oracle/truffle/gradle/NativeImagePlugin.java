package com.oracle.truffle.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ApplicationPlugin;
import org.gradle.api.plugins.ApplicationPluginConvention;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Map;

/**
 * <p>Native Image Plugin ({@code org.graalvm.plugin.native-image}) provides a {@link NativeImage} task prototype
 * for creating native binaries using the GraalVM {@code native-image} tool. If the application plugin is enabled,
 * we also automatically create a {@code distNative} task which generates an executable binary for the
 * main distribution.</p>
 */
public class NativeImagePlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        // We have to run this after evaluate because application plugin must be first applied.
        project.afterEvaluate(it -> {
            Map<String, Object> plugins = project.getConvention().getPlugins();
            if (plugins.containsKey("application")) {
                Object applicationConvention = plugins.get("application");
                if (!(applicationConvention instanceof ApplicationPluginConvention)) {
                    System.err.println("Cannot automatically create distNative task.");
                    System.err.println("Expected ApplicationPluginConvention, but found "+applicationConvention+".");
                } else {
                    ApplicationPluginConvention app = (ApplicationPluginConvention) applicationConvention;
                    project.getTasks().create("distNative", NativeImage.class, task -> {
                        task.setForMainClass(app.getMainClassName());
                        task.setOutputName(app.getApplicationName());
                        task.setOutputDir(new File(project.getBuildDir(), "distributions"));
                    });
                }
            }
        });
    }

}
