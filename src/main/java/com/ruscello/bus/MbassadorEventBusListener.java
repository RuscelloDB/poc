package com.ruscello.bus;

import net.engio.mbassy.listener.*;
import net.engio.mbassy.subscription.MessageEnvelope;

import java.util.HashMap;
import java.util.LinkedList;

@Listener(references = References.Strong)
public class MbassadorEventBusListener {

    @Handler
    public void messageListener(Message message) {
        System.out.println("picked up by message listener");
    }

    @Handler
    public void fakeMessageListener(FakeMessage message) {
        System.out.println("picked up by fake message listener");
    }

    @Handler(delivery = Invoke.Asynchronously)
    //@Handler
    public void expensiveOperation(AnotherFakeMessage message){
        System.out.println("picked up by another fake message asynchronously");
    }

    @Handler(condition = "msg.size() >= 0")
    @Enveloped(messages = {HashMap.class, LinkedList.class})
    public void handleMapOrList(MessageEnvelope envelope) {
        System.out.println("picked up by handle map or list");
    }

}

