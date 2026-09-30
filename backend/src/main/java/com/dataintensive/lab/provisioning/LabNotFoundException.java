package com.dataintensive.lab.provisioning;

public class LabNotFoundException extends RuntimeException {
    public LabNotFoundException(String labId) {
        super("Laboratório '" + labId + "' não foi encontrado no catálogo técnico.");
    }
}
