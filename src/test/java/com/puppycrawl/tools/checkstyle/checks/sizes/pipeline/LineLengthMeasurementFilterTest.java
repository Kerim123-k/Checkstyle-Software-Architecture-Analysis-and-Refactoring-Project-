package com.puppycrawl.tools.checkstyle.checks.sizes.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.checks.pipeline.message.FileLine;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.Measurement;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;

class LineLengthMeasurementFilterTest {

    @Test
    void countsLineLengthWithTabs() {
        final LineLengthMeasurementFilter filter = new LineLengthMeasurementFilter(4, 80, "maxLineLen");
        final QueuePipe<FileLine> in = new QueuePipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        
        in.write(new FileLine(1, "\tline")); // tab + 4 chars = 4 + 4 = 8
        in.write(new FileLine(2, "short line")); // 10 chars
        
        filter.process(in, out);
        
        Measurement m1 = out.read();
        assertNotNull(m1);
        assertEquals(1, m1.getLineNo());
        assertEquals(8, m1.getValue());
        assertEquals("maxLineLen", m1.getMessageKey());
        
        Measurement m2 = out.read();
        assertNotNull(m2);
        assertEquals(2, m2.getLineNo());
        assertEquals(10, m2.getValue());
        
        assertFalse(out.hasNext());
    }
}
