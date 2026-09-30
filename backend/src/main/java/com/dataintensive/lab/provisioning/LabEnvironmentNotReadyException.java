package com.dataintensive.lab.provisioning;

public class LabEnvironmentNotReadyException extends RuntimeException {
    public LabEnvironmentNotReadyException(String labId, LabEnvironmentStatus currentStatus) {
        super("O ambiente para o laboratório '" + labId + "' não está no estado READY (status atual: " + currentStatus + "). Aguarde a conclusão do provisionamento.");
    }
}
