package jree.api;

public interface ReadMessageCriteria<ID> {

    Session session();
    Recipient recipient();
    ID from();
    int length();
    default boolean containsDisposables(){
        return false;
    };
    default boolean backward(){
        return false;
    };
}
