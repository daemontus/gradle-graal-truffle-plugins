package com.oracle.truffle.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GraalCompilerPluginFunctionalTest {

    @Test
    public void canRunConfig() throws IOException {
        /*
            Create a fake test project which will:
                - Load the Graal compiler plugin and application plugin
                - Load a dependency on Graal JS
                - Create a custom JavaExec task
            Then verify that:
                - version configuration works (no warnings)
                - prepareCompiler is defined, executes and downloads compiler to expected location
                - installDist creates a distribution with the compiler included
                - start scripts for the distribution initialize compiler and Graal JS
         */
        File projectDir = new File("build/testProject");
        File settingsFile = new File(projectDir, "settings.gradle");
        File buildFile = new File(projectDir, "build.gradle");
        Files.createDirectories(projectDir.toPath());
        Files.write(settingsFile.toPath(), "".getBytes());
        Files.write(buildFile.toPath(),
                ("plugins { \n" +
                        "id 'java-library'\n" +
                        "id 'application'\n" +
                        "id 'org.graalvm.plugin.compiler'\n" +
                    "}\n" +
                    "repositories { jcenter() }\n" +
                    "application { mainClassName 'package.Main' }\n" +
                    "graal { version '20.1.0' }\n" +
                    "dependencies { language 'org.graalvm.js:js:20.1.0' }\n" +
                    "task runApp(type: JavaExec) {\n" +
                        "classpath = sourceSets.main.runtimeClasspath\n" +
                        "main = 'package.Main'\n" +
                    "}\n").getBytes()
        );

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("prepareCompiler", "installDist");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        File projectBuildDir = new File(projectDir, "build");
        assertTrue(projectBuildDir.exists() && projectBuildDir.isDirectory());

        File compilerDir = new File(projectBuildDir, "graalCompiler");
        assertTrue(compilerDir.exists() && compilerDir.isDirectory());
        assertTrue(Objects.requireNonNull(compilerDir.listFiles()).length > 0);

        File installDir = new File(projectBuildDir, "install");
        assertTrue(installDir.exists() && installDir.isDirectory());

        File distributionDir = new File(installDir, "testProject");
        assertTrue(distributionDir.exists() && distributionDir.isDirectory());

        File distCompilerDir = new File(distributionDir, "graalCompiler");
        assertTrue(distCompilerDir.exists() && distCompilerDir.isDirectory());
        assertTrue(Objects.requireNonNull(distCompilerDir.listFiles()).length > 0);

        File distBinDir = new File(distributionDir, "bin");
        assertTrue(distBinDir.exists() && distBinDir.isDirectory());

        File distStartScript = new File(distBinDir, "testProject");
        assertTrue(distStartScript.exists() && !distStartScript.isDirectory());
        String startScript = new String(Files.readAllBytes(distStartScript.toPath()), Charset.defaultCharset());
        assertTrue(startScript.contains("--upgrade-module-path=$APP_HOME/graalCompiler/"));
        assertTrue(startScript.contains("-Dtruffle.class.path.append=$APP_HOME/lib/js-20.1.0.jar"));

        assertFalse(result.getOutput().contains("WARNING"));
    }


}
