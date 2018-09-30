package com.ruscello.bus;


import dorkbox.messageBus.annotations.Handler;

import java.util.HashMap;
import java.util.LinkedList;

public class MessageBusEventBusListener {

    @Handler
    public void messageListener(Message message) {
        System.out.println("picked up by message listener");
    }

    @Handler
    public void fakeMessageListener(FakeMessage message) {
        System.out.println("picked up by fake message listener");
    }

    // @Handler(delivery = Invoke.Asynchronously)
    @Handler
    public void expensiveOperation(AnotherFakeMessage message){
        System.out.println("picked up by another fake message asynchronously");
    }

}
