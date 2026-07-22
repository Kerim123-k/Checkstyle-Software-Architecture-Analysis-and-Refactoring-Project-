///////////////////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code and other text files for adherence to a set of rules.
// Copyright (C) 2001-2026 the original author or authors.
//
// Licensed under the GNU Lesser General Public License v2.1.
///////////////////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Asserts measurement-filter unit tests run in isolation from the Checkstyle
 * execution framework — no test under the {@code .pipeline} test packages may
 * instantiate or otherwise depend on {@code TreeWalker} or {@code Checker}.
 *
 * <p>Implements US4 / T095. Backs the spec promise that filters can be
 * exercised with hand-built {@code AstEvent} streams alone.
 */
public class FilterIsolationArchTest {

    private static final JavaClasses TEST_CLASSES = new ClassFileImporter()
            .withImportOption(location ->
                location.contains("/test-classes/")
                    || location.contains("\\test-classes\\"))
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
            .importPackages(
                "com.puppycrawl.tools.checkstyle.checks.pipeline",
                "com.puppycrawl.tools.checkstyle.checks.metrics.pipeline",
                "com.puppycrawl.tools.checkstyle.checks.sizes.pipeline");

    @Test
    public void filterTestsDoNotDependOnTreeWalker() {
        noClasses().that().haveSimpleNameEndingWith("Test")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                "com.puppycrawl.tools.checkstyle.TreeWalker")
            .check(TEST_CLASSES);
    }

    @Test
    public void filterTestsDoNotDependOnChecker() {
        noClasses().that().haveSimpleNameEndingWith("Test")
            .should().dependOnClassesThat().haveFullyQualifiedName(
                "com.puppycrawl.tools.checkstyle.Checker")
            .check(TEST_CLASSES);
    }
}
