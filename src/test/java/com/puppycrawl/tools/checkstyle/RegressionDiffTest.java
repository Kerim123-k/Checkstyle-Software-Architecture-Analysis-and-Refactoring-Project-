///////////////////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code and other text files for adherence to a set of rules.
// Copyright (C) 2001-2026 the original author or authors.
//
// Licensed under the GNU Lesser General Public License v2.1.
///////////////////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.AbstractAutomaticBean.OutputStreamOptions;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.Configuration;

/**
 * Asserts the refactored slice produces output equivalent to the pinned
 * baseline captured in {@code pre-refactor-output.txt}.
 *
 * <p>Implements FR-007 / SC-001 / T069. Path normalization is applied so the
 * comparison is host-independent.
 */
public class RegressionDiffTest {

    private static final Path REPO_ROOT = Paths.get("").toAbsolutePath();
    private static final Path CONFIG = REPO_ROOT.resolve("bench-config.xml");
    private static final Path SAMPLE =
            REPO_ROOT.resolve("violation-sample/SampleAllViolations.java");
    private static final Path BASELINE = REPO_ROOT.resolve("pre-refactor-output.txt");

    @Test
    public void outputMatchesBaseline() throws Exception {
        final String actual = runCheckstyle();
        final String expected = Files.readString(BASELINE, StandardCharsets.UTF_8);

        assertThat(normalize(actual))
                .as("Refactored slice must produce baseline-equivalent output")
                .isEqualTo(normalize(expected));
    }

    private static String runCheckstyle() throws Exception {
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

        return out.toString(StandardCharsets.UTF_8);
    }

    private static String normalize(String text) {
        return text.replace(REPO_ROOT.toString(), "${ROOT}")
                .replace("\r\n", "\n")
                .trim();
    }
}
