package jree.api;

public class FailReason extends RuntimeException {

    private final int code;

    public FailReason(int code) {
        super("Operation fail with code : "+code);
        this.code = code;
    }

    public FailReason(Throwable e , int code)
    {
        super("Operation fail with code : "+code , e);
        this.code = code;
    }

    public FailReason(String e , int code)
    {
        super(e);
        this.code = code;
    }


    public int code() {
        return code;
    }
}
