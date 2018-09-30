package com.ruscello.bus;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;

import java.nio.ByteBuffer;

public class DisruptorLongEventProducerWithTranslator {

    private final RingBuffer<DisruptorLongEvent> ringBuffer;

    public DisruptorLongEventProducerWithTranslator(RingBuffer<DisruptorLongEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    private static final EventTranslatorOneArg<DisruptorLongEvent, ByteBuffer> TRANSLATOR =
            new EventTranslatorOneArg<DisruptorLongEvent, ByteBuffer>() {
                public void translateTo(DisruptorLongEvent event, long sequence, ByteBuffer bb) {
                    event.set(bb.getLong(0));
                }
            };

    public void onData(ByteBuffer bb) {
        ringBuffer.publishEvent(TRANSLATOR, bb);
    }

}
