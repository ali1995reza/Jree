package jree.api;

public interface ReadMessageCriteria<ID> {

    Session session();
    Recipient recipient();
    ID from();
    long length();
    boolean containsDisposables();
    boolean backward();
}
