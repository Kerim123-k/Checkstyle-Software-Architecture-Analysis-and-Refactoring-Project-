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

import com.puppycrawl.tools.checkstyle.FileStatefulCheck;
import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.metrics.pipeline.CyclomaticMeasurementFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.Pipeline;
import com.puppycrawl.tools.checkstyle.checks.pipeline.PipelineBuilder;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.ThresholdFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.TokenFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.ViolationSink;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.ViolationMessage;

/**
 * <div>
 * Checks cyclomatic complexity against a specified limit. It is a measure of
 * the minimum number of possible paths through the source and therefore the
 * number of required tests, it is not about quality of code! It is only
 * applied to methods, c-tors,
 * <a href="https://docs.oracle.com/javase/tutorial/java/javaOO/initial.html">
 * static initializers and instance initializers</a>.
 * </div>
 *
 * <p>
 * The complexity is equal to the number of decision points {@code + 1}.
 * Decision points:
 * </p>
 * <ul>
 * <li>
 * {@code if}, {@code while}, {@code do}, {@code for},
 * {@code ?:}, {@code catch}, {@code switch}, {@code case} statements.
 * </li>
 * <li>
 *  Operators {@code &&} and {@code ||} in the body of target.
 * </li>
 * <li>
 *  {@code when} expression in case labels, also known as guards.
 * </li>
 * </ul>
 *
 * <p>
 * By pure theory level 1-4 is considered easy to test, 5-7 OK, 8-10 consider
 * re-factoring to ease testing, and 11+ re-factor now as testing will be painful.
 * </p>
 *
 * <p>
 * When it comes to code quality measurement by this metric level 10 is very
 * good level as a ultimate target (that is hard to archive). Do not be ashamed
 * to have complexity level 15 or even higher, but keep it below 20 to catch
 * really bad-designed code automatically.
 * </p>
 *
 * <p>
 * Please use Suppression to avoid violations on cases that could not be split
 * in few methods without damaging readability of code or encapsulation.
 * </p>
 *
 * @since 3.2
 */
@FileStatefulCheck
public class CyclomaticComplexityCheck
    extends AbstractCheck {

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_KEY = "cyclomaticComplexity";

    /** Default allowed complexity. */
    private static final int DEFAULT_COMPLEXITY_VALUE = 10;

    /** Control whether to treat the whole switch block as a single decision point. */
    private boolean switchBlockAsSingleDecisionPoint;

    /** Specify the maximum threshold allowed. */
    private int max = DEFAULT_COMPLEXITY_VALUE;

    /** Pipeline driving the per-token measurement + threshold + sink chain. */
    private Pipeline<AstEvent, ViolationMessage> pipeline;

    /**
     * Setter to control whether to treat the whole switch block as a single decision point.
     *
     * @param switchBlockAsSingleDecisionPoint whether to treat the whole switch
     *                                          block as a single decision point.
     * @since 6.11
     */
    public void setSwitchBlockAsSingleDecisionPoint(boolean switchBlockAsSingleDecisionPoint) {
        this.switchBlockAsSingleDecisionPoint = switchBlockAsSingleDecisionPoint;
    }

    /**
     * Setter to specify the maximum threshold allowed.
     *
     * @param max the maximum threshold
     * @since 3.2
     */
    public final void setMax(int max) {
        this.max = max;
    }

    @Override
    public int[] getDefaultTokens() {
        return new int[] {
            TokenTypes.CTOR_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.INSTANCE_INIT,
            TokenTypes.STATIC_INIT,
            TokenTypes.LITERAL_WHILE,
            TokenTypes.LITERAL_DO,
            TokenTypes.LITERAL_FOR,
            TokenTypes.LITERAL_IF,
            TokenTypes.LITERAL_SWITCH,
            TokenTypes.LITERAL_CASE,
            TokenTypes.LITERAL_CATCH,
            TokenTypes.QUESTION,
            TokenTypes.LAND,
            TokenTypes.LOR,
            TokenTypes.COMPACT_CTOR_DEF,
            TokenTypes.LITERAL_WHEN,
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[] {
            TokenTypes.CTOR_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.INSTANCE_INIT,
            TokenTypes.STATIC_INIT,
            TokenTypes.LITERAL_WHILE,
            TokenTypes.LITERAL_DO,
            TokenTypes.LITERAL_FOR,
            TokenTypes.LITERAL_IF,
            TokenTypes.LITERAL_SWITCH,
            TokenTypes.LITERAL_CASE,
            TokenTypes.LITERAL_CATCH,
            TokenTypes.QUESTION,
            TokenTypes.LAND,
            TokenTypes.LOR,
            TokenTypes.COMPACT_CTOR_DEF,
            TokenTypes.LITERAL_WHEN,
        };
    }

    @Override
    public final int[] getRequiredTokens() {
        return new int[] {
            TokenTypes.CTOR_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.INSTANCE_INIT,
            TokenTypes.STATIC_INIT,
            TokenTypes.COMPACT_CTOR_DEF,
        };
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        pipeline = PipelineBuilder.<AstEvent>start()
                .add(new TokenFilter(getAcceptableTokens()))
                .add(new CyclomaticMeasurementFilter(
                        switchBlockAsSingleDecisionPoint, max, MSG_KEY))
                .add(new ThresholdFilter(max))
                .addQueued(new ViolationSink())
                .build();
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
