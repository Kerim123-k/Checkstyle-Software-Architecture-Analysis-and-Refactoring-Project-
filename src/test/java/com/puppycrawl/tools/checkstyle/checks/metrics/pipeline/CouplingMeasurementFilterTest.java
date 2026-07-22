///////////////////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code and other text files for adherence to a set of rules.
// Copyright (C) 2001-2026 the original author or authors.
//
// Licensed under the GNU Lesser General Public License v2.1.
///////////////////////////////////////////////////////////////////////////////////////////////

package com.puppycrawl.tools.checkstyle.checks.metrics.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.Measurement;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;

/**
 * Implements T067 for the parameterized {@code CouplingMeasurementFilter}.
 * Drives the filter with a hand-built event stream representing a single
 * class with two referenced types and asserts the emitted Measurement.
 */
class CouplingMeasurementFilterTest {

    @Test
    void emitsMeasurementOnLeaveClassDef() {
        final DetailAstImpl classDef = node(TokenTypes.CLASS_DEF, 1, 0);
        final DetailAstImpl ident = node(TokenTypes.IDENT, 1, 6);
        ident.setText("Foo");
        classDef.addChild(ident);

        final CouplingMeasurementFilter filter = new CouplingMeasurementFilter(
                "msg", 1, Set.of(), Collections.emptySet(),
                List.of(Pattern.compile("^$")));

        final QueuePipe<AstEvent> in = new QueuePipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();

        in.write(new AstEvent(classDef, AstEvent.Phase.BEGIN_TREE));
        in.write(new AstEvent(classDef, AstEvent.Phase.VISIT));
        in.write(new AstEvent(classDef, AstEvent.Phase.LEAVE));

        filter.process(in, out);

        final Measurement m = out.read();
        assertNotNull(m, "filter must emit a Measurement on LEAVE of class def");
        assertEquals("msg", m.getMessageKey());
        assertEquals(0, m.getValue(),
                "no referenced classes were registered, so count is 0");
        assertNull(out.read(), "only one measurement expected");
    }

    private static DetailAstImpl node(int type, int line, int col) {
        final DetailAstImpl n = new DetailAstImpl();
        n.setType(type);
        n.setLineNo(line);
        n.setColumnNo(col);
        return n;
    }
}
