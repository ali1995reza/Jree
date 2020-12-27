package jree.api;

public interface Attachable {

    <T> T attachment();
    <T> T attach(Object attachment);
}
