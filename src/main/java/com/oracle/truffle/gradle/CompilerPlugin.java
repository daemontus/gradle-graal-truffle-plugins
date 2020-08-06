package com.oracle.truffle.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.application.tasks.CreateStartScripts;
import org.gradle.process.JavaForkOptions;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

/**
 * <p>Graal Compiler Plugin ({@code org.graalvm.plugin.compiler}) is responsible for managing dependencies on
 * the Graal compiler binaries and custom truffle languages. It also ensures the dependencies are properly
 * linked for usage on both Hotspot and Graal JVMs.</p>
 *
 * <p>The plugin is configured using a global {@code graal} extension. However, for now, only the compiler
 * version is required:</p>
 *
 * {@code
 * graal {
 *     // The compiler version used on Hotspot JVMs (On GraalVM, the built-in compiler is used).
 *     version '20.2.0'
 * }
 * }
 *
 * <p>To use the Graal compiler on Hotspot JVMs (supported since version 11), the plugin will automatically
 * add the compiler dependency and configure the compiler in all {@code JavaExec} tasks as well as distributions
 * (by modifying the distribution start scripts). When running on GraalVM, the compiler is already built-in, so
 * no modifications are necessary.</p>
 *
 * <p>When depending on Graal languages, two new configurations are used: {@code language} and
 * {@code installedLanguage}. On Hotspot, both configurations essentially translate to standard runtime
 * dependencies, since Hotspot does not have installed languages. On GraalVM, any language dependency declared
 * using {@code installedLanguage} is assumed to be already installed using {@code gu}. Meanwhile, if
 * the dependency is {@code language}, the language will be explicitly loaded using the
 * {@code truffle.class.path.append} property.</p>
 *
 * <p>For example, if your project depends on both JavaScript and some custom DSL (domain specific language), it
 * is reasonable to declare JavaScript as {@code installedLanguage} because typical GraalVM installation
 * already comes with JavaScript bundled. On the other hand, unless we expect the users to explicitly install
 * your DSL using {@code gu}, the DSL should be simply declared as {@code language}. This will ensure that
 * the project works fine with GraalVM installations where the DSL is not present.</p>
 *
 * <p>On Hotspot, neither JavaScript, nor the DSL is available and thus needs to be loaded explicitly. So if you are
 * only targeting Hotspot JVMs, you can ignore the {@code installedLanguage} configuration entirely.</p>
 */
public class CompilerPlugin implements Plugin<Project> {

    private static final String COMPILER_CONFIG = "graalCompiler";

    @Override
    public void apply(@Nonnull Project project) {
        project.getPluginManager().apply(JavaPlugin.class); // Graal requires Java plugin.
        GraalExtension config = GraalExtension.initInProject(project);   // Load configuration object.

        // Setup Graal compiler
        Task compilerTask = this.declareCompilerDependency(project, config);
        this.setupGraalCompilerInExecutableTasks(project, config, compilerTask);
        this.setupGraalCompilerInDistributions(project);

        // Setup language dependency configurations
        this.setupLanguageDependencyConfigurations(project);
        this.setupDynamicGraalLanguages(project);
    }

    /* Create dependency, configuration and download task for the Graal compiler. */
    private Task declareCompilerDependency(Project project, GraalExtension config) {
        Configuration compilerConfig = project.getConfigurations().create(COMPILER_CONFIG);

        compilerConfig.setVisible(false);
        compilerConfig.setCanBeResolved(true);
        compilerConfig.setDescription("Graal compiler and its dependencies.");
        compilerConfig.withDependencies(dependencies ->
                dependencies.add(project.getDependencies().create("org.graalvm.compiler:compiler:"+config.getVersion()))
        );

        Copy prepareCompiler = project.getTasks().create("prepareCompiler", Copy.class, task ->
                task.setGroup("graal")
        );

        // Task must be configured after project because it depends on config and dependency resolution.
        project.afterEvaluate(it -> {
            prepareCompiler.from(compilerConfig.getFiles());
            prepareCompiler.into(config.getCompilerDir());
        });

        return prepareCompiler;
    }

    /* Setup every JavaForOptions task so that it uses the Graal compiler. */
    private void setupGraalCompilerInExecutableTasks(Project project, GraalExtension config, Task compilerTask) {
        project.getTasks().all(task -> {
            if (task instanceof JavaForkOptions) {
                task.dependsOn(compilerTask);
                task.doFirst(it -> {
                   if (!PluginUtils.isGraalVM()) {  // When running on Graal, the compiler is already there...
                       if (!PluginUtils.hasJVMCI()) {   // Unsupported JVM - print warning.
                           System.err.println("WARNING: Support for JVM Compiler Interface not detected.");
                           System.err.println("Truffle languages running in interpreter mode only.");
                       } else {
                           JavaForkOptions opts = (JavaForkOptions) it;
                           String compilerPath = config.getCompilerDir().getAbsolutePath();
                           opts.jvmArgs(
                                   "-XX:+UnlockExperimentalVMOptions", "-XX:+EnableJVMCI",
                                   "--module-path=" + compilerPath, "--upgrade-module-path=" + compilerPath
                           );
                       }
                   }
                });

            }
        });
    }

