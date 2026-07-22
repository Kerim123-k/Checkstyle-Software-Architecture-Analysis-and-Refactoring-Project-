package com.puppycrawl.tools.checkstyle.checks.sizes.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.Measurement;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.SingletonPipe;

class ExecutableStatementCountMeasurementFilterTest {

    @Test
    void countsExecutableStatements() {
        final DetailAstImpl methodDef = node(TokenTypes.METHOD_DEF, 1, 0);

        final DetailAstImpl slist = node(TokenTypes.SLIST, 1, 10);
        
        // Add 2 children to SLIST (represents 1 statement and 1 semi)
        slist.addChild(node(TokenTypes.EXPR, 2, 0));
        slist.addChild(node(TokenTypes.SEMI, 2, 5));
        
        // Add 2 more children to SLIST (represents another statement)
        slist.addChild(node(TokenTypes.EXPR, 3, 0));
        slist.addChild(node(TokenTypes.SEMI, 3, 5));

        methodDef.addChild(slist);

        final ExecutableStatementCountMeasurementFilter filter =
                new ExecutableStatementCountMeasurementFilter(1, "maxStatements");
        
        final QueuePipe<AstEvent> in = new QueuePipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        
        in.write(new AstEvent(methodDef, AstEvent.Phase.VISIT));
        in.write(new AstEvent(slist, AstEvent.Phase.VISIT));
        in.write(new AstEvent(slist, AstEvent.Phase.LEAVE));
        in.write(new AstEvent(methodDef, AstEvent.Phase.LEAVE));
        
        filter.process(in, out);

        final Measurement m = out.read();
        assertNotNull(m);
        assertEquals(2, m.getValue()); // 4 children / 2 = 2 statements
        assertEquals("maxStatements", m.getMessageKey());
        
        assertFalse(out.hasNext());
    }

    private static DetailAstImpl node(int type, int line, int col) {
        final DetailAstImpl n = new DetailAstImpl();
        n.setType(type);
        n.setLineNo(line);
        n.setColumnNo(col);
        return n;
    }
}
