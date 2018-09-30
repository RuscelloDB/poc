package com.ruscello.bus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.ParameterizedType;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;

/**
 * Synchronously dispatches messages to zero or more subscribers.
 * Subscribers are responsible for handling exceptions
 * Probably should use guava's eventbus instead
 * https://github.com/dorkbox/MessageBus
 * https://github.com/bennidi/mbassador
 * http://psy-lob-saw.blogspot.com/2012/12/atomiclazyset-is-performance-win-for.html
 * https://vertx.io/docs/vertx-core/java/#_the_event_bus_api
 * https://github.com/bennidi/eventbus-performance/issues/1
 */
public class InMemoryBus implements IBus, ISubscriber, IPublisher, IHandle<Message> {

    public static InMemoryBus createTest() {
        return new InMemoryBus();
    }

    public static final Duration DEFAULT_SLOW_MESSAGE_THRESHOLD = Duration.of(48, ChronoUnit.MILLIS); //TimeSpan.FromMilliseconds(48);

    private static final Logger LOG = LogManager.getLogger(InMemoryBus.class);

    private String name;
    //private final List<IMessageHandler>[]_handlers;
    private final List<IMessageHandler>[] _handlers;
    private final boolean _watchSlowMsg;
    private final Duration _slowMsgThreshold;

    private InMemoryBus() {
        this("Test") ;
    }

    private InMemoryBus(String name) {
        this(name, true, null);
    }

    public InMemoryBus(String name, boolean watchSlowMsg, Duration slowMsgThreshold) {
        this.name = name;
        _watchSlowMsg = watchSlowMsg;
        _slowMsgThreshold = slowMsgThreshold == null ? DEFAULT_SLOW_MESSAGE_THRESHOLD : slowMsgThreshold;

        //_handlers=new List<IMessageHandler>[MessageHierarchy.MaxMsgTypeId+1];
//        _handlers = new ArrayList<IMessageHandler>();
        _handlers = (List<IMessageHandler>[])new List[1];
        for(int i=0;i < _handlers.length; ++i) {
            _handlers[i] = new ArrayList<>();
        }
    }

    public <T extends Message> void subscribe(IHandle<T> handler)  {
        // Ensure.NotNull(handler,"handler");

        Class<T> clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        int[]descendants = MessageHierarchy.DescendantsByType.get(clazz);
        for(int i=0;i<descendants.length;++i) {
            List<IMessageHandler> handlers = _handlers[descendants[i]];
            if (hasHandler(handlers, handler)) {
                handlers.add(new MessageHandler<>(handler, handler.getClass().getName()));
            }
//            if(!handlers.Any(x=>x.IsSame<T>(handler))){
//                handlers.Add(new MessageHandler<T>(handler, handler.GetType().Name));
//            }
        }
    }

    private static <T> boolean hasHandler(List<IMessageHandler> handlers, IHandle<T> handler) {
        for (IMessageHandler messageHandler : handlers) {
            if (messageHandler.equals(handler)) {
                return true;
            }
        }
        return false;
    }

    public <T extends Message> void unsubscribe(IHandle<T> handler) {
        // Ensure.NotNull(handler,"handler");

        // int[]descendants=MessageHierarchy.DescendantsByType[typeof(T)];
        Class<T> clazz = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        int[]descendants = MessageHierarchy.DescendantsByType.get(clazz);
        for(int i=0;i<descendants.length;++i)  {
            // var handlers=_handlers[descendants[i]];
            List<IMessageHandler> handlers = _handlers[descendants[i]];
//            var messageHandler=handlers.FirstOrDefault(x=>x.IsSame<T>(handler));
//            if(messageHandler!=null) {
//                handlers.Remove(messageHandler);
//            }
            if (hasHandler(handlers, handler)) {

            }
        }
    }

    public void handle(Message message) {
        // publish(message);
    }

//    public void publish(Message message) {
//        //if (message == null) throw new ArgumentNullException("message");
//
//        var handlers=_handlers[message.MsgTypeId];
//        for(int i=0,n=handlers.Count;i<n; ++i) {
//            var handler=handlers[i];
//            if(_watchSlowMsg) {
//                var start=DateTime.UtcNow;
//
//                handler.TryHandle(message);
//
//                var elapsed=Clock.UTDateTime.UtcNow-start;
//                if(elapsed>_slowMsgThreshold) {
//                    LOG.trace("SLOW BUS MSG [{0}]: {1} - {2}ms. Handler: {3}.",
//                            Name, message.GetType().Name, (int) elapsed.TotalMilliseconds, handler.HandlerName);
//                    if (elapsed > QueuedHandler.VerySlowMsgThreshold && !(message is SystemMessage.SystemInit)){
//                        Log.Error("---!!! VERY SLOW BUS MSG [{0}]: {1} - {2}ms. Handler: {3}.", Name, message.GetType().Name, (int) elapsed.TotalMilliseconds, handler.HandlerName);
//                    }
//                }
//            } else {
//                handler.TryHandle(message);
//            }
//        }
//    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}