package com.oracle.truffle.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

public class NativeImagePluginFunctionalTest {


    @Test @Ignore
    public void canDeclareTask() throws IOException {
        /*
            Create a fake test project which will:
                - Load the native image plugin and application plugin
                - Create a custom NativeImage task
            Then verify that:
                - task can be created
                - distNative is defined
         */
        File projectDir = new File("build/nativeImageTestProject");
        File settingsFile = new File(projectDir, "settings.gradle");
        File buildFile = new File(projectDir, "build.gradle");
        Files.createDirectories(projectDir.toPath());
        Files.write(settingsFile.toPath(), "".getBytes());
        Files.write(buildFile.toPath(),
                ("import com.oracle.truffle.gradle.NativeImage\n" +
                        "plugins { \n" +
                        "id 'java-library'\n" +
                        "id 'application'\n" +
                        "id 'org.graalvm.plugin.native-image'\n" +
                        "}\n" +
                        "repositories { jcenter() }\n" +
                        "application { mainClassName 'package.Main' }\n" +
                        "task customNative(type: NativeImage) {\n" +
                        "forJar 'libs/myFancyJar.jar'\n" +
                        "}\n").getBytes()
        );

        // Run the build
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("distNative");
        runner.withProjectDir(projectDir);
        BuildResult result = runner.buildAndFail();

        assertTrue(result.getOutput().contains("Not running on GraalVM and GRAALVM_HOME not set. Native image not available."));
    }

}
