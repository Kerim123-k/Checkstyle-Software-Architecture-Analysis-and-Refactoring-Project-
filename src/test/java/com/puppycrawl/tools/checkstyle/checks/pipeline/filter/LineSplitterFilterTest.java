package com.puppycrawl.tools.checkstyle.checks.pipeline.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.api.FileText;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.FileLine;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.SingletonPipe;

class LineSplitterFilterTest {
    @Test
    void splitsFileTextIntoLines() {
        final LineSplitterFilter filter = new LineSplitterFilter();
        final SingletonPipe<FileText> in = new SingletonPipe<>();
        final QueuePipe<FileLine> out = new QueuePipe<>();
        in.write(new FileText(new File("dummy"), Arrays.asList("hello", "world")));
        filter.process(in, out);
        
        assertTrue(out.hasNext());
        FileLine l1 = out.read();
        assertEquals(1, l1.getLineNo());
        assertEquals("hello", l1.getText());
        
        assertTrue(out.hasNext());
        FileLine l2 = out.read();
        assertEquals(2, l2.getLineNo());
        assertEquals("world", l2.getText());
        
        assertFalse(out.hasNext());
    }
}
