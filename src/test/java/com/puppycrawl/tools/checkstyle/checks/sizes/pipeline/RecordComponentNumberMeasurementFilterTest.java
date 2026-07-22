package com.puppycrawl.tools.checkstyle.checks.sizes.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.naming.AccessModifierOption;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.Measurement;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.SingletonPipe;

class RecordComponentNumberMeasurementFilterTest {

    @Test
    void emitsMeasurementForRecordComponents() {
        final DetailAstImpl recordDef = node(TokenTypes.RECORD_DEF, 1, 0);
        
        final DetailAstImpl modifiers = node(TokenTypes.MODIFIERS, 1, 0);
        modifiers.addChild(node(TokenTypes.LITERAL_PUBLIC, 1, 0));
        recordDef.addChild(modifiers);

        final DetailAstImpl components = node(TokenTypes.RECORD_COMPONENTS, 1, 10);
        components.addChild(node(TokenTypes.RECORD_COMPONENT_DEF, 1, 11));
        components.addChild(node(TokenTypes.RECORD_COMPONENT_DEF, 1, 15));
        recordDef.addChild(components);

        final RecordComponentNumberMeasurementFilter filter =
                new RecordComponentNumberMeasurementFilter(1, new AccessModifierOption[]{AccessModifierOption.PUBLIC}, "maxRecord");
        
        final SingletonPipe<AstEvent> in = new SingletonPipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        in.write(new AstEvent(recordDef, AstEvent.Phase.VISIT));
        filter.process(in, out);

        final Measurement m = out.read();
        assertNotNull(m);
        assertEquals(2, m.getValue());
        assertEquals("maxRecord", m.getMessageKey());
    }

    @Test
    void ignoresLeavePhase() {
        final DetailAstImpl recordDef = node(TokenTypes.RECORD_DEF, 1, 0);
        final RecordComponentNumberMeasurementFilter filter =
                new RecordComponentNumberMeasurementFilter(1, new AccessModifierOption[]{AccessModifierOption.PUBLIC}, "maxRecord");
        final SingletonPipe<AstEvent> in = new SingletonPipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        in.write(new AstEvent(recordDef, AstEvent.Phase.LEAVE));
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
