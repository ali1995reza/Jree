package jree.test;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

public class TestCassandra {


    public static void main(String[] args)
    {
        Cluster cluster = Cluster.builder().addContactPoint("localhost").build();

        Session session = cluster.connect();

        System.out.println(session);


    }
}
