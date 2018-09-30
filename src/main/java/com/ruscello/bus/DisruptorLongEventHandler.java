package com.ruscello.bus;

import com.lmax.disruptor.EventHandler;

public class DisruptorLongEventHandler implements EventHandler<DisruptorLongEvent> {
    public void onEvent(DisruptorLongEvent event, long sequence, boolean endOfBatch) {
        System.out.println("Event: " + event);
    }
}