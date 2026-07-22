package com.puppycrawl.tools.checkstyle.checks.sizes.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.Measurement;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.SingletonPipe;

class OuterTypeNumberMeasurementFilterTest {

    @Test
    void countsOuterTypes() {
        final DetailAstImpl root = node(TokenTypes.COMPILATION_UNIT, 1, 0);
        final DetailAstImpl classDef1 = node(TokenTypes.CLASS_DEF, 2, 0);
        final DetailAstImpl classDef2 = node(TokenTypes.CLASS_DEF, 10, 0);

        final OuterTypeNumberMeasurementFilter filter =
                new OuterTypeNumberMeasurementFilter(1, "maxOuterTypes");
        
        final QueuePipe<AstEvent> in = new QueuePipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        
        in.write(new AstEvent(root, AstEvent.Phase.BEGIN_TREE));
        
        in.write(new AstEvent(classDef1, AstEvent.Phase.VISIT));
        // Inner class inside classDef1
        in.write(new AstEvent(node(TokenTypes.CLASS_DEF, 3, 0), AstEvent.Phase.VISIT));
        in.write(new AstEvent(node(TokenTypes.CLASS_DEF, 3, 0), AstEvent.Phase.LEAVE));
        in.write(new AstEvent(classDef1, AstEvent.Phase.LEAVE));

        in.write(new AstEvent(classDef2, AstEvent.Phase.VISIT));
        in.write(new AstEvent(classDef2, AstEvent.Phase.LEAVE));
        
        in.write(new AstEvent(root, AstEvent.Phase.FINISH_TREE));
        
        filter.process(in, out);

        final Measurement m = out.read();
        assertNotNull(m);
        assertEquals(2, m.getValue()); // 2 outer types
        assertEquals("maxOuterTypes", m.getMessageKey());
    }

    private static DetailAstImpl node(int type, int line, int col) {
        final DetailAstImpl n = new DetailAstImpl();
        n.setType(type);
        n.setLineNo(line);
        n.setColumnNo(col);
        return n;
    }
}
