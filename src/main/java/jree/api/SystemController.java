package jree.api;

public interface SystemController {

    void start();

    void shutdown();

    void waitForSessionsToCloseAndShutdown();

}
