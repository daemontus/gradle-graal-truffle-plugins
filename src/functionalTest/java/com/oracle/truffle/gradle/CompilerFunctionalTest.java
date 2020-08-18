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
     - [Hotspot] `run` and `runCustom` can be executed and do not fail due to missing Graal
     - application scripts contain Graal compiler initialization
     - application scripts can be executed and do not fail due to missing Graal
 */
public class CompilerFunctionalTest extends AbstractFunctionalTest {

    public CompilerFunctionalTest() {
        super("src/functionalTest/project/test-compiler");
    }

    @Test
    public void addsCompilerToJavaExec() {
        Assume.assumeFalse(TestUtils.isGraalVM());  // Hotspot only
        cleanProject();
        BuildResult result = runBuild("runCustom");
        // Make sure Graal version is initialized properly.
        assertFalse(result.getOutput().contains("WARNING"));
        cleanProject();
    }

    @Test
    public void addsCompilerToApplicationRun() {
        Assume.assumeFalse(TestUtils.isGraalVM());  // Hotspot only
        cleanProject();
        runBuild("run");
        cleanProject();
    }

    @Test
    public void addsCompilerToApplicationScripts() throws IOException, InterruptedException {
        cleanProject();
        runBuild("installDist");

        String scriptFilePath = "src/functionalTest/project/test-compiler/build/install/test-compiler/bin/test-compiler";
        File startScriptFile = new File(scriptFilePath);
        String startScript = new String(Files.readAllBytes(startScriptFile.toPath()), Charset.defaultCharset());
        assertTrue(startScript.contains("--module-path=$APP_HOME/graalCompiler/"));

        File compilerDir = new File("src/functionalTest/project/test-compiler/build/install/test-compiler/graalCompiler");
        assertTrue(compilerDir.exists() && compilerDir.isDirectory());
        assertTrue(Objects.requireNonNull(compilerDir.listFiles()).length > 0);

        if (TestUtils.isWindows()) {
            scriptFilePath = scriptFilePath + ".bat";
        }
        Process app = Runtime.getRuntime().exec(scriptFilePath);
        assertEquals(0, app.waitFor());

        cleanProject();
    }

}
