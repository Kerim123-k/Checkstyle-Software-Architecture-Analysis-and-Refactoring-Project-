package com.puppycrawl.tools.checkstyle.checks.pipeline.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.DetailAstImpl;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.AstEvent;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;

class TokenFilterTest {

    @Test
    void forwardsAllowedTokens() {
        final TokenFilter filter = new TokenFilter(TokenTypes.CLASS_DEF, TokenTypes.METHOD_DEF);
        final QueuePipe<AstEvent> in = new QueuePipe<>();
        final QueuePipe<AstEvent> out = new QueuePipe<>();
        
        in.write(new AstEvent(node(TokenTypes.CLASS_DEF), AstEvent.Phase.VISIT));
        in.write(new AstEvent(node(TokenTypes.VARIABLE_DEF), AstEvent.Phase.VISIT));
        in.write(new AstEvent(node(TokenTypes.METHOD_DEF), AstEvent.Phase.LEAVE));
        
        filter.process(in, out);
        
        assertTrue(out.hasNext());
        AstEvent e1 = out.read();
        assertEquals(TokenTypes.CLASS_DEF, e1.getNode().getType());
        
        assertTrue(out.hasNext());
        AstEvent e2 = out.read();
        assertEquals(TokenTypes.METHOD_DEF, e2.getNode().getType());
        
        assertFalse(out.hasNext());
    }

    @Test
    void forwardsTreeLifecyclePhases() {
        final TokenFilter filter = new TokenFilter(TokenTypes.CLASS_DEF);
        final QueuePipe<AstEvent> in = new QueuePipe<>();
        final QueuePipe<AstEvent> out = new QueuePipe<>();
        
        in.write(new AstEvent(null, AstEvent.Phase.BEGIN_TREE));
        in.write(new AstEvent(null, AstEvent.Phase.FINISH_TREE));
        
        filter.process(in, out);
        
        assertTrue(out.hasNext());
        assertEquals(AstEvent.Phase.BEGIN_TREE, out.read().getPhase());
        
        assertTrue(out.hasNext());
        assertEquals(AstEvent.Phase.FINISH_TREE, out.read().getPhase());
        
        assertFalse(out.hasNext());
    }

    private static DetailAstImpl node(int type) {
        final DetailAstImpl n = new DetailAstImpl();
        n.setType(type);
        return n;
    }
}
