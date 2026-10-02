package com.dataintensive.lab.provisioning;

import com.dataintensive.lab.domain.EngineType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DockerContainerSecurityTest {

    @Test
    @DisplayName("Deve gerar identificador padronizado tbl-lab-<engine> para execução local")
    void shouldResolveContainerNameWithStandardPrefix() {
        SessionId sessionId = SessionId.of("e5a8383c-6c25-4a6f-8e11-601b59836f68");
        String labId = "ddia-cap-03-lab-01";

        String containerName = DockerComposeLabManager.resolveContainerName(sessionId, labId, EngineType.POSTGRES);

        assertThat(containerName).isEqualTo("tbl-lab-postgres");
    }

    @Test
    @DisplayName("Deve construir argumentos Docker contendo flags de segurança obrigatórias e sem acesso ao host")
    void shouldBuildRunCommandWithMandatorySecurityFlags() {
        String containerName = "tbl-lab-postgres";
        List<String> command = DockerComposeLabManager.buildRunCommand(containerName, EngineType.POSTGRES);

        // Flags de seguranca mandatorias
        assertThat(command).contains("--security-opt=no-new-privileges:true");
        assertThat(command).contains("--cap-drop=ALL");
        assertThat(command).contains("--cap-add=CHOWN");
        assertThat(command).contains("--cap-add=SETUID");
        assertThat(command).contains("--cap-add=SETGID");
        assertThat(command).contains("--cap-add=DAC_OVERRIDE");
        assertThat(command).contains("--pids-limit=100");
        assertThat(command).contains("--memory=512m");

        // Porta dinamica (sem bind fixo no host)
        assertThat(command).contains("-p");
        assertThat(command).contains("0:5432");

        // Nao deve conter volumes do host (-v com caminho absoluto do host)
        assertThat(command.stream().noneMatch(arg -> arg.startsWith("-v") && arg.contains(":/"))).isTrue();
    }
}
