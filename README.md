# Graal & Truffle Gradle Plugins
 
This repository contains several useful plugins for developing polyglot 
applications using the Graal compiler. Currently, there are three main
plugins, each focusing on a specific sub-part of Graal functionality:

 - *Graal Compiler Plugin:* Configures the project and its distributions 
 to use Graal compiler when possible, even on standard Hotspot JVMs. This
 also allows usage of Graal scripting languages and polyglot APIs on 
 Hotspot JVMs.
 - *Native Image Plugin:* Allows declaration of `NativeImage` tasks which
 use Graal's `native-image` compiler to produce native binaries of Java
 executables. 
 - *Graal Language Plugin:* Sets-up the project for development of a custom
 Graal/Truffle language. Automatically configures Truffle classpath and 
 allows creation of packages installable with `gu`.

## Graal Compiler Plugin

To start using the *Graal Compiler Plugin*, add it to your plugin list and 
configure the desired Graal version:

```
plugins {
    id 'org.graalvm.plugin.compiler'
}

graal {
    version '20.1.0'
}
```

*Note that only Hotspot JVMs use this declared version of the compiler. GraalVM
naturally uses its built-in compiler.*

With this setup, any `JavaExec` task or a distribution created by the 
`application` plugin will automatically use the Graal compiler if possible
(only supported from version 11+).

Furthermore, we can declare language dependencies. We differentiate `language`
and `installedLanguage` dependencies. On Hotspot, these essentially correspond
to `runtime` dependencies, as Hotspot does not support installation of Graal 
languages. On GraalVM, we assume `installedLanguage` is already installed using `gu`
and are thus not loaded explicitly. However, we load any `language` dependency
explicitly using `truffle.class.path.append` to ensure the language works even
when not installed:

```groovy
dependencies {
    // JS is typically part of GraalVM, so we 
    // can assume it will be installed.
    installedLanguage 'org.graalvm.js:js:20.1.0'
    // Some custom, domain specific language may not be
    // installed, we thus load it explicitly.
    language 'org.company:my-awesome-language:1.2.3'
}
```