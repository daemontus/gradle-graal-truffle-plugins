package com.oracle.truffle.gradle;

import org.gradle.api.Project;
import org.gradle.internal.extensibility.DefaultExtraPropertiesExtension;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;

public class GraalExtension {

    public static final String DEFAULT_GRAAL_VERSION = "20.1.0";

    private String version;
    private String languageId;
    private String languageName;

    private File compilerDir;

    /**
     * <p>Setup default configuration values from project properties.</p>
     * @param project Project to load defaults from.
     */
    void initDefaults(@Nonnull Project project) {
        // Set default path to graal-compiler folder.
        if (this.compilerDir == null) {
            this.compilerDir = new File(project.getBuildDir(), "graalCompiler");
        }
        // Try to load version from default extra properties.
        Object ext = project.getExtensions().findByName("ext");
        if (ext instanceof DefaultExtraPropertiesExtension) {
            DefaultExtraPropertiesExtension props = (DefaultExtraPropertiesExtension) ext;
            Object versionCandidate = props.find("graalVersion");
            if (versionCandidate instanceof String) {
                this.version = (String) versionCandidate;
            }
        }
        // Init language name to project name.
        this.languageName = project.getName();
    }

    /**
     * @return Requested Graal compiler version.
     */
    @Nullable
    public String getVersion() {
        if (this.version == null) {
            System.err.println("WARNING: Graal version not set. Defaulting to "+DEFAULT_GRAAL_VERSION+".");
            System.err.println("Set graal version using: graal { version = 'version_string' } in the build.gradle file.");
            return DEFAULT_GRAAL_VERSION;
        } else {
            return this.version;
        }
    }

    /**
     * @return Directory with Graal compiler and its dependencies.
     */
    @Nonnull
    public File getCompilerDir() {
        return this.compilerDir;
    }

    /**
     * @param version Requested Graal compiler version.
     */
    public void setVersion(@Nullable String version) {
        this.version = version;
    }

    /**
     * @return The string id of a truffle language defined in this project (or null if this project
     * is not a language project).
     */
    @Nullable
    public String getLanguageId() {
        return this.languageId;
    }

    /**
     * Set the identifier of the language defined in this project.
     * @param languageId string id
     */
    public void setLanguageId(@Nullable String languageId) {
        this.languageId = languageId;
    }

    @Nonnull
    public String getLanguageName() {
        return this.languageName;
    }

    public void setLanguageName(@Nonnull String languageName) {
        this.languageName = languageName;
    }

    /**
     * <p>Initialize the Graal config extension (with defaults) in the given project.</p>
     */
    static GraalExtension initInProject(Project project) {
        GraalExtension config;
        Object extension = project.getExtensions().findByName("graal");
        if (extension != null && !(extension instanceof GraalExtension)) {
            throw new IllegalStateException("Name clash - extension graal already exists.");
        }
        if (extension != null) {
            config = (GraalExtension) extension;
        } else {
            config = project.getExtensions().create("graal", GraalExtension.class);
            config.initDefaults(project);
        }
        return config;
    }


}
