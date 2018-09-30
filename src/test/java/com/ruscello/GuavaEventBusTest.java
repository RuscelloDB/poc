package com.ruscello;

import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.reflect.TypeToken;
import com.ruscello.bus.FakeMessage;
import com.ruscello.bus.GuavaEventBusListener;
import com.ruscello.bus.Message;
import org.junit.jupiter.api.Test;

public class GuavaEventBusTest {

    // SubscriberRegistry - flattenHierarchyCache

    @Test
    public void rawTypesTest() {
        ImmutableSet rawTypes = ImmutableSet.<Class<?>>copyOf(TypeToken.of(FakeMessage.class).getTypes().rawTypes());
        System.out.println(rawTypes);
    }

    @Test
    public void test() {
        EventBus bus = new EventBus();

        GuavaEventBusListener listener = new GuavaEventBusListener();
        bus.register(listener);

        System.out.println("post message to bus");
        bus.post(new Message());

        System.out.println("post fake message to bus");
        bus.post(new FakeMessage());
    }
}
