package com.oracle.truffle.gradle;

public class TestUtils {

    /**
     * <p>Returns true if the current JVM is Graal.</p>
     */
    static boolean isGraalVM() {
        // We don't want to use vendor name because GraalVM vendor also releases non-graal JVMs
        // (JVMCI enabled OpenJDK8). For Graal based on JDK11, java.vendor.version is set. For older JDKs,
        // java.vm.name should contain GraalVM as well.
        return System.getProperty("java.vendor.version", "").contains("GraalVM") || System.getProperty("java.vm.name", "").contains("GraalVM");
    }

    static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().startsWith("windows");
    }

}
