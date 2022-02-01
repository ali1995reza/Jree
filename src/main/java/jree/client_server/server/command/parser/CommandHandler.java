package jree.client_server.server.command.parser;

public interface CommandHandler<T> {

    void handle(T t) throws Throwable;
}
