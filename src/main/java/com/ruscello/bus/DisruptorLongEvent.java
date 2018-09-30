package com.ruscello.bus;

public class DisruptorLongEvent {
    private long value;

    public void set(long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "DisruptorLongEvent{" +
                "value=" + value +
                '}';
    }
}

