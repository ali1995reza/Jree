package jree.util.concurrentiter;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ConcurrentIter<T> {

    private IterNode<T> first;
    private IterNode<T> last;
    private int size;
    private final Object _sync = new Object();
    private Object attachment;

    private ConcurrentIterEventListener<T> eventListener = ConcurrentIterEventListener.EMPTY;


    public IterNode<T> add(T t)
    {
        synchronized (_sync)
        {
            if(first == null)
            {
                first = new IterNode<>(t , null , null, this);
                last = first;
                ++size;
                eventListener.afterAdd(this , first);
                return first;
            }else
            {
                IterNode<T> added = last.setNext(new IterNode<>(t , last , null, this));
                last = added;
                ++size;
                eventListener.afterAdd(this , last);
                return last;
            }
        }
    }

    ConcurrentIter<T> remove(IterNode<T> iterNode)
    {
        synchronized (_sync)
        {
            if(iterNode.iterator!=this)
                throw new IllegalStateException("this node not related to this iterator");

            if(iterNode.isRemoved())
                throw new IllegalStateException("this node already removed");

            if(iterNode==first || iterNode==last) {
                if (iterNode == first) {
                    first = iterNode.next();
                    if (first != null)
                        first.setPrevious(null);
                }

                if (iterNode == last) {
                    last = iterNode.previous();
                    if (last != null)
                        last.setNext(null);
                }
            }else {
                IterNode<T> p = iterNode.previous();
                IterNode<T> n = iterNode.next();
                p.setNext(n);
                n.setPrevious(p);
            }

            --size;

            eventListener.afterRemove(this , iterNode);

            return this;
        }
    }

    public int size() {
        return size;
    }

    public boolean isEmpty()
    {
        return size==0;
    }

    public ConcurrentIter<T> forEach(Consumer<T> consumer)
    {
        IterNode<T> node = first;
        while (node!=null)
        {
            consumer.accept(node.value());
            node = node.next();
        }
        return this;
    }


    public ConcurrentIter<T> attach(Object attachment)
    {
        this.attachment = attachment;
        return this;
    }
    public <A> A attachment()
    {
        return (A) attachment;
    }

    public ConcurrentIter<T> setEventListener(ConcurrentIterEventListener<T> eventListener) {
        if(eventListener==null)
            return this;

        this.eventListener = eventListener;

        return this;
    }

    public <A> ConcurrentIter<T> forEach(BiConsumer<T , A> consumer , A attachment)
    {
        IterNode<T> node = first;
        while (node!=null)
        {
            consumer.accept(node.value() , attachment);
            node = node.next();
        }
        return this;
    }
}
