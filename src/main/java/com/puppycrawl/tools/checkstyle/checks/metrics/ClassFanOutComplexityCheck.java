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

package com.puppycrawl.tools.checkstyle.checks.metrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.puppycrawl.tools.checkstyle.FileStatefulCheck;
import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.metrics.pipeline.CouplingMeasurementFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.Pipeline;
import com.puppycrawl.tools.checkstyle.checks.pipeline.PipelineBuilder;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.ThresholdFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.TokenFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.ViolationSink;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.ViolationMessage;
import com.puppycrawl.tools.checkstyle.utils.CommonUtil;

/**
 * <div>
 * Checks the number of other types a given class/record/interface/enum/annotation
 * relies on. Also, the square of this has been shown to indicate the amount
 * of maintenance required in functional programs (on a file basis) at least.
 * </div>
 *
 * @since 3.4
 */
@FileStatefulCheck
public final class ClassFanOutComplexityCheck extends AbstractCheck {

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_KEY = "classFanOutComplexity";

    /** Default value of max value. */
    private static final int DEFAULT_MAX = 20;

    /** Specify the maximum threshold allowed. */
    private int max = DEFAULT_MAX;

    /** Specify user-configured class names to ignore. */
    private Set<String> excludedClasses = AbstractClassCouplingCheck.DEFAULT_EXCLUDED_CLASSES;

    /** Specify user-configured packages to ignore. */
    private Set<String> excludedPackages = AbstractClassCouplingCheck.DEFAULT_EXCLUDED_PACKAGES;

    /** Specify user-configured regular expressions to ignore classes. */
    private final List<Pattern> excludeClassesRegexps = new ArrayList<>();

    /** Pipeline driving the per-token measurement + threshold + sink chain. */
    private Pipeline<AstEvent, ViolationMessage> pipeline;

    /** Creates new instance of this check. */
    public ClassFanOutComplexityCheck() {
        excludeClassesRegexps.add(CommonUtil.createPattern("^$"));
    }

    @Override
    public int[] getDefaultTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[] {
            TokenTypes.PACKAGE_DEF,
            TokenTypes.IMPORT,
            TokenTypes.CLASS_DEF,
            TokenTypes.EXTENDS_CLAUSE,
            TokenTypes.IMPLEMENTS_CLAUSE,
            TokenTypes.ANNOTATION,
            TokenTypes.INTERFACE_DEF,
            TokenTypes.ENUM_DEF,
            TokenTypes.TYPE,
            TokenTypes.LITERAL_NEW,
            TokenTypes.LITERAL_THROWS,
            TokenTypes.ANNOTATION_DEF,
            TokenTypes.RECORD_DEF,
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return getRequiredTokens();
    }

    /**
     * Setter to specify the maximum threshold allowed.
     *
     * @param max allowed complexity.
     */
    public void setMax(int max) {
        this.max = max;
    }

    /**
     * Setter to specify user-configured class names to ignore.
     *
     * @param excludedClasses classes to ignore.
     * @propertySince 5.7
     */
    public void setExcludedClasses(String... excludedClasses) {
        this.excludedClasses = Set.of(excludedClasses);
    }

    /**
     * Setter to specify user-configured regular expressions to ignore classes.
     *
     * @param from array representing regular expressions of classes to ignore.
     * @propertySince 7.7
     */
    public void setExcludeClassesRegexps(Pattern... from) {
        excludeClassesRegexps.addAll(Arrays.asList(from));
    }

    /**
     * Setter to specify user-configured packages to ignore.
     *
     * @param excludedPackages packages to ignore.
     * @throws IllegalArgumentException if there are invalid identifiers among the packages.
     * @propertySince 7.7
     */
    public void setExcludedPackages(String... excludedPackages) {
        final List<String> invalidIdentifiers = Arrays.stream(excludedPackages)
            .filter(Predicate.not(CommonUtil::isName))
            .toList();
        if (!invalidIdentifiers.isEmpty()) {
            throw new IllegalArgumentException(
                "the following values are not valid identifiers: " + invalidIdentifiers);
        }
        this.excludedPackages = Set.of(excludedPackages);
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        pipeline = PipelineBuilder.<AstEvent>start()
                .add(new TokenFilter(getRequiredTokens()))
                .add(new CouplingMeasurementFilter(MSG_KEY, max,
                        excludedClasses, excludedPackages, excludeClassesRegexps))
                .add(new ThresholdFilter(max))
                .addQueued(new ViolationSink())
                .build();

        pipeline.submit(new AstEvent(rootAST, AstEvent.Phase.BEGIN_TREE));
        drainAndLog();
    }

    @Override
    public void visitToken(DetailAST ast) {
        pipeline.submit(new AstEvent(ast, AstEvent.Phase.VISIT));
        drainAndLog();
    }

    @Override
    public void leaveToken(DetailAST ast) {
        pipeline.submit(new AstEvent(ast, AstEvent.Phase.LEAVE));
        drainAndLog();
    }

    /** Drain sink, forward each violation to the framework log. */
    private void drainAndLog() {
        while (pipeline.hasResults()) {
            final ViolationMessage v = pipeline.drain();
            log(v.getLine(), v.getCol(), v.getMessageKey(), v.getArgs());
        }
    }

}
