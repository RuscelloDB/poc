package com.ruscello;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.ruscello.bus.DisruptorLongEvent;
import com.ruscello.bus.DisruptorLongEventFactory;
import com.ruscello.bus.DisruptorLongEventHandler;
import com.ruscello.bus.DisruptorLongEventProducerWithTranslator;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

// https://github.com/LMAX-Exchange/disruptor/wiki/Getting-Started
// https://github.com/LMAX-Exchange/disruptor/issues/148
// https://github.com/mikeb01/ticketing
// https://groups.google.com/forum/#!topic/lmax-disruptor/LTlq4RvLxlg
// https://medium.com/@johnmcclean/applying-back-pressure-across-streams-f8185ad57f3a
// http://mechanitis.blogspot.com/2011/07/dissecting-disruptor-writing-to-ring.html
// https://jobs.one2team.com/apache-storms/
// https://github.com/AxonFramework/AxonFramework/search?utf8=%E2%9C%93&q=Disruptor&type=
// https://github.com/Graylog2/graylog2-server
// https://www.youtube.com/watch?v=2Be_Lqa35Y0
public class DisruptorTest {

    @Test
    public void test() throws InterruptedException {

        // Executor that will be used to construct new threads for consumers
        Executor executor = Executors.newCachedThreadPool();
        // Prefer ThreadFactory
        ThreadFactory threadFactory = DaemonThreadFactory.INSTANCE;

        // The factory for the event
        DisruptorLongEventFactory factory = new DisruptorLongEventFactory();

        // Specify the size of the ring buffer, must be power of 2.
        int bufferSize = 1024;

        // Construct the Disruptor
        // Disruptor<DisruptorLongEvent> disruptor = new Disruptor<>(factory, bufferSize, executor);
        Disruptor<DisruptorLongEvent> disruptor = new Disruptor<>(factory, bufferSize, threadFactory);

        // Connect the handler
        disruptor.handleEventsWith(new DisruptorLongEventHandler());

        // Start the Disruptor, starts all threads running
        disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        RingBuffer<DisruptorLongEvent> ringBuffer = disruptor.getRingBuffer();

        DisruptorLongEventProducerWithTranslator producer = new DisruptorLongEventProducerWithTranslator(ringBuffer);

        ByteBuffer bb = ByteBuffer.allocate(8);
        for (long l = 0; l < 10; l++) {
            bb.putLong(0, l);
            producer.onData(bb);
            Thread.sleep(1000);
        }
    }

}