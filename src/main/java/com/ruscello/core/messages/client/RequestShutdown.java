package com.ruscello.core.messages.client;

public class RequestShutdown {

    private final boolean exitProcess;
    private final boolean shutdownHttp;

    public RequestShutdown(boolean exitProcess, boolean shutdownHttp) {
        this.exitProcess = exitProcess;
        this.shutdownHttp = shutdownHttp;
    }

    public boolean isExitProcess() {
        return exitProcess;
    }

    public boolean isShutdownHttp() {
        return shutdownHttp;
    }
}
