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
import com.puppycrawl.tools.checkstyle.checks.metrics.pipeline.JavaNCSSMeasurementFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.Pipeline;
import com.puppycrawl.tools.checkstyle.checks.pipeline.PipelineBuilder;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.TokenFilter;
import com.puppycrawl.tools.checkstyle.checks.pipeline.filter.ViolationSink;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.ViolationMessage;

/**
 * <div>
 * Determines complexity of methods, classes and files by counting
 * the Non Commenting Source Statements (NCSS). This check adheres to the
 * <a href="http://www.kclee.de/clemens/java/javancss/#specification">specification</a>
 * for the <a href="http://www.kclee.de/clemens/java/javancss/">JavaNCSS-Tool</a>
 * written by <b>Chr. Clemens Lee</b>.
 * </div>
 *
 * <p>
 * Roughly said the NCSS metric is calculated by counting the source lines which are
 * not comments, (nearly) equivalent to counting the semicolons and opening curly braces.
 * </p>
 *
 * @since 3.5
 */
// -@cs[AbbreviationAsWordInName] We can not change it as,
// check's name is a part of API (used in configurations).
@FileStatefulCheck
public class JavaNCSSCheck extends AbstractCheck {

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_METHOD = "ncss.method";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_CLASS = "ncss.class";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_RECORD = "ncss.record";

    /**
     * A key is pointing to the warning message text in "messages.properties"
     * file.
     */
    public static final String MSG_FILE = "ncss.file";

    /** Default constant for max file ncss. */
    private static final int FILE_MAX_NCSS = 2000;

    /** Default constant for max file ncss. */
    private static final int CLASS_MAX_NCSS = 1500;

    /** Default constant for max record ncss. */
    private static final int RECORD_MAX_NCSS = 150;

    /** Default constant for max method ncss. */
    private static final int METHOD_MAX_NCSS = 50;

    /**
     * Specify the maximum allowed number of non commenting lines in a file
     * including all top level and nested classes.
     */
    private int fileMaximum = FILE_MAX_NCSS;

    /** Specify the maximum allowed number of non commenting lines in a class. */
    private int classMaximum = CLASS_MAX_NCSS;

    /** Specify the maximum allowed number of non commenting lines in a record. */
    private int recordMaximum = RECORD_MAX_NCSS;

    /** Specify the maximum allowed number of non commenting lines in a method. */
    private int methodMaximum = METHOD_MAX_NCSS;

    /** Pipeline driving the per-token measurement + violation sink chain. */
    private Pipeline<AstEvent, ViolationMessage> pipeline;

    @Override
    public int[] getDefaultTokens() {
        return getRequiredTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return new int[] {
            TokenTypes.CLASS_DEF,
            TokenTypes.INTERFACE_DEF,
            TokenTypes.METHOD_DEF,
            TokenTypes.CTOR_DEF,
            TokenTypes.INSTANCE_INIT,
            TokenTypes.STATIC_INIT,
            TokenTypes.PACKAGE_DEF,
            TokenTypes.IMPORT,
            TokenTypes.VARIABLE_DEF,
            TokenTypes.CTOR_CALL,
            TokenTypes.SUPER_CTOR_CALL,
            TokenTypes.LITERAL_IF,
            TokenTypes.LITERAL_ELSE,
            TokenTypes.LITERAL_WHILE,
            TokenTypes.LITERAL_DO,
            TokenTypes.LITERAL_FOR,
            TokenTypes.LITERAL_SWITCH,
            TokenTypes.LITERAL_BREAK,
            TokenTypes.LITERAL_CONTINUE,
            TokenTypes.LITERAL_RETURN,
            TokenTypes.LITERAL_THROW,
            TokenTypes.LITERAL_SYNCHRONIZED,
            TokenTypes.LITERAL_CATCH,
            TokenTypes.LITERAL_FINALLY,
            TokenTypes.EXPR,
            TokenTypes.LABELED_STAT,
            TokenTypes.LITERAL_CASE,
            TokenTypes.LITERAL_DEFAULT,
            TokenTypes.RECORD_DEF,
            TokenTypes.COMPACT_CTOR_DEF,
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return getRequiredTokens();
    }

    @Override
    public void beginTree(DetailAST rootAST) {
        pipeline = PipelineBuilder.<AstEvent>start()
                .add(new TokenFilter(getRequiredTokens()))
                .addQueued(new JavaNCSSMeasurementFilter(
                        fileMaximum, classMaximum, recordMaximum, methodMaximum,
                        MSG_METHOD, MSG_CLASS, MSG_RECORD, MSG_FILE))
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

    @Override
    public void finishTree(DetailAST rootAST) {
        pipeline.submit(new AstEvent(rootAST, AstEvent.Phase.FINISH_TREE));
        drainAndLog();
    }

    /** Drain sink, forward each violation to the framework log. */
    private void drainAndLog() {
        while (pipeline.hasResults()) {
            final ViolationMessage v = pipeline.drain();
            log(v.getLine(), v.getCol(), v.getMessageKey(), v.getArgs());
        }
    }

    /**
     * Setter to specify the maximum allowed number of non commenting lines
     * in a file including all top level and nested classes.
     *
     * @param fileMaximum
     *            the maximum ncss
     * @since 3.5
     */
    public void setFileMaximum(int fileMaximum) {
        this.fileMaximum = fileMaximum;
    }

    /**
     * Setter to specify the maximum allowed number of non commenting lines in a class.
     *
     * @param classMaximum
     *            the maximum ncss
     * @since 3.5
     */
    public void setClassMaximum(int classMaximum) {
        this.classMaximum = classMaximum;
    }

    /**
     * Setter to specify the maximum allowed number of non commenting lines in a record.
     *
     * @param recordMaximum
     *            the maximum ncss
     * @since 8.36
     */
    public void setRecordMaximum(int recordMaximum) {
        this.recordMaximum = recordMaximum;
    }

    /**
     * Setter to specify the maximum allowed number of non commenting lines in a method.
     *
     * @param methodMaximum
     *            the maximum ncss
     * @since 3.5
     */
    public void setMethodMaximum(int methodMaximum) {
        this.methodMaximum = methodMaximum;
    }

}
