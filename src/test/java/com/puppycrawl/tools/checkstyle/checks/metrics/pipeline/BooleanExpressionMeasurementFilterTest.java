package com.puppycrawl.tools.checkstyle.checks.metrics.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.Measurement;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;

class BooleanExpressionMeasurementFilterTest {

    @Test
    void countsBooleanExpressions() {
        final DetailAstImpl methodDef = node(TokenTypes.METHOD_DEF, 1, 0);
        final DetailAstImpl ident = node(TokenTypes.IDENT, 1, 0);
        ident.setText("myMethod");
        methodDef.addChild(node(TokenTypes.MODIFIERS, 1, 0));
        methodDef.addChild(node(TokenTypes.TYPE, 1, 0));
        methodDef.addChild(ident);
        methodDef.addChild(node(TokenTypes.PARAMETERS, 1, 0));

        final DetailAstImpl expr = node(TokenTypes.EXPR, 2, 0);
        final DetailAstImpl land = node(TokenTypes.LAND, 2, 5);
        expr.addChild(land);
        methodDef.addChild(expr);

        final BooleanExpressionMeasurementFilter filter =
                new BooleanExpressionMeasurementFilter(0, "maxBool");
        
        final QueuePipe<AstEvent> in = new QueuePipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        
        in.write(new AstEvent(methodDef, AstEvent.Phase.VISIT));
        in.write(new AstEvent(expr, AstEvent.Phase.VISIT));
        in.write(new AstEvent(land, AstEvent.Phase.VISIT));
        in.write(new AstEvent(land, AstEvent.Phase.LEAVE));
        in.write(new AstEvent(expr, AstEvent.Phase.LEAVE));
        in.write(new AstEvent(methodDef, AstEvent.Phase.LEAVE));
        
        filter.process(in, out);

        assertTrue(out.hasNext());
        final Measurement m = out.read();
        assertNotNull(m);
        assertEquals(1, m.getValue()); // 1 boolean operator
        assertEquals("maxBool", m.getMessageKey());
        assertEquals(methodDef, m.getSubject()); // parentAST is methodDef
        
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
