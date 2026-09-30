package com.dataintensive.lab.provisioning;

public class SessionExpiredException extends RuntimeException {
    public SessionExpiredException(String sessionId) {
        super("Sessão '" + sessionId + "' expirada ou não possui ambiente ativo.");
    }
}
