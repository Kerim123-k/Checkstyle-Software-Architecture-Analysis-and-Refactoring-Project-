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

class AnonInnerLengthMeasurementFilterTest {

    @Test
    void emitsMeasurementForAnonInnerWithBlock() {
        final DetailAstImpl newLiteral = node(TokenTypes.LITERAL_NEW, 1, 0);

        final DetailAstImpl objBlock = node(TokenTypes.OBJBLOCK, 1, 10);
        final DetailAstImpl rcurly = node(TokenTypes.RCURLY, 5, 0);
        objBlock.addChild(rcurly);
        
        newLiteral.addChild(objBlock);

        final AnonInnerLengthMeasurementFilter filter =
                new AnonInnerLengthMeasurementFilter(3, "maxLen.anonInner");
        final SingletonPipe<AstEvent> in = new SingletonPipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        in.write(new AstEvent(newLiteral, AstEvent.Phase.VISIT));
        filter.process(in, out);

        final Measurement m = out.read();
        assertNotNull(m);
        assertEquals(5, m.getValue()); // 5 - 1 + 1 = 5
        assertEquals("maxLen.anonInner", m.getMessageKey());
        assertEquals(1, m.getLineNo());
        assertEquals(0, m.getColNo());
    }

    @Test
    void ignoresLeavePhase() {
        final DetailAstImpl newLiteral = node(TokenTypes.LITERAL_NEW, 1, 0);
        final AnonInnerLengthMeasurementFilter filter =
                new AnonInnerLengthMeasurementFilter(3, "maxLen.anonInner");
        final SingletonPipe<AstEvent> in = new SingletonPipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        in.write(new AstEvent(newLiteral, AstEvent.Phase.LEAVE));
        filter.process(in, out);
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
