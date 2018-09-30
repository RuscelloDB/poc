package com.ruscello.bus;

import com.lmax.disruptor.EventFactory;

public class DisruptorLongEventFactory implements EventFactory<DisruptorLongEvent> {
    public DisruptorLongEvent newInstance() {
        return new DisruptorLongEvent();
    }
}