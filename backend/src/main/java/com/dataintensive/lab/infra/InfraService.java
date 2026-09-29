package com.dataintensive.lab.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;

@Service
public class InfraService {

    private final String postgresUrl;
    private final String postgresUser;
    private final String postgresPassword;

    public InfraService(
            @Value("${lab.postgres.url}") String postgresUrl,
            @Value("${lab.postgres.username}") String postgresUser,
            @Value("${lab.postgres.password}") String postgresPassword) {
        this.postgresUrl = postgresUrl;
        this.postgresUser = postgresUser;
        this.postgresPassword = postgresPassword;
    }

    public InfraStatus checkStatus() {
        boolean pgReady = false;
        String pgMsg;
        try (Connection conn = DriverManager.getConnection(postgresUrl, postgresUser, postgresPassword)) {
            pgReady = conn.isValid(2);
            pgMsg = pgReady ? "PostgreSQL 16 conectado com sucesso na porta 5432" : "PostgreSQL não respondeu";
        } catch (Exception e) {
            pgMsg = "Inacessível: " + e.getMessage();
        }

        boolean neo4jReady = false;
        String neoMsg;
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 7687), 1500);
            neo4jReady = true;
            neoMsg = "Neo4j 5 disponível via protocolo Bolt na porta 7687";
        } catch (Exception e) {
            neoMsg = "Inacessível na porta 7687: " + e.getMessage();
        }

        return new InfraStatus(pgReady, pgMsg, neo4jReady, neoMsg, System.currentTimeMillis());
    }
}
