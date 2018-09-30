package com.ruscello.bus;

import com.google.common.base.Strings;

public class MessageHandler<T extends Message> implements IMessageHandler {

    private final IHandle<T> _handler;
    private final String handlerName;

    public MessageHandler(IHandle<T> handler, String handlerName) {
        if (handler == null) {
            throw new IllegalArgumentException("handler");
        }
        _handler = handler;
        this.handlerName = handlerName == null ? "" : handlerName;
    }

    public boolean TryHandle(Message message) {
        //var msg = message as T;
        //if (msg != null) {
        if (message != null) {
            // _handler.Handle(msg);
            return true;
        }
        return false;
    }

    public <T2> boolean IsSame(Object handler) {
        // return ReferenceEquals(_handler, handler) && typeof(T) == typeof(T2);
        return false;
    }

    @Override
    public String toString() {
        return Strings.isNullOrEmpty(handlerName) ? _handler.toString() : handlerName;
    }

    public String getHandlerName() {
        return handlerName;
    }

}
