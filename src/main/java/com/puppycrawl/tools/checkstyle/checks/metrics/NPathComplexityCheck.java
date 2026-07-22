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
import com.puppycrawl.tools.checkstyle.checks.metrics.pipeline.NPathMeasurementFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.Pipeline;
import com.puppycrawl.tools.checkstyle.checks.pipeline.PipelineBuilder;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.ThresholdFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.TokenFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.ViolationSink;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.ViolationMessage;

/**
 * <div>
 * Checks the NPATH complexity against a specified limit.
 * </div>
 *
 * <p>
 * The NPATH metric computes the number of possible execution paths through a
 * function(method). It takes into account the nesting of conditional statements
 * and multipart boolean expressions (A &amp;&amp; B, C || D, E ? F :G and
 * their combinations).
 * </p>
 *
 * <p>
 * The NPATH metric was designed base on Cyclomatic complexity to avoid problem
 * of Cyclomatic complexity metric like nesting level within a function(method).
 * </p>
 *
 * <p>
 * Metric was described at <a href="http://dl.acm.org/citation.cfm?id=42379">
 * "NPATH: a measure of execution pathcomplexity and its applications"</a>.
 * If you need detailed description of algorithm, please read that article,
 * it is well written and have number of examples and details.
 * </p>
 *
 * <p>
 * <b>Rationale:</b> Nejmeh says that his group had an informal NPATH limit of
 * 200 on individual routines; functions(methods) that exceeded this value were
 * candidates for further decomposition - or at least a closer look.
 * <b>Please do not be fanatic with limit 200</b> - choose number that suites
 * your project style. Limit 200 is empirical number base on some sources of at
 * AT&amp;T Bell Laboratories of 1988 year.
 * </p>
 *
 * @since 3.4
 */
// -@cs[AbbreviationAsWordInName] Can't change check name
@FileStatefulCheck
public final class NPathComplexityCheck extends AbstractCheck {

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_KEY = "npathComplexity";

    /** Default allowed complexity. */
    private static final int DEFAULT_MAX = 200;

    /** Specify the maximum threshold allowed. */
    private int max = DEFAULT_MAX;

    /** Pipeline driving the per-token measurement + threshold + sink chain. */
    private Pipeline<AstEvent, ViolationMessage> pipeline;

    /**
     * Setter to specify the maximum threshold allowed.
     *
     * @param max the maximum threshold
     * @since 3.4
     */
    public void setMax(int max) {
        this.max = max;
    }

    @Override
    public int[] getDefaultTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getAcceptableTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[] {
            TokenTypes.CTOR_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.STATIC_INIT,
            TokenTypes.INSTANCE_INIT,
            TokenTypes.LITERAL_WHILE,
            TokenTypes.LITERAL_DO,
            TokenTypes.LITERAL_FOR,
            TokenTypes.LITERAL_IF,
            TokenTypes.LITERAL_ELSE,
            TokenTypes.LITERAL_SWITCH,
            TokenTypes.CASE_GROUP,
            TokenTypes.LITERAL_TRY,
            TokenTypes.LITERAL_CATCH,
            TokenTypes.QUESTION,
            TokenTypes.LITERAL_RETURN,
            TokenTypes.LITERAL_DEFAULT,
            TokenTypes.COMPACT_CTOR_DEF,
            TokenTypes.SWITCH_RULE,
            TokenTypes.LITERAL_WHEN,
        };
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        buildPipeline();
    }

    private void buildPipeline() {
        pipeline = PipelineBuilder.<AstEvent>start()
                .add(new TokenFilter(getRequiredTokens()))
                .add(new NPathMeasurementFilter(max, MSG_KEY))
                .add(new ThresholdFilter(max))
                .addQueued(new ViolationSink())
                .build();
    }

    @Override
    public void visitToken(DetailAST ast) {
        if (pipeline == null) {
            buildPipeline();
        }
        pipeline.submit(new AstEvent(ast, AstEvent.Phase.VISIT));
        drainAndLog();
    }

    @Override
    public void leaveToken(DetailAST ast) {
        if (pipeline == null) {
            buildPipeline();
        }
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
