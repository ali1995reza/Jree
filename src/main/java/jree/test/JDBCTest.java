package jree.test;

import jree.api.OperationResultListener;
import jree.mongo_base.H2ConnectionPool;

import java.sql.Statement;

public class JDBCTest {


    public final static void main(String[] args) throws Exception
    {

        H2ConnectionPool pool = new H2ConnectionPool(10 , "jdbc:h2:E:\\h2db\\db");

        //pool.execute("CREATE TABLE TEST (a int , b int)" , OperationResultListener.LOGGER_LISTENER);

        //Thread.sleep(100000);

        pool.execute("INSERT INTO TEST VALUES (1 , 1 ) , (2 , 2)", OperationResultListener.LOGGER_LISTENER);
    }
}
