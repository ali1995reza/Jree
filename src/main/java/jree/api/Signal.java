package jree.api;

public interface Signal<BODY> {

    BODY body();

    Publisher publisher();

    Recipient recipient();
}
