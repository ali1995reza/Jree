package jree.api;

public final class InsertTag<ID> {

    private String name;
    private String value;
    private ID from;
    private ID to;


    public InsertTag<ID> from(ID idIndex) {
        this.from = idIndex;
        return this;
    }

    public InsertTag<ID> to(ID to) {
        this.to = to;
        return this;
    }

    public InsertTag<ID> withName(String name) {
        this.name = name;
        return this;
    }

    public InsertTag<ID> andValue(String value)
    {
        this.value = value;
        return this;
    }


    public ID from() {
        return from;
    }

    public ID to() {
        return to;
    }

    public String value() {
        return value;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return "InsertTag{" + "name='" + name + '\'' + ", value='" + value + '\'' + ", from=" + from + ", to=" + to +
                '}';
    }

}
