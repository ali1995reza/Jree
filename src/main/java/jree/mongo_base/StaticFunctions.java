package jree.mongo_base;

import jree.api.PubMessage;
import jree.api.Publisher;
import jree.api.Recipient;
import jree.api.Session;

import java.security.SecureRandom;
import java.util.Random;

public class StaticFunctions {


    private final static Random RANDOM  = new SecureRandom();


    public final static String uniqueConversationId(Session session , Recipient recipient)
    {
        if(recipient.conversation()!=-1)
        {
            return String.valueOf(recipient.conversation());
        }else
        {
            if(recipient.session()!=-1)
            {
                if(session.clientId()>recipient.client())
                {
                    return ""+session.clientId()+"_"+
                            session.id()+"_"+
                            recipient.client()+"_"+
                            recipient.session();
                }else
                {
                    return ""+recipient.client()+"_"+
                            recipient.session()+"_"+
                            session.clientId()+"_"+
                            session.id();

                }
            }else
            {
                if(session.clientId()>recipient.client())
                {
                    return ""+session.clientId()+"_"+
                            recipient.client();
                }else
                {
                    return ""+recipient.client()+"_"+
                            session.clientId();

                }
            }
        }
    }

    public final static String relatedConversation(PubMessage message)
    {
        if(message.type().is(PubMessage.Type.SESSION_TO_SESSION))
        {
            if(message.publisher().client()>message.recipient().client())
            {
                return ""+message.publisher().client()+"_"+
                        message.publisher().session()+"_"+
                        message.recipient().client()+"_"+
                        message.recipient().session();
            }else
            {
                return ""+message.recipient().client()+"_"+
                        message.recipient().session()+"_"+
                        message.publisher().client()+"_"+
                        message.publisher().session();

            }
        }else if(message.type().is(PubMessage.Type.CLIENT_TO_CLIENT))
        {
            if(message.publisher().client()>message.recipient().client())
            {
                return ""+message.publisher().client()+"_"+
                        message.recipient().client();
            }else
            {
                return ""+message.recipient().client()+"_"+
                        message.publisher().client();

            }
        }else
        {
            return String.valueOf(message.recipient().conversation());
        }
    }

    public final static String relatedConversation(Publisher publisher  ,
                                                   Recipient recipient ,
                                                   PubMessage.Type type)
    {
        if(type.is(PubMessage.Type.SESSION_TO_SESSION))
        {
            if(publisher.client()>recipient.client())
            {
                return ""+publisher.client()+"_"+
                        publisher.session()+"_"+
                        recipient.client()+"_"+
                        recipient.session();
            }else
            {
                return ""+recipient.client()+"_"+
                        recipient.session()+"_"+
                        publisher.client()+"_"+
                        publisher.session();

            }
        }else if(type.is(PubMessage.Type.CLIENT_TO_CLIENT))
        {
            if(publisher.client()>recipient.client())
            {
                return ""+publisher.client()+"_"+
                        recipient.client();
            }else
            {
                return ""+recipient.client()+"_"+
                        publisher.client();

            }
        }else
        {
            return String.valueOf(recipient.conversation());
        }
    }

    public final static String relatedConversation(Session publisher  ,
                                                   Recipient recipient ,
                                                   PubMessage.Type type)
    {
        if(type.is(PubMessage.Type.SESSION_TO_SESSION))
        {
            if(publisher.clientId()>recipient.client())
            {
                return ""+publisher.clientId()+"_"+
                        publisher.id()+"_"+
                        recipient.client()+"_"+
                        recipient.session();
            }else
            {
                return ""+recipient.client()+"_"+
                        recipient.session()+"_"+
                        publisher.clientId()+"_"+
                        publisher.id();

            }
        }else if(type.is(PubMessage.Type.CLIENT_TO_CLIENT))
        {
            if(publisher.clientId()>recipient.client())
            {
                return ""+publisher.id()+"_"+
                        recipient.client();
            }else
            {
                return ""+recipient.client()+"_"+
                        publisher.clientId();

            }
        }else
        {
            return String.valueOf(recipient.conversation());
        }
    }

    public final static PubMessage.Type getType(Recipient recipient)
    {
        if(recipient.conversation()!=-1)
            return PubMessage.Type.CLIENT_TO_CONVERSATION;
        else if(recipient.session()!=-1)
            return PubMessage.Type.SESSION_TO_SESSION;
        else
            return PubMessage.Type.CLIENT_TO_CLIENT;
    }

    public final static long newID()
    {
        return Math.abs(
                RANDOM.nextLong()
        );
    }
}