    /* Add Graal compiler to every distribution and hack into CreateStartScripts so that it is actually used. */
    private void setupGraalCompilerInDistributions(Project project) {
        project.afterEvaluate(it -> {   // needs to be done after evaluation because we depend on compilerConfig
            Configuration compilerConfig = project.getConfigurations().getByName(COMPILER_CONFIG);

            // Copy compiler to all distributions
            PluginUtils.withDistributions(it, distributions -> distributions.all(distribution ->
                    distribution.getContents().from(compilerConfig.getFiles(), spec -> spec.into("graalCompiler"))
            ));

            // In all CreateStartScripts tasks, replace occurrences of __APP_HOME__ with teh appropriate
            // environment variable (this is also used in truffle language plugin).
            project.getTasks().withType(CreateStartScripts.class)
                    .all(CompilerPlugin::addReplaceAppHomeAction);

            // Add default JVM arguments to the start scripts which will enable the Graal compiler.
            project.getTasks().withType(CreateStartScripts.class)
                    .all(CompilerPlugin::addCompilerArgsToDistribution);
        });
    }

    /* Declares the `graalLanguage` and `installedGraalLanguage` configurations. */
    private void setupLanguageDependencyConfigurations(Project project) {
        Configuration graalLanguage = project.getConfigurations().create("language");
        graalLanguage.setVisible(true);
        graalLanguage.setCanBeResolved(false);
        graalLanguage.setDescription("Graal languages which should be dynamically loaded.");

        Configuration installedGraalLanguage = project.getConfigurations().create("installedLanguage");
        installedGraalLanguage.setVisible(true);
        installedGraalLanguage.setCanBeResolved(false);
        installedGraalLanguage.setDescription("Graal languages which are already installed by `gu`.");

        Configuration truffleClasspath = project.getConfigurations().create("truffleClasspath");
        truffleClasspath.setCanBeResolved(true);
        truffleClasspath.extendsFrom(graalLanguage);

        Configuration runtime = project.getConfigurations().getByName("runtimeClasspath");
        runtime.extendsFrom(graalLanguage);
        runtime.extendsFrom(installedGraalLanguage);
    }

    /* Load dynamic (not-installed) Graal languages using truffle.class.path.append. */
    private void setupDynamicGraalLanguages(Project project) {
        // Update all fork tasks (relevant only if running on Graal):
        if (PluginUtils.isGraalVM()) {
            project.getTasks().all(task -> {
                if (task instanceof JavaForkOptions) {
                    // Do this as the task executes to make sure truffle classpath can be resolved.
                    task.doFirst(it -> {
                        JavaForkOptions opts = (JavaForkOptions) task;
                        opts.systemProperty("truffle.class.path.append", getTruffleClasspath(project, false).getAsPath());
                    });
                }
            });
        }
        // Configure all distributions (relevant on any VM since distribution can run on anything):
        project.getTasks().withType(CreateStartScripts.class).all(task -> task.doFirst(it -> {
            CreateStartScripts scripts = (CreateStartScripts) it;
            // Build the truffle classpath for the start script. Note that this is different from the
            // runtime classpath used in Fork tasks, because here the path is relative to the APP_HOME folder.
            StringBuilder classpath = new StringBuilder();
            for (File f : getTruffleClasspath(project, true).getFiles()) {
                classpath.append("__APP_HOME__/lib/");
                classpath.append(f.getName());
                classpath.append(":");
            }
            scripts.setDefaultJvmOpts(PluginUtils.appendIterable(
                    scripts.getDefaultJvmOpts(),
                    "-Dtruffle.class.path.append="+classpath.toString()
            ));
        }));
    }

    /* Replace occurrences of __APP_HOME__ with a platform-specific environment variable. */
    private static void addReplaceAppHomeAction(CreateStartScripts scripts) {
        scripts.doLast(it -> {
            try {
                PluginUtils.replaceInFile(scripts.getUnixScript(), "__APP_HOME__", "$APP_HOME");
                PluginUtils.replaceInFile(scripts.getWindowsScript(), "__APP_HOME__", "%APP_HOME%");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /* Add default JVM options that enable the Graal compiler. */
    private static void addCompilerArgsToDistribution(CreateStartScripts scripts) {
        scripts.doFirst(it ->
                scripts.setDefaultJvmOpts(PluginUtils.appendIterable(scripts.getDefaultJvmOpts(),
                        // A workaround so that we don't have to remove Graal compiler on older JVMs.
                        "-XX:+IgnoreUnrecognizedVMOptions",
                        "-XX:+UnlockExperimentalVMOptions",
                        "-XX:+EnableJVMCI",
                        "--module-path=__APP_HOME__/graalCompiler/",
                        "--upgrade-module-path=__APP_HOME__/graalCompiler/"
                ))
        );
    }

    /*
        In normal projects, truffle classpath is based on the truffleClasspath configuration. But if the language
        plugin is also applied, we add other dependencies and sources as well.
     */
    private FileCollection getTruffleClasspath(Project project, boolean fromArchive) {
        Configuration truffleClasspath = project.getConfigurations().findByName("truffleClasspath");
        assert truffleClasspath != null;
        if (!project.getPluginManager().hasPlugin("org.graalvm.plugin.truffle-language")) {
            // Normal project
            return truffleClasspath.fileCollection();
        } else {
            // Language project
            FileCollection classpath = truffleClasspath.fileCollection();
            // Add all runtime configuration files except for installed languages:
            Configuration runtime = project.getConfigurations().getAt("runtimeClasspath");
            for (Configuration cfg : runtime.getExtendsFrom()) {
                if (cfg.getName().equals("installedLanguage")) continue;
                classpath = classpath.plus(cfg.fileCollection());
            }
            if (fromArchive) {
                // Running from compiled .jar, add jar location:
                Task jar = project.getTasks().getAt("jar");
                classpath = classpath.plus(jar.getOutputs().getFiles());
            } else {
                // Running from compiled .class files, get compiled files location:
                JavaPluginConvention javaPlugin = project.getConvention().getPlugin(JavaPluginConvention.class);
                SourceSet mainSources = javaPlugin.getSourceSets().getAt("main");
                classpath = classpath.plus(mainSources.getOutput());
            }
            return classpath;
        }
    }

}
