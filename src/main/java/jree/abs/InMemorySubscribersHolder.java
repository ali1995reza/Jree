package jree.abs;

import jree.abs.funcs.ForEach;
import jree.abs.parts.SubscribersHolder;
import jree.api.OperationResultListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class InMemorySubscribersHolder implements SubscribersHolder {


    private final class SubscriberGarbageCollector implements Runnable {

        private ArrayBlockingQueue<Long> queue = new ArrayBlockingQueue(10000);

        @Override
        public void run() {
            try {
                mainLoop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void mainLoop() throws Exception {
            List<Long> batch = new ArrayList<>(1000);
            while (true) {
                Long l = queue.take();
                while (l != null) {
                    batch.add(l);
                    if (batch.size() == 1000) {
                        break;
                    }
                    l = queue.poll();
                }
                removeSubscribers(batch);
                batch.clear();
            }
        }

        private void removeSubscribers(List<Long> subscribersToRemove) {
            for (List<Long> subscribers : subscribersMap.values()) {
                synchronized (subscribers) {
                    for (long clientId : subscribersToRemove) {
                        int index = Collections.binarySearch(subscribers, clientId, ASC);
                        if (index >= 0) {
                            subscribers.remove(index);
                        }
                    }
                }
            }
        }
    }


    private final static Comparator<Long> ASC = Long::compareTo;

    private final ConcurrentHashMap<Long, List<Long>> subscribersMap;
    private final SubscriberGarbageCollector collector = new SubscriberGarbageCollector();
    private final ThreadLocal<List<Long>> chunkThreadLocal = ThreadLocal.withInitial(ArrayList::new);

    public InMemorySubscribersHolder() {
        subscribersMap = new ConcurrentHashMap<>();
        new Thread(collector).start();
    }


    @Override
    public void addSubscriber(long conversation, long clientId, OperationResultListener<Boolean> callback) {
        List<Long> subscribers = subscribersMap.computeIfAbsent(conversation, this::builder);
        synchronized (subscribers) {
            int index = Collections.binarySearch(subscribers, clientId, ASC);
            if (index < 0) {
                index = -index - 1;
                subscribers.add(index, clientId);
            } //else this is exists
        }
        callback.onSuccess(true);
    }

    @Override
    public void addSubscriber(List<Long> conversations, long clientId, OperationResultListener<Boolean> callback) {
        for (Long conversation : conversations) {
            addSubscriber(conversation, clientId, OperationResultListener.EMPTY_LISTENER);
        }
        callback.onSuccess(true);
    }

    @Override
    public void removeSubscriber(long conversation, long clientId, OperationResultListener<Boolean> callback) {
        List<Long> subscribers = subscribersMap.get(conversation);
        if(subscribers == null) {
            callback.onSuccess(true);
            return;
        }
        synchronized (subscribers) {
            int index = Collections.binarySearch(subscribers, clientId, ASC);
            if(index >= 0) {
                subscribers.remove(index);
            }
        }
        callback.onSuccess(true);
    }

    @Override
    public void removeSubscriberFromAllConversations(long clientId) {
        try {
            collector.queue.put(clientId);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void getSubscribers(long conversation, ForEach<List<Long>> forEach) {
        List<Long> chunk = chunkThreadLocal.get();
        List<Long> subscribers = subscribersMap.get(conversation);
        if (subscribers == null) {
            forEach.done(null);
            return;
        }
        int count = 0;
        Long last = null;
        while (true) {
            boolean done = false;
            synchronized (subscribers) {
                for (int i = count; i < subscribers.size(); i++) {
                    long subscriber = subscribers.get(i);
                    if (last == null || last < subscriber) {
                        last = subscriber;
                        chunk.add(subscriber);
                    }
                    ++count;
                    if (chunk.size() >= 200) {
                        break;
                    }
                }
                done = count >= subscribers.size();
            }
            forEach.accept(chunk);
            chunk.clear();
            if (done) {
                forEach.done(null);
                return;
            }
        }
    }


    private List<Long> builder(long conversation) {
        return new ArrayList<>();
    }
}
