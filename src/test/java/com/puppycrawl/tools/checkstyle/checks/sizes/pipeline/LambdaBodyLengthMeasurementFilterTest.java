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

class LambdaBodyLengthMeasurementFilterTest {

    @Test
    void measuresLambdaWithBlock() {
        final DetailAstImpl parent = node(TokenTypes.EXPR, 1, 0);
        final DetailAstImpl lambda = node(TokenTypes.LAMBDA, 1, 0);
        parent.addChild(lambda);

        final DetailAstImpl slist = node(TokenTypes.SLIST, 1, 10);
        final DetailAstImpl rcurly = node(TokenTypes.RCURLY, 5, 0);
        slist.addChild(rcurly);
        lambda.addChild(slist);

        final LambdaBodyLengthMeasurementFilter filter =
                new LambdaBodyLengthMeasurementFilter(3, "maxLambdaBody");
        
        final SingletonPipe<AstEvent> in = new SingletonPipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        in.write(new AstEvent(lambda, AstEvent.Phase.VISIT));
        filter.process(in, out);

        final Measurement m = out.read();
        assertNotNull(m);
        assertEquals(5, m.getValue()); // 5 - 1 + 1 = 5
        assertEquals("maxLambdaBody", m.getMessageKey());
    }

    @Test
    void ignoresLeavePhase() {
        final DetailAstImpl lambda = node(TokenTypes.LAMBDA, 1, 0);
        final LambdaBodyLengthMeasurementFilter filter =
                new LambdaBodyLengthMeasurementFilter(3, "maxLambdaBody");
        final SingletonPipe<AstEvent> in = new SingletonPipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        in.write(new AstEvent(lambda, AstEvent.Phase.LEAVE));
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
