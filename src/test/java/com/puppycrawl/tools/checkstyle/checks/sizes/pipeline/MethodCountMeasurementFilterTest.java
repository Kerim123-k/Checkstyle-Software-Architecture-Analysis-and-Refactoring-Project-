package com.puppycrawl.tools.checkstyle.checks.sizes.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.ViolationMessage;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;

class MethodCountMeasurementFilterTest {

    @Test
    void countsMethods() {
        final DetailAstImpl classDef = node(TokenTypes.CLASS_DEF, 1, 0);
        final DetailAstImpl objBlock = node(TokenTypes.OBJBLOCK, 2, 0);
        classDef.addChild(objBlock);
        
        final DetailAstImpl methodDef = node(TokenTypes.METHOD_DEF, 3, 0);
        final DetailAstImpl modifiers = node(TokenTypes.MODIFIERS, 3, 0);
        modifiers.addChild(node(TokenTypes.LITERAL_PUBLIC, 3, 0));
        methodDef.addChild(modifiers);
        objBlock.addChild(methodDef);

        final MethodCountMeasurementFilter filter =
                new MethodCountMeasurementFilter(0, 0, 0, 0, 0,
                        "maxPrivate", "maxPackage", "maxProtected", "maxPublic", "maxTotal");
        
        final QueuePipe<AstEvent> in = new QueuePipe<>();
        final QueuePipe<ViolationMessage> out = new QueuePipe<>();
        
        in.write(new AstEvent(classDef, AstEvent.Phase.VISIT));
        in.write(new AstEvent(methodDef, AstEvent.Phase.VISIT));
        in.write(new AstEvent(methodDef, AstEvent.Phase.LEAVE));
        in.write(new AstEvent(classDef, AstEvent.Phase.LEAVE));
        
        filter.process(in, out);

        assertTrue(out.hasNext());
        // Since all thresholds are 0, it should emit violations for Public and Total.
        // It processes in order: private, package, protected, public, total.
        final ViolationMessage m1 = out.read();
        assertEquals("maxPublic", m1.getMessageKey());
        assertEquals(1, m1.getArgs()[0]); // value
        
        assertTrue(out.hasNext());
        final ViolationMessage m2 = out.read();
        assertEquals("maxTotal", m2.getMessageKey());
        assertEquals(1, m2.getArgs()[0]); // total
        
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
