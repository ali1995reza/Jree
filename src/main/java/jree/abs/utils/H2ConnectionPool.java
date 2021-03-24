package jree.abs.utils;


import jree.api.FailReason;
import jree.api.OperationResultListener;
import jree.abs.codes.FailReasonsCodes;
import org.checkerframework.checker.units.qual.C;
import org.h2.jdbcx.JdbcDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

public class H2ConnectionPool {

    private final static class ExecutionRequest{
        private final String sql;
        private final OperationResultListener<Statement> callback;

        private ExecutionRequest(String sql, OperationResultListener<Statement> callback) {
            this.sql = sql;
            this.callback = callback;
        }
    }

    private final static class H2ConnectionThread {

        private final static ExecutionRequest CLOSE_TOKEN = new ExecutionRequest("CLOSE" , OperationResultListener.EMPTY_LISTENER);

        private final Connection connection;
        private final Statement statement;
        private final BlockingQueue<ExecutionRequest> requests;
        private final Thread thread;
        private final CountDownLatch closeLatch;

        private H2ConnectionThread(Connection connection, BlockingQueue<ExecutionRequest> requests, CountDownLatch closeLatch) {
            this.connection = connection;
            this.closeLatch = closeLatch;
            try {
                this.statement = this.connection.createStatement();
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            this.requests = requests;
            this.thread = new Thread(this::mainLoop);
        }

        private void start()
        {
            thread.start();
        }

        private final void mainLoop()
        {
            try{
                while (true) {
                    ExecutionRequest request = requests.take();
                    if (request == CLOSE_TOKEN) {
                        close();
                        return;
                    }
                    execute(request);
                }

            }catch (Throwable e)
            {
                try {
                    close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }

        private final void execute(ExecutionRequest request)
        {
            try {
                statement.execute(request.sql);

                request.callback.onSuccess(statement);

            } catch (Throwable e) {
                request.callback.onFailed(new FailReason(e , FailReasonsCodes.RUNTIME_EXCEPTION));
            }
        }


        private final void close() throws SQLException {
            closeLatch.countDown();
            statement.close();
            connection.close();
        }
    }



    private final List<H2ConnectionThread> threads = new ArrayList<>();
    private final BlockingQueue<ExecutionRequest> requests = new LinkedBlockingQueue<>();
    private final JdbcDataSource dataSource;
    private final CountDownLatch closeLatch;

    public H2ConnectionPool(int size , String connectionString)
    {
        dataSource = new JdbcDataSource();
        dataSource.setURL(connectionString);
        closeLatch = new CountDownLatch(size);
        for(int i=0;i<size;i++)
        {
            H2ConnectionThread thread = createConnectionThread();
            thread.start();
            threads.add(thread);
        }
    }

    public JdbcDataSource getDataSource() {
        return dataSource;
    }

    public H2ConnectionPool(String connectionString)
    {
        this(1 , connectionString);
    }


    private final H2ConnectionThread createConnectionThread()
    {
        try {
            return new H2ConnectionThread(
                    dataSource.getConnection() ,
                    requests, closeLatch);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }


    public void execute(String sql , OperationResultListener<Statement> callback)
    {
        if(!requests.add(new ExecutionRequest(sql, callback)))
        {
            callback.onFailed(new FailReason("can't queue for execute" , FailReasonsCodes.RUNTIME_EXCEPTION));
        }
    }

    public void close()
    {
        try {
            for(int i=0;i<threads.size();i++) {
                requests.put(H2ConnectionThread.CLOSE_TOKEN);
            }
            closeLatch.await();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }


}
