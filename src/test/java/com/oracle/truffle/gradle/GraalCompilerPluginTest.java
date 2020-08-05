package com.oracle.truffle.gradle;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.JavaExec;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class GraalCompilerPluginTest {

    @Test
    public void registersDownloadTask() {
        /* Check that prepareCompiler is defined when compiler plugin is applied. */
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("org.graalvm.plugin.compiler");

        assertNotNull(project.getTasks().findByName("prepareCompiler"));
    }

    @Test
    public void extendsJavaExecTask() {
        /* Check that a JavaExec task is enhanced to depend on prepareCompiler. */
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("org.graalvm.plugin.compiler");
        Task testExec = project.getTasks().create("testExec", JavaExec.class, task -> {
            task.setMain("test.Main");
            task.classpath("irrelevant");
        });

        assertTrue(testExec.getDependsOn().contains(project.getTasks().findByName("prepareCompiler")));
    }

}
