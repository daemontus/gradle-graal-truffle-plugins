package com.oracle.truffle.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/*
    We have a project `test-javascript` which will try to run some JavaScript code using polyglot Context
    and old ScriptEngine API and verify that both are running using Graal. Everything is runnable using
    `application` plugin.

    We then test that:
     - both JavaScript variants work with Graal
     - JavaScript is correctly loaded on truffle classpath
 */
public class JavascriptFunctionalTest extends AbstractFunctionalTest {

    public JavascriptFunctionalTest() {
        super("src/functionalTest/project/test-javascript");
    }

    @Test
    public void canRunJavaScript() {
        cleanProject();
        BuildResult result = runBuild("run");
        assertTrue(result.getOutput().contains("Execution success."));
        cleanProject();
    }

    @Test
    public void hasJavaScriptInDistribution() throws IOException, InterruptedException {
        cleanProject();
        // Build distribution:
        runBuild("installDist");
        String scriptFilePath = "src/functionalTest/project/test-javascript/build/install/test-javascript/bin/test-javascript";
        File startScriptFile = new File(scriptFilePath);
        // Check that start script loads JavaScript to truffle classpath:
        String startScript = new String(Files.readAllBytes(startScriptFile.toPath()), Charset.defaultCharset());
        assertTrue(startScript.contains("-Dtruffle.class.path.append=$APP_HOME/lib/js-20.1.0.jar"));
        // Run the distribution:
        if (TestUtils.isWindows()) {
            scriptFilePath = scriptFilePath + ".bat";
        }
        Process app = Runtime.getRuntime().exec(scriptFilePath);
        assertEquals(0, app.waitFor());
        cleanProject();
    }

}
