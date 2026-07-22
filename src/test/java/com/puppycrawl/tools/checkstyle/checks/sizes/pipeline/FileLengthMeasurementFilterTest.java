package com.puppycrawl.tools.checkstyle.checks.sizes.pipeline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.checks.pipeline.message.FileLine;
import com.puppycrawl.tools.checkstyle.checks.pipeline.message.Measurement;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;

class FileLengthMeasurementFilterTest {

    @Test
    void countsLines() {
        final FileLengthMeasurementFilter filter = new FileLengthMeasurementFilter(1, "maxFileLen");
        final QueuePipe<FileLine> in = new QueuePipe<>();
        final QueuePipe<Measurement> out = new QueuePipe<>();
        
        in.write(new FileLine(1, "line1"));
        in.write(new FileLine(2, "line2"));
        
        filter.process(in, out);
        
        final Measurement m = out.read();
        assertNotNull(m);
        assertEquals(2, m.getValue());
        assertEquals("maxFileLen", m.getMessageKey());
        
        assertFalse(out.hasNext());
    }
}
