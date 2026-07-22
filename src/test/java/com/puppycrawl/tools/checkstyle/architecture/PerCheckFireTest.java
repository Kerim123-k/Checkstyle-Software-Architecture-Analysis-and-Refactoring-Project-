///////////////////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code and other text files for adherence to a set of rules.
// Copyright (C) 2001-2026 the original author or authors.
//
// Licensed under the GNU Lesser General Public License v2.1.
///////////////////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean.OutputStreamOptions;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.DefaultLogger;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.Configuration;

/**
 * Asserts every check in the metrics + sizes slice fires at least once
 * against the bundled sample input.
 *
 * <p>Implements SC-005 / T070. Drives one Checker run with bench-config.xml
 * then asserts the output contains a violation tagged with each check's
 * short name.
 */
public class PerCheckFireTest {

    private static final Path REPO_ROOT = Paths.get("").toAbsolutePath();
    private static final Path CONFIG = REPO_ROOT.resolve("bench-config.xml");
    private static final Path SAMPLE =
            REPO_ROOT.resolve("violation-sample/SampleAllViolations.java");

    private static String cachedOutput;

    static Stream<String> sliceCheckNames() {
        return Stream.of(
                "AnonInnerLength",
                "BooleanExpressionComplexity",
                "ClassDataAbstractionCoupling",
                "ClassFanOutComplexity",
                "CyclomaticComplexity",
                "ExecutableStatementCount",
                "FileLength",
                "JavaNCSS",
                "LambdaBodyLength",
                "LineLength",
                "MethodCount",
                "MethodLength",
                "NPathComplexity",
                "OuterTypeNumber",
                "ParameterNumber",
                "RecordComponentNumber");
    }

    @ParameterizedTest(name = "{0} fires at least once")
    @MethodSource("sliceCheckNames")
    public void checkFiresAtLeastOnce(String checkName) throws Exception {
        final String output = output();
        assertThat(output)
                .as("%s must produce at least one violation on the bundled sample",
                        checkName)
                .contains("[" + checkName + "]");
    }

    private static synchronized String output() throws Exception {
        if (cachedOutput == null) {
            final Configuration config = ConfigurationLoader.loadConfiguration(
                    CONFIG.toString(),
                    new PropertiesExpander(System.getProperties()));

            final Checker checker = new Checker();
            checker.setModuleClassLoader(Checker.class.getClassLoader());
            checker.configure(config);

            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final AuditListener logger = new DefaultLogger(out, OutputStreamOptions.NONE);
            checker.addListener(logger);

            checker.process(Collections.singletonList(new File(SAMPLE.toString())));
            checker.destroy();

            cachedOutput = out.toString(StandardCharsets.UTF_8);
        }
        return cachedOutput;
    }
}
