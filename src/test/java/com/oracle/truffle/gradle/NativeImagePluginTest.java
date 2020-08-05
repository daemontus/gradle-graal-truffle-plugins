package com.oracle.truffle.gradle;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class NativeImagePluginTest {

    @Test
    public void registersNativeDistributionTask() {
        // Check that prepareCompiler is defined when compiler plugin is applied.
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("org.graalvm.plugin.native-image");
        project.getPlugins().apply("application");


        // Magical `false` will internally call project.evaluate()
        Set<Task> tasks = project.getTasksByName("distNative", false);
        assertFalse(tasks.isEmpty());
        assertTrue(tasks.iterator().next() instanceof NativeImage);
    }

}
