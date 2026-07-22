package com.puppycrawl.tools.checkstyle.checks.pipeline.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.checks.pipeline.message.FileLine;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;

class IgnorePatternFilterTest {
    @Test
    void ignoresMatchingLines() {
        final IgnorePatternFilter filter = new IgnorePatternFilter(Pattern.compile("^ignore"));
        final QueuePipe<FileLine> in = new QueuePipe<>();
        final QueuePipe<FileLine> out = new QueuePipe<>();
        
        in.write(new FileLine(1, "keep this"));
        in.write(new FileLine(2, "ignore this"));
        in.write(new FileLine(3, "also keep this"));
        
        filter.process(in, out);
        
        assertTrue(out.hasNext());
        FileLine l1 = out.read();
        assertEquals(1, l1.getLineNo());
        assertEquals("keep this", l1.getText());
        
        assertTrue(out.hasNext());
        FileLine l3 = out.read();
        assertEquals(3, l3.getLineNo());
        assertEquals("also keep this", l3.getText());
        
        assertFalse(out.hasNext());
    }
    
    @Test
    void handlesNullPattern() {
        final IgnorePatternFilter filter = new IgnorePatternFilter(null);
        final QueuePipe<FileLine> in = new QueuePipe<>();
        final QueuePipe<FileLine> out = new QueuePipe<>();
        
        in.write(new FileLine(1, "keep this"));
        
        filter.process(in, out);
        
        assertTrue(out.hasNext());
        assertEquals("keep this", out.read().getText());
    }
}
