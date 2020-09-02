package com.oracle.truffle.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;

import java.io.File;

import static org.junit.Assert.assertFalse;

public class AbstractFunctionalTest {

    protected final File projectDir;

    public AbstractFunctionalTest(String projectDir) {
        this.projectDir = new File(projectDir);
    }

    /* Clean up test project. */
    protected void cleanProject() {
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments("clean");
        runner.withProjectDir(projectDir);
        runner.build();
        File buildDir = new File(projectDir, "build");
        assertFalse(buildDir.exists());
    }

    protected BuildResult runBuild(String... args) {
        return runBuild(false, args);
    }

    protected BuildResult runFailingBuild(String... args) {
        return runBuild(true, args);
    }

    private BuildResult runBuild(boolean fails, String... args) {
        GradleRunner runner = GradleRunner.create();
        runner.forwardOutput();
        runner.withPluginClasspath();
        runner.withArguments(args);
        runner.withProjectDir(projectDir);
        if (fails) {
            return runner.buildAndFail();
        } else {
            return runner.build();
        }
    }

}
