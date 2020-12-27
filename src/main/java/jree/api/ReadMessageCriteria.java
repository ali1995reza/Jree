package jree.api;

public interface ReadMessageCriteria {

    Session session();
    Recipient recipient();
    long offset();
    long length();
    boolean backward();
}
