package com.ruscello.bus;

import com.google.common.eventbus.Subscribe;

public class GuavaEventBusListener {

    @Subscribe
    public void messageListener(Message message) {
        System.out.println("picked up by message listener");
    }

    @Subscribe
    public void fakeMessageListener(FakeMessage message) {
        System.out.println("picked up by fake message listener");
    }
}
