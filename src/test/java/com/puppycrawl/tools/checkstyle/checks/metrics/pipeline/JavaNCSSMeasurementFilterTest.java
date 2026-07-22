package com.puppycrawl.tools.checkstyle.checks.metrics.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.ViolationMessage;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;

class JavaNCSSMeasurementFilterTest {

    @Test
    void countsJavaNcss() {
        final DetailAstImpl root = node(TokenTypes.COMPILATION_UNIT, 1, 0);
        final DetailAstImpl classDef = node(TokenTypes.CLASS_DEF, 2, 0);
        final DetailAstImpl objBlock = node(TokenTypes.OBJBLOCK, 2, 10);
        final DetailAstImpl varDef = node(TokenTypes.VARIABLE_DEF, 3, 0);

        classDef.addChild(objBlock);
        objBlock.addChild(varDef);

        final JavaNCSSMeasurementFilter filter =
                new JavaNCSSMeasurementFilter(0, 0, 0, 0,
                        "msgMethod", "msgClass", "msgRecord", "msgFile");
        
        final QueuePipe<AstEvent> in = new QueuePipe<>();
        final QueuePipe<ViolationMessage> out = new QueuePipe<>();
        
        in.write(new AstEvent(root, AstEvent.Phase.BEGIN_TREE));
        in.write(new AstEvent(classDef, AstEvent.Phase.VISIT));
        in.write(new AstEvent(varDef, AstEvent.Phase.VISIT));
        in.write(new AstEvent(varDef, AstEvent.Phase.LEAVE));
        in.write(new AstEvent(classDef, AstEvent.Phase.LEAVE));
        in.write(new AstEvent(root, AstEvent.Phase.FINISH_TREE));
        
        filter.process(in, out);

        assertTrue(out.hasNext());
        final ViolationMessage mClass = out.read();
        assertNotNull(mClass);
        assertEquals("msgClass", mClass.getMessageKey());
        assertEquals(2, mClass.getArgs()[0]);

        assertTrue(out.hasNext());
        final ViolationMessage mFile = out.read();
        assertNotNull(mFile);
        assertEquals("msgFile", mFile.getMessageKey());
        assertEquals(2, mFile.getArgs()[0]);
        
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
