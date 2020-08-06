# Graal & Truffle Gradle Plugins
 
This repository contains several useful plugins for developing **native and polyglot 
applications using the Graal compiler**. The goal of all plugins is to 
*"just work"* whether you are using Hotspot (OpenJDK, OracleJDK, etc.) or 
GraalVM, so you don't actually need GraalVM to start using the Graal 
compiler and its polyglot features. 

There are three plugins, each focusing on a specific sub-part 
of Graal functionality:

 - *Graal Compiler Plugin:* Configures the project and its distributions 
 to use Graal compiler when possible, even on standard Hotspot JVMs. This
 provides a significant speed-up when using Graal languages and polyglot 
 APIs on Hotspot JVMs. Finally, it also allows easy declaration of
 Graal language dependencies from standard artefacts (bypassing 
 `gu install`).
 - *Native Image Plugin:* Allows declaration of `NativeImage` tasks which
 use Graal's `native-image` compiler to produce native binaries of Java
 executables with minimal configuration. Automatically configures 
 a `NativeImage` task for application distributions.
 - *Graal Language Plugin:* Sets-up the project for development of a custom
 Graal&Truffle language. Automatically configures Truffle classpath and 
 allows creation of packages installable with `gu`.
 
You can see some basic and advanced examples of how to use the plugins in the 
[demo repository](https://github.com/daemontus/gradle-graal-truffle-plugins-demo).
 
> Disclaimer: There are currently two other plugins that integrate with 
> `native-image`: [org.mikeneck.graalvm-native-image](https://github.com/mike-neck/graalvm-native-image-plugin)
> and [com.palantir.graal](https://github.com/palantir/gradle-graal). 
> These have slightly different feature sets than our Native Image Plugin,
> so feel free to use whatever works best for you.   

## Graal Compiler Plugin

To start using the *Graal Compiler Plugin*, add it to your plugin list and 
configure the desired Graal version:

```groovy
plugins {
    id 'org.graalvm.plugin.compiler' version '$latest'
}

graal {
    version '20.1.0'
}
```

> This declared version of the compiler will be used on Hotspot JVMs. GraalVM
will use its built-in compiler.

With this setup, any `JavaExec` task or a distribution created by the 
`application` plugin will automatically use the Graal compiler if possible
(only supported from JDK version 11+).

Furthermore, we can declare language dependencies. We differentiate `language`
and `installedLanguage` dependencies. On Hotspot, these essentially correspond
to `runtime` dependencies, as Hotspot does not support installation of Graal 
languages. On GraalVM, we assume `installedLanguage` is already installed using `gu`
and is thus not loaded explicitly. However, we load any `language` dependency
explicitly using `truffle.class.path.append` to ensure the language works even
when not installed:

```groovy
dependencies {
    // JavaScript is typically part of GraalVM, so we 
    // can assume it is installed. We still have to 
    // include it as a dependency to make it available 
    // on Hotspot though. 
    installedLanguage 'org.graalvm.js:js:20.1.0'
    // Some custom, domain specific language that may not be
    // installed, we thus load it explicitly even on GraalVM.
    language 'org.company:my-awesome-language:1.2.3'
}
```

In the demo repository, you can see how this can be used to run 
[fast Graal JavaScript](https://github.com/daemontus/gradle-graal-truffle-plugins-demo/tree/master/fast-javascript)
instead of the deprecated Nashorn engine, or how to consume a [custom
Graal language as a dependency](TODO).  

## Native Image Plugin

> Native image tasks only work when running on GraalVM or when `GRAALVM_HOME` 
> points to a distribution of GraalVM with `native-image` installed. 

Start by apllying the *Native Image Plugin* to your project:
 
```groovy
plugins {
    id 'org.graalvm.plugin.native-image' version '$latest'
}
```

If you also have the `application` plugin applied and configured, you should
immediately see a `distNative` task which will run `native-image` with the same
configuration as the main application distribution and place it in `build/distributions`. 

If you need more control, you can declare a custom `NativeImage` task. `NativeImage` task
can have two types: it is either based on a main class (`forMainClass`) or an executable 
jar (`forJar`). For the jar-based tasks, ideally you should specify a Gradle task of the 
type `Jar`. However, `forJar` will also accept any `File` or a string path.

Every `NativeImage` task can be then configured with `outputDir` (defaults to 
`$buildDir/nativeImage`), `outputName` (defaults to task name), `classpath` (defaults
to runtime classpath) and extra `cmdArgs` for the `native-image` tool:

```groovy
import com.oracle.truffle.gradle.NativeImage

// A custom NativeImage task based on a main class. 
task compileAppFromMainClass(type: NativeImage) {
    forMainClass "my.app.Application"
    outputDir "$buildDir/bin"
    outputName "awesome-app"
    // Classpath:
    // `appendClasspath` adds extra items to the end of the list; 
    // `classpath` completely replaces all items. 
    appendClasspath "libs/extra-dependency-1.jar", "libs/extra-dependency-2.jar"
    // Similar to classpath, there is `cmdArgs` and `appendCmdArgs`:
    cmdArgs "--extra-cmd-arg"
}

// A custom NativeImage task based on a jar file.
task compileAppFromJar(type: NativeImage) {
    forJar customJar
    /* ... some configuration ... */
}

task customJar(type: Jar) {
    /* ... some configuration ... */
}
```
