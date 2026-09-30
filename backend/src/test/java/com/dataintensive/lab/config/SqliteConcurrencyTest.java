package com.dataintensive.lab.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteConcurrencyTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Deve executar leituras e escritas concorrentes sem erros de SQLITE_BUSY sob modo WAL")
    void shouldExecuteConcurrentReadsAndWritesWithoutDatabaseLocked() throws Exception {
        String dbUrl = "jdbc:sqlite:" + tempDir.resolve("concurrency-stress.db").toAbsolutePath();
        try (HikariDataSource dataSource = SqliteTestDataSourceFactory.createDataSource(dbUrl)) {
            // Inicializar tabela de teste
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("""
                    CREATE TABLE stress_catalog (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        thread_name TEXT NOT NULL,
                        operation_index INTEGER NOT NULL,
                        payload TEXT NOT NULL
                    );
                """);
            }

            int numThreads = 8;
            int operationsPerThread = 50;
            int totalExpectedWrites = (numThreads * operationsPerThread) / 2;

            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(numThreads);
            List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger successfulWrites = new AtomicInteger(0);
            AtomicInteger successfulReads = new AtomicInteger(0);

            for (int t = 0; t < numThreads; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int op = 0; op < operationsPerThread; op++) {
                            if (op % 2 == 0) {
                                // Escrita concorrente
                                try (Connection conn = dataSource.getConnection();
                                     PreparedStatement ps = conn.prepareStatement(
                                             "INSERT INTO stress_catalog (thread_name, operation_index, payload) VALUES (?, ?, ?)"
                                     )) {
                                    ps.setString(1, "thread-" + threadId);
                                    ps.setInt(2, op);
                                    ps.setString(3, "payload-content-" + threadId + "-" + op);
                                    ps.executeUpdate();
                                    successfulWrites.incrementAndGet();
                                }
                            } else {
                                // Leitura concorrente
                                try (Connection conn = dataSource.getConnection();
                                     Statement stmt = conn.createStatement();
                                     ResultSet rs = stmt.executeQuery("SELECT count(*), max(operation_index) FROM stress_catalog")) {
                                    if (rs.next()) {
                                        long count = rs.getLong(1);
                                        assertThat(count).isGreaterThanOrEqualTo(0);
                                        successfulReads.incrementAndGet();
                                    }
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        exceptions.add(ex);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).as("Todas as threads devem concluir dentro do timeout").isTrue();
            assertThat(exceptions).as("Nenhuma exceção de concorrência ou SQLITE_BUSY deve ocorrer").isEmpty();
            assertThat(successfulWrites.get()).isEqualTo(totalExpectedWrites);

            // Validar integridade e quantidade final de registros
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT count(*) FROM stress_catalog")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(totalExpectedWrites);
            }
        }
    }

    @Test
    @DisplayName("Deve permitir leituras imediatas sem bloqueio enquanto transação de escrita está em andamento")
    void shouldAllowConcurrentReadersWhileWriteTransactionIsInFlight() throws Exception {
        String dbUrl = "jdbc:sqlite:" + tempDir.resolve("concurrency-readers.db").toAbsolutePath();
        try (HikariDataSource dataSource = SqliteTestDataSourceFactory.createDataSource(dbUrl)) {
            try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE read_write_isolation (id INTEGER PRIMARY KEY, title TEXT);");
                stmt.execute("INSERT INTO read_write_isolation (id, title) VALUES (1, 'Initial Data');");
            }

            CountDownLatch writeStartedLatch = new CountDownLatch(1);
            CountDownLatch readFinishedLatch = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);

            // Thread 1: Inicia transação de escrita e retém a conexão por 300ms
            Future<?> writeFuture = executor.submit(() -> {
                try (Connection conn = dataSource.getConnection()) {
                    conn.setAutoCommit(false);
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute("INSERT INTO read_write_isolation (id, title) VALUES (2, 'Uncommitted Data');");
                        writeStartedLatch.countDown();
                        // Simula delay com lock de escrita aberto
                        Thread.sleep(300);
                        conn.commit();
                    }
                }
                return null;
            });

            // Thread 2: Executa leitura concorrente durante o intervalo da escrita
            Future<?> readFuture = executor.submit(() -> {
                try {
                    writeStartedLatch.await();
                    long startTime = System.currentTimeMillis();
                    try (Connection conn = dataSource.getConnection();
                         Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery("SELECT count(*) FROM read_write_isolation WHERE id = 1")) {
                        assertThat(rs.next()).isTrue();
                        assertThat(rs.getInt(1)).isEqualTo(1);
                    }
                    long duration = System.currentTimeMillis() - startTime;
                    // No modo WAL, o leitor não espera os 300ms da escrita
                    assertThat(duration).as("Leitura em modo WAL deve responder quase instantaneamente (< 250ms)").isLessThan(250);
                } finally {
                    readFinishedLatch.countDown();
                }
                return null;
            });

            writeFuture.get(5, TimeUnit.SECONDS);
            readFuture.get(5, TimeUnit.SECONDS);
            executor.shutdown();

            // Validar que ambos os dados estão gravados após o commit
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT count(*) FROM read_write_isolation")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(2);
            }
        }
    }
}
