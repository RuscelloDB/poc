package com.ruscello;

import com.ruscello.bus.AnotherFakeMessage;
import com.ruscello.bus.FakeMessage;
import com.ruscello.bus.Message;
import com.ruscello.bus.MessageBusEventBusListener;
import dorkbox.messageBus.MessageBus;
import org.junit.jupiter.api.Test;

public class MessageBusEventBusTest {

    @Test
    public void test() {

        MessageBus bus = new MessageBus();
        bus.useStrongReferencesByDefault = false;

        bus.subscribe(new MessageBusEventBusListener());

        System.out.println("post message to bus");
        bus.publish(new Message());

        System.out.println("post fake message to bus");
        bus.publish(new FakeMessage());

        System.out.println("post another fake message to bus");
        bus.publishAsync(new AnotherFakeMessage());

        System.out.println("fin");
    }

}
