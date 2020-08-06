package com.oracle.truffle.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import javax.annotation.Nonnull;

public class LanguagePlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        // We depend on the compiler plugin. Specifically we assume that
        //  1. Graal compiler is correctly added on Hotspot and bundled to distributions.
        //  2. Truffle classpath is adjusted to include this project (when language plugin is applied).
        project.getPluginManager().apply(CompilerPlugin.class);


    }

}
