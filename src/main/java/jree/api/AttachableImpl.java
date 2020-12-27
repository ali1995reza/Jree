package jree.api;

public class AttachableImpl implements Attachable {

    private Object attachment;

    @Override
    public <T> T attachment() {
        return (T)attachment;
    }

    @Override
    public <T> T attach(Object attachment) {
        Object old = this.attachment;
        this.attachment = attachment;
        return (T)old;
    }
}
