package com.oracle.truffle.gradle;

import org.junit.Assume;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/*
    We have a test project `test-native-image` which defines a simple application that will try to run
    som JavaScript and then test that it is running in AOT compiled mode. We then use the default `distNative`
    task and also a more customized `myBinary` to build two native executables. We test that we can run both
    executables without problems.
 */
public class NativeImageFunctionalTest extends AbstractFunctionalTest {

    public NativeImageFunctionalTest() {
        super("src/functionalTest/project/test-native-image");
    }

    @Test
    public void canExecuteMyBinaryForClass() throws InterruptedException, IOException {
        Assume.assumeTrue(TestUtils.isGraalVM());
        cleanProject();
        runBuild("myBinaryClass");
        String binary = "src/functionalTest/project/test-native-image/build/nativeImage/myBinaryClass";
        Process app = Runtime.getRuntime().exec(binary);
        assertEquals(0, app.waitFor());
        cleanProject();
    }

    @Test
    public void canExecuteMyBinaryForJar() throws InterruptedException, IOException {
        Assume.assumeTrue(TestUtils.isGraalVM());
        cleanProject();
        runBuild("myBinaryJar");
        String binary = "src/functionalTest/project/test-native-image/build/nativeImage/myBinaryJar";
        Process app = Runtime.getRuntime().exec(binary);
        assertEquals(0, app.waitFor());
        cleanProject();
    }

    @Test
    public void canExecuteNativeApp() throws IOException, InterruptedException {
        Assume.assumeTrue(TestUtils.isGraalVM());
        cleanProject();
        runBuild("distNative");
        String binary = "src/functionalTest/project/test-native-image/build/distributions/customBinaryName";
        Process app = Runtime.getRuntime().exec(binary);
        assertEquals(0, app.waitFor());
        cleanProject();
    }

}
