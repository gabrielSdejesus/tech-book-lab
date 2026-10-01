package com.dataintensive.lab.ai;

import com.dataintensive.lab.domain.DomainValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiProviderRegistryTest {

    @Test
    @DisplayName("Deve registrar e resolver cliente de IA pelo ID ignorando maiúsculas/minúsculas")
    void shouldRegisterAndResolveProviderByIdCaseInsensitive() {
        AiProviderClient mockClient = mock(AiProviderClient.class);
        when(mockClient.getProviderId()).thenReturn("custom_ai");
        AiProviderInfo info = new AiProviderInfo("custom_ai", "Custom AI", "Desc", true, "key", null, "model-1", List.of());
        when(mockClient.getInfo()).thenReturn(info);

        AiProviderRegistry registry = new AiProviderRegistry(List.of(mockClient));

        Optional<AiProviderClient> resolved = registry.findClient("CUSTOM_AI");
        assertThat(resolved).isPresent();
        assertThat(resolved.get().getProviderId()).isEqualTo("custom_ai");
        assertThat(registry.getClient("Custom_AI")).isSameAs(mockClient);
    }

    @Test
    @DisplayName("Deve lançar DomainValidationException com lista de provedores suportados ao buscar provedor inexistente")
    void shouldThrowDomainValidationExceptionWhenProviderNotFound() {
        AiProviderClient mockClient = mock(AiProviderClient.class);
        when(mockClient.getProviderId()).thenReturn("gemini");

        AiProviderRegistry registry = new AiProviderRegistry(List.of(mockClient));

        DomainValidationException ex = assertThrows(
                DomainValidationException.class,
                () -> registry.getClient("openai")
        );

        assertThat(ex.getMessage()).contains("Provedor de IA não suportado: openai");
        assertThat(ex.getMessage()).contains("gemini");
    }

    @Test
    @DisplayName("Deve listar todos os provedores e metadados cadastrados")
    void shouldListAllRegisteredProviders() {
        AiProviderClient client1 = mock(AiProviderClient.class);
        when(client1.getProviderId()).thenReturn("gemini");
        AiProviderInfo info1 = new AiProviderInfo("gemini", "Gemini", "Desc1", true, "key", null, "gemini-2.5", List.of());
        when(client1.getInfo()).thenReturn(info1);

        AiProviderClient client2 = mock(AiProviderClient.class);
        when(client2.getProviderId()).thenReturn("ollama");
        AiProviderInfo info2 = new AiProviderInfo("ollama", "Ollama", "Desc2", false, null, null, "qwen", List.of());
        when(client2.getInfo()).thenReturn(info2);

        AiProviderRegistry registry = new AiProviderRegistry(List.of(client1, client2));

        assertThat(registry.getSupportedProviderIds()).containsExactlyInAnyOrder("gemini", "ollama");
        assertThat(registry.getAllProviders()).containsExactlyInAnyOrder(info1, info2);
    }

    @Test
    @DisplayName("Deve permitir registrar dinamicamente novos provedores em tempo de execução")
    void shouldAllowDynamicRegistrationOfNewProvider() {
        AiProviderRegistry registry = new AiProviderRegistry(List.of());
        assertThat(registry.findClient("deepseek")).isEmpty();

        AiProviderClient deepseekClient = mock(AiProviderClient.class);
        when(deepseekClient.getProviderId()).thenReturn("deepseek");
        registry.register(deepseekClient);

        assertThat(registry.findClient("deepseek")).isPresent();
        assertThat(registry.getClient("deepseek")).isSameAs(deepseekClient);
    }
}
