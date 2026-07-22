package com.puppycrawl.tools.checkstyle.checks.metrics.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.Measurement;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;

class CyclomaticMeasurementFilterTest {

    @Test
    void countsCyclomaticComplexity() {
        final DetailAstImpl methodDef = node(TokenTypes.METHOD_DEF, 1, 0);
        final DetailAstImpl ifToken = node(TokenTypes.LITERAL_IF, 2, 0);
        methodDef.addChild(ifToken);

        final CyclomaticMeasurementFilter filter =
                new CyclomaticMeasurementFilter(false, 0, "maxComplexity");
        
        final QueuePipe<AstEvent> in = new QueuePipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        
        in.write(new AstEvent(methodDef, AstEvent.Phase.VISIT));
        in.write(new AstEvent(ifToken, AstEvent.Phase.VISIT));
        in.write(new AstEvent(ifToken, AstEvent.Phase.LEAVE));
        in.write(new AstEvent(methodDef, AstEvent.Phase.LEAVE));
        
        filter.process(in, out);

        final Measurement m = out.read();
        assertNotNull(m);
        assertEquals(2, m.getValue()); // initial 1 + 1 (ifToken)
        assertEquals("maxComplexity", m.getMessageKey());
        
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
