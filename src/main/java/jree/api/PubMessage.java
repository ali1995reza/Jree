package jree.api;


import jutils.assertion.Assertion;

import java.time.Instant;
import java.util.List;

public interface PubMessage<BODY , ID> extends Attachable {

    enum Type{
        SESSION_TO_SESSION(1), CLIENT_TO_CLIENT(2), CLIENT_TO_CONVERSATION(3);

        private final int code;

        Type(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }

        public boolean is(PubMessage.Type type)
        {
            return this==type;
        }

        public boolean isNot(PubMessage.Type type)
        {
            return this!=type;
        }

        public final static PubMessage.Type findByCode(int code)
        {
            if(code==1)
                return SESSION_TO_SESSION;
            if(code==2)
                return CLIENT_TO_CLIENT;
            if(code==3)
                return CLIENT_TO_CONVERSATION;

            Assertion.ifTrue("can not find a type with code ["+code+"]" , true);

            return null;
        }
    }

    ID id();

    BODY body();

    Instant time();

    Publisher publisher();

    Recipient recipient();

    PubMessage.Type type();

    List<Tag> tags();
}
