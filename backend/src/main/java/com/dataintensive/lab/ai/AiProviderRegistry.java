package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.DomainValidationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AiProviderRegistry {

    private final Map<String, AiProviderClient> clients = new ConcurrentHashMap<>();

    public AiProviderRegistry(List<AiProviderClient> providerClients) {
        if (providerClients != null) {
            for (AiProviderClient client : providerClients) {
                register(client);
            }
        }
    }

    public void register(AiProviderClient client) {
        clients.put(client.getProviderId().toLowerCase(), client);
    }

    public Optional<AiProviderClient> findClient(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(clients.get(providerId.trim().toLowerCase()));
    }

    public AiProviderClient getClient(String providerId) {
        return findClient(providerId)
                .orElseThrow(() -> new DomainValidationException(
                        "Provedor de IA não suportado: " + providerId + ". Provedores suportados: " + String.join(", ", getSupportedProviderIds())
                ));
    }

    public List<String> getSupportedProviderIds() {
        return clients.values().stream().map(AiProviderClient::getProviderId).toList();
    }

    public List<AiProviderInfo> getAllProviders() {
        return clients.values().stream().map(AiProviderClient::getInfo).toList();
    }

    public List<AiProviderInfo> getAllProviders(String language) {
        if (language == null || language.isBlank()) {
            return getAllProviders();
        }
        return clients.values().stream().map(c -> c.getInfo(language)).toList();
    }

    public List<AiProviderInfo> getAllProviders(com.dataintensive.lab.domain.AssessmentLanguage language) {
        if (language == null) {
            return getAllProviders();
        }
        return clients.values().stream().map(c -> c.getInfo(language)).toList();
    }

    public List<AiProviderInfo> getConfigurableTutors() {
        return clients.values().stream()
                .filter(AiProviderClient::isConfigurableTutor)
                .map(AiProviderClient::getInfo)
                .toList();
    }

    public List<AiProviderInfo> getConfigurableTutors(String language) {
        if (language == null || language.isBlank()) {
            return getConfigurableTutors();
        }
        return clients.values().stream()
                .filter(AiProviderClient::isConfigurableTutor)
                .map(c -> c.getInfo(language))
                .toList();
    }

    public List<AiProviderInfo> getConfigurableTutors(com.dataintensive.lab.domain.AssessmentLanguage language) {
        if (language == null) {
            return getConfigurableTutors();
        }
        return clients.values().stream()
                .filter(AiProviderClient::isConfigurableTutor)
                .map(c -> c.getInfo(language))
                .toList();
    }
}
