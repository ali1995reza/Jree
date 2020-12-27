package jree.mongo_base;

import jree.api.InsertTagResult;
import jree.api.Tag;

public class InsertTagResultImpl implements InsertTagResult {

    private final long effectedMessages;
    private final Tag tag;

    public InsertTagResultImpl(long effectedMessages, Tag tag) {
        this.effectedMessages = effectedMessages;
        this.tag = tag;
    }


    @Override
    public long howManyMessageEffected() {
        return effectedMessages;
    }

    @Override
    public Tag tag() {
        return tag;
    }


    @Override
    public String toString() {
        return "InsertTagResult{" +
                "effectedMessages=" + effectedMessages +
                ", tag=" + tag +
                '}';
    }
}
