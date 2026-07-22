///////////////////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code and other text files for adherence to a set of rules.
// Copyright (C) 2001-2026 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
///////////////////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.checks.sizes;

import java.io.File;
import java.util.regex.Pattern;

import com.puppycrawl.tools.checkstyle.StatelessCheck;
import com.puppycrawl.tools.checkstyle.api.AbstractFileSetCheck;
import com.puppycrawl.tools.checkstyle.api.FileText;
import com.puppycrawl.tools.checkstyle.checks.pipeline.Pipeline;
import com.puppycrawl.tools.checkstyle.checks.pipeline.PipelineBuilder;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.IgnorePatternFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.LineSplitterFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.ThresholdFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.ViolationSink;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.ViolationMessage;
import com.puppycrawl.tools.checkstyle.checks.sizes.pipeline.LineLengthMeasurementFilter;

/**
 * <div>
 * Checks for long lines.
 * </div>
 *
 * <p>
 * Rationale: Long lines are hard to read in printouts or if developers
 * have limited screen space for the source code, e.g. if the IDE displays
 * additional information like project tree, class hierarchy, etc.
 * </p>
 * <ul>
 * <li>
 * Notes:
 * The calculation of the length of a line takes into account the number of
 * expanded spaces for a tab character ({@code '\t'}). The default number of spaces is {@code 8}.
 * To specify a different number of spaces, the user can set
 * <a href="https://checkstyle.org/config.html#Checker">{@code Checker}</a>
 * property {@code tabWidth} which applies to all Checks, including {@code LineLength};
 * or can set property {@code tabWidth} for {@code LineLength} alone.
 * </li>
 * <li>
 * By default, package and import statements (lines matching pattern {@code ^(package|import) .*})
 * are not verified by this check.
 * </li>
 * <li>
 * Trailing comments are taken into consideration while calculating the line length.
 * <div class="wrapper"><pre class="prettyprint"><code class="language-java">
 * import java.util.regex.Pattern; // The length of this comment will be taken into consideration
 * </code></pre></div>
 * In the example above the length of the import statement is just 31 characters but total length
 * will be 94 characters.
 * </li>
 * </ul>
 *
 * @since 3.0
 */
@StatelessCheck
public class LineLengthCheck extends AbstractFileSetCheck {

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_KEY = "maxLineLen";

    /** Default maximum number of columns in a line. */
    private static final int DEFAULT_MAX_COLUMNS = 80;

    /** Specify the maximum line length allowed. */
    private int max = DEFAULT_MAX_COLUMNS;

    /** Specify pattern for lines to ignore. */
    private Pattern ignorePattern = Pattern.compile("^(package|import) .*");

    @Override
    protected void processFiltered(File file, FileText fileText) {
        final Pipeline<FileText, ViolationMessage> pipeline = PipelineBuilder.<FileText>start()
                .addQueued(new LineSplitterFilter())
                .addQueued(new IgnorePatternFilter(ignorePattern))
                .addQueued(new LineLengthMeasurementFilter(getTabWidth(), max, MSG_KEY))
                .addQueued(new ThresholdFilter(max))
                .addQueued(new ViolationSink())
                .build();

        pipeline.submit(fileText);

        while (pipeline.hasResults()) {
            final ViolationMessage v = pipeline.drain();
            log(v.getLine(), v.getMessageKey(), v.getArgs());
        }
    }

    /**
     * Setter to specify the maximum line length allowed.
     *
     * @param length the maximum length of a line
     * @since 3.0
     */
    public void setMax(int length) {
        max = length;
    }

    /**
     * Setter to specify pattern for lines to ignore.
     *
     * @param pattern a pattern.
     * @since 3.0
     */
    public final void setIgnorePattern(Pattern pattern) {
        ignorePattern = pattern;
    }

    /**
     * Setter to specify the file extensions of the files to process.
     *
     * @param extensions the set of file extensions. A missing
     *         initial '.' character of an extension is automatically added.
     * @throws IllegalArgumentException is argument is null
     * @propertySince 8.24
     */
    @Override
    public final void setFileExtensions(String... extensions) {
        super.setFileExtensions(extensions);
    }

}
