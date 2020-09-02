package com.oracle.truffle.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.bundling.Jar;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class LanguagePlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        // We depend on the compiler plugin. Specifically we assume that
        //  1. Graal compiler is correctly added on Hotspot and bundled to distributions.
        //  2. Truffle classpath is adjusted to include this project (when language plugin is applied).
        project.getPluginManager().apply(CompilerPlugin.class);
        GraalExtension config = GraalExtension.initInProject(project);

        project.afterEvaluate(p -> {
            if (config.getLanguageId() == null) {
                throw new IllegalStateException("Please specify truffle language id using `graal { languageId = 'my.id.language' }`");
            }
            Jar graalComponent = project.getTasks().create("graalComponent", Jar.class, task -> {
                task.setGroup("distribution");
                task.getArchiveBaseName().set(config.getLanguageName() + "-component");
                task.getDestinationDirectory().set(new File(project.getBuildDir(), "distributions"));
                File tmpDir = task.getTemporaryDir();
                /*File symlinks = new File(tmpDir, "symlinks");
                try {
                    Files.write(symlinks.toPath(), "Hello symlinks!".getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }*/
                /*task.metaInf(c -> {
                    c.from(symlinks);
                });*/
                task.manifest(manifest -> {
                    manifest.getAttributes().put("Bundle-Name", config.getLanguageName());
                    manifest.getAttributes().put("Bundle-Symbolic-Name", config.getLanguageId());
                    manifest.getAttributes().put("Bundle-Version", config.getVersion());
                    manifest.getAttributes().put("Bundle-RequireCapability", "org.graalvm; filter:=\"(&(graalvm_version="+config.getVersion()+"))\"");
                    manifest.getAttributes().put("x-GraalVM-Polyglot-Part", "True");
                });
                JavaPluginConvention javaPlugin = project.getConvention().findPlugin(JavaPluginConvention.class);
                if (javaPlugin == null) return;
                SourceSet mainSources = javaPlugin.getSourceSets().findByName("main");
                if (mainSources == null) return;
                task.from(mainSources.getCompileClasspath(), copy -> {
                    copy.into("languages/"+config.getLanguageName()+"/lib");
                });
                task.from(project.getTasks().findByName("jar").getOutputs(), copy -> {
                    copy.into("languages/"+config.getLanguageName());
                });
            });

        });
    }

}
