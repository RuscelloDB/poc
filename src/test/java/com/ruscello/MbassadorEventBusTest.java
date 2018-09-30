package com.ruscello;

import com.ruscello.bus.AnotherFakeMessage;
import com.ruscello.bus.FakeMessage;
import com.ruscello.bus.MbassadorEventBusListener;
import com.ruscello.bus.Message;
import net.engio.mbassy.bus.MBassador;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class MbassadorEventBusTest {

    // SubscriptionManager getSubscriptionsByMessageType --> Class[] types = ReflectionUtils.getSuperTypes(messageType);
    // ReflectionUtils.getSuperTypes(messageType); --> class.getSuperclass

    @Test
    public void test() {
        MBassador bus = new MBassador();
        bus.subscribe(new MbassadorEventBusListener());

        System.out.println("post message to bus");
        bus.publish(new Message());

        System.out.println("post fake message to bus");
        bus.publish(new FakeMessage());

        // posting map doesn't work
        System.out.println("post map to bus");
        Map<String, String> map = new HashMap<>();
        //map.put("key", "value");
        bus.publish(map);

        System.out.println("post another fake message to bus");
        bus.post(new AnotherFakeMessage()).asynchronously();

        System.out.println("fin");
    }
}
