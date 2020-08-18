package com.oracle.truffle.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Objects;

import static org.junit.Assert.*;

/*
    We have a test project `test-compiler-plugin` which has a main class that checks if polyglot engine
    is Graal and if not, throws an error. The project then has an application plugin setup for this main class
    as well as custom `JavaExec` task (`runCustom`) to execute this class.

    We then test that:
     - `clean` is runnable and removes the `build` directory
     - [Hotspot] `run` and `runCustom` can be executed and do not fail due to missing Graal
     - application scripts contain Graal compiler initialization
     - application scripts can be executed and do not fail due to missing Graal
 */
public class CompilerPluginFunctionalTest {

    private final File projectDir = new File("src/functionalTest/project/test-compiler-plugin");

    /* Clean up test project. */
    private void cleanProject(File projectDir) {
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("clean");
        runner.withProjectDir(projectDir);
        runner.build();
        File buildDir = new File(projectDir, "build");
        assertFalse(buildDir.exists());
    }

    @Test
    public void addsCompilerToJavaExec() {
        Assume.assumeFalse(TestUtils.isGraalVM());  // Hotspot only
        cleanProject(projectDir);

        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("runCustom");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.build();

        // Make sure Graal version is initialized properly.
        assertFalse(result.getOutput().contains("WARNING"));

        cleanProject(projectDir);
    }

    @Test
    public void addsCompilerToApplicationRun() {
        Assume.assumeFalse(TestUtils.isGraalVM());  // Hotspot only

        cleanProject(projectDir);

        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("run");
        runner.withProjectDir(projectDir);
        runner.build();

        cleanProject(projectDir);
    }

    @Test
    public void addsCompilerToApplicationScripts() throws IOException, InterruptedException {
        cleanProject(projectDir);

        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("installDist");
        runner.withProjectDir(projectDir);
        runner.build();

        String scriptFilePath = "src/functionalTest/project/test-compiler-plugin/build/install/test-compiler-plugin/bin/test-compiler-plugin";
        File startScriptFile = new File(scriptFilePath);
        String startScript = new String(Files.readAllBytes(startScriptFile.toPath()), Charset.defaultCharset());
        assertTrue(startScript.contains("--module-path=$APP_HOME/graalCompiler/"));

        File compilerDir = new File("src/functionalTest/project/test-compiler-plugin/build/install/test-compiler-plugin/graalCompiler");
        assertTrue(compilerDir.exists() && compilerDir.isDirectory());
        assertTrue(Objects.requireNonNull(compilerDir.listFiles()).length > 0);

        if (TestUtils.isWindows()) {
            scriptFilePath = scriptFilePath + ".bat";
        }
        Process app = Runtime.getRuntime().exec(scriptFilePath);
        assertEquals(0, app.waitFor());

        cleanProject(projectDir);
    }

}
