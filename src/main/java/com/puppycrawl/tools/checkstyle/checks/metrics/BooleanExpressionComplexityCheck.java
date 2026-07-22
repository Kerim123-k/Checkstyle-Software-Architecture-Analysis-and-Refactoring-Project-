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
import com.puppycrawl.tools.checkstyle.checks.metrics.pipeline.BooleanExpressionMeasurementFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.Pipeline;
import com.puppycrawl.tools.checkstyle.checks.pipeline.PipelineBuilder;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.ThresholdFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.TokenFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.ViolationSink;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.ViolationMessage;

/**
 * <div>
 * Restricts the number of boolean operators ({@code &&}, {@code ||},
 * {@code &}, {@code |} and {@code ^}) in an expression.
 * </div>
 *
 * <p>
 * Rationale: Too many conditions leads to code that is difficult to read
 * and hence debug and maintain.
 * </p>
 *
 * <p>
 * Note that the operators {@code &} and {@code |} are not only integer bitwise
 * operators, they are also the
 * <a href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.22.2">
 * non-shortcut versions</a> of the boolean operators {@code &&} and {@code ||}.
 * </p>
 *
 * <p>
 * Note that {@code &}, {@code |} and {@code ^} are not checked if they are part
 * of constructor or method call because they can be applied to non-boolean
 * variables and Checkstyle does not know types of methods from different classes.
 * </p>
 *
 * @since 3.4
 */
@FileStatefulCheck
public final class BooleanExpressionComplexityCheck extends AbstractCheck {

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_KEY = "booleanExpressionComplexity";

    /** Default allowed complexity. */
    private static final int DEFAULT_MAX = 3;

    /** Specify the maximum number of boolean operations allowed in one expression. */
    private int max = DEFAULT_MAX;

    /** Pipeline driving the per-token measurement + threshold + sink chain. */
    private Pipeline<AstEvent, ViolationMessage> pipeline;

    @Override
    public int[] getDefaultTokens() {
        return new int[] {
            TokenTypes.CTOR_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.EXPR,
            TokenTypes.LAND,
            TokenTypes.BAND,
            TokenTypes.LOR,
            TokenTypes.BOR,
            TokenTypes.BXOR,
            TokenTypes.COMPACT_CTOR_DEF,
        };
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[] {
            TokenTypes.CTOR_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.EXPR,
            TokenTypes.COMPACT_CTOR_DEF,
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return new int[] {
            TokenTypes.CTOR_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.EXPR,
            TokenTypes.LAND,
            TokenTypes.BAND,
            TokenTypes.LOR,
            TokenTypes.BOR,
            TokenTypes.BXOR,
            TokenTypes.COMPACT_CTOR_DEF,
        };
    }

    /**
     * Setter to specify the maximum number of boolean operations allowed in one expression.
     *
     * @param max new maximum allowed complexity.
     * @since 3.4
     */
    public void setMax(int max) {
        this.max = max;
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        pipeline = PipelineBuilder.<AstEvent>start()
                .add(new TokenFilter(getAcceptableTokens()))
                .add(new BooleanExpressionMeasurementFilter(max, MSG_KEY))
                .add(new ThresholdFilter(max))
                .addQueued(new ViolationSink())
                .build();
    }

    @Override
    public void visitToken(DetailAST ast) {
        switch (ast.getType()) {
            case TokenTypes.CTOR_DEF, TokenTypes.METHOD_DEF, TokenTypes.COMPACT_CTOR_DEF,
                 TokenTypes.EXPR, TokenTypes.LAND, TokenTypes.LOR,
                 TokenTypes.BAND, TokenTypes.BOR, TokenTypes.BXOR -> { /* accepted */ }
            default -> throw new IllegalArgumentException("Unknown type: " + ast);
        }
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
