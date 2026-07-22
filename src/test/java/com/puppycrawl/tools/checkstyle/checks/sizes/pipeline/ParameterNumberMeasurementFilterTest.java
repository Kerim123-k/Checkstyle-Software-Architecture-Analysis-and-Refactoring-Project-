package com.puppycrawl.tools.checkstyle.checks.sizes.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.Measurement;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.SingletonPipe;

class ParameterNumberMeasurementFilterTest {

    @Test
    void emitsMeasurementForParameters() {
        final DetailAstImpl methodDef = node(TokenTypes.METHOD_DEF, 1, 0);

        final DetailAstImpl ident = node(TokenTypes.IDENT, 1, 4);
        methodDef.addChild(ident);

        final DetailAstImpl params = node(TokenTypes.PARAMETERS, 1, 10);
        params.addChild(node(TokenTypes.PARAMETER_DEF, 1, 11));
        params.addChild(node(TokenTypes.PARAMETER_DEF, 1, 15));
        methodDef.addChild(params);

        final ParameterNumberMeasurementFilter filter =
                new ParameterNumberMeasurementFilter(1, false, Collections.emptySet(), "maxParam");
        
        final SingletonPipe<AstEvent> in = new SingletonPipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        in.write(new AstEvent(methodDef, AstEvent.Phase.VISIT));
        filter.process(in, out);

        final Measurement m = out.read();
        assertNotNull(m);
        assertEquals(2, m.getValue()); // 2 parameters
        assertEquals("maxParam", m.getMessageKey());
        assertEquals(1, m.getLineNo());
        assertEquals(4, m.getColNo()); // ident col
    }

    @Test
    void ignoresLeavePhase() {
        final DetailAstImpl methodDef = node(TokenTypes.METHOD_DEF, 1, 0);
        final ParameterNumberMeasurementFilter filter =
                new ParameterNumberMeasurementFilter(1, false, Collections.emptySet(), "maxParam");
        final SingletonPipe<AstEvent> in = new SingletonPipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        in.write(new AstEvent(methodDef, AstEvent.Phase.LEAVE));
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
