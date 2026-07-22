package com.puppycrawl.tools.checkstyle.checks.pipeline.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.puppycrawl.tools.checkstyle.checks.pipeline.message.ViolationMessage;
import com.puppycrawl.tools.checkstyle.checks.pipeline.pipe.QueuePipe;

class ViolationSinkTest {
    @Test
    void forwardsMessages() {
        final ViolationSink sink = new ViolationSink();
        final QueuePipe<ViolationMessage> in = new QueuePipe<>();
        final QueuePipe<ViolationMessage> out = new QueuePipe<>();
        
        ViolationMessage msg = new ViolationMessage(1, 1, "key", 1);
        in.write(msg);
        
        sink.process(in, out);
        
        assertTrue(out.hasNext());
        assertEquals(msg, out.read());
        assertFalse(out.hasNext());
    }
}
