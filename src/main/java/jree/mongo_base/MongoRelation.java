package jree.mongo_base;

import jree.api.Relation;

import java.util.Collections;
import java.util.Map;

public class MongoRelation implements Relation {

    private final Map<String, String> attributes;

    public MongoRelation(Map<String, String> attributes) {
        this.attributes = attributes==null? Collections.emptyMap():attributes;
    }

    @Override
    public String getAttribute(String attar) {
        return attributes.get(attar);
    }
}
