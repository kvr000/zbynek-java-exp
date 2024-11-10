package com.github.kvr000.exp.java.sql.sqlite.performance;

import com.google.common.base.Stopwatch;
import lombok.extern.log4j.Log4j2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.concurrent.TimeUnit;

@Log4j2
public class SqlPerformance
{
    public static void main(String[] args) throws Exception
    {
        System.exit(new SqlPerformance().run(args));
    }

    public int run(String[] args) throws Exception
    {
        try (Connection connection = createDb()) {
            return runInsertBenchmark(connection, args);
        }
    }

    public int runInsertBenchmark(Connection connection, String[] args) throws Exception
    {
        connection.createStatement().execute("CREATE TABLE test1 (id varchar(256), content varchar(512))");
        connection.createStatement().execute("CREATE INDEX test1_id ON test1 (id)");
        connection.setAutoCommit(false);

        Stopwatch stopwatch = Stopwatch.createStarted();
        for (int i = 0; ; ++i) {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO test1 (id, content) VALUES (?, ?)")) {
                statement.addBatch();
                statement.setInt(0, i);
                statement.setString(1, Long.toString(i));
                statement.execute();
            }
            if (i%1000 == 0) {
                connection.commit();
                if (i%1000 == 0) {
                    log.info("Inserted 1000 items in: time={} ms total={}", stopwatch.elapsed(TimeUnit.MILLISECONDS), i);
                    stopwatch.reset().start();
                }
            }
            if (i == 1_000_000) {
                break;
            }
        }
        return 0;
    }

    public Connection createDb() throws Exception {
        Path path = Files.createTempFile("sqlite-", ".db");
        //path.toFile().deleteOnExit();
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);

        log.info("Opened database at: {}", path);
        return connection;
    }
}
