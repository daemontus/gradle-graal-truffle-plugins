package com.oracle.truffle.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class LanguageFunctionalTest extends AbstractFunctionalTest {

    public LanguageFunctionalTest() {
        super("src/functionalTest/project/test-language");
    }

    @Test
    public void canRunCustomLanguage() {
        cleanProject();
        runBuild("run", "--args='identity'");
        cleanProject();
    }

    @Test
    public void customLanguageCanFail() {
        cleanProject();
        runFailingBuild("run", "--args='some stupid value'");
        cleanProject();
    }

    @Test
    public void buildGraalBundle() {
        cleanProject();
        runBuild("graalComponent");
    }


}
