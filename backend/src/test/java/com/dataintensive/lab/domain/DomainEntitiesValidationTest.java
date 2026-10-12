package com.dataintensive.lab.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainEntitiesValidationTest {

    @Test
    @DisplayName("Deve instanciar Book e Chapter com integridade de campos e coleções")
    void shouldValidateBookAndChapterEntities() {
        Challenge challenge = new Challenge(
                "ch-1", 1, "Desafio 1", "Desc", "Cenário",
                "SELECT 1;", List.of("Guia 1"), "Reflexão"
        );

        Lab lab = new Lab(
                "ddia-cap-03-lab-01", 1, "relacional", "Lab 1", "Sumário",
                List.of("Conceito 1"), "tbl_lab", "DROP TABLE IF EXISTS t;",
                List.of(challenge)
        );

        Chapter chapter = new Chapter("ddia-cap-03", 3, "Capítulo 3", "Subtítulo", "Sumário Cap", List.of(lab));
        Book book = new Book("ddia", "DDIA", "Martin Kleppmann", "Tagline", "#059669", "/covers/ddia.svg", "Desc", List.of(chapter));

        assertThat(book.id()).isEqualTo("ddia");
        assertThat(book.title()).isEqualTo("DDIA");
        assertThat(book.author()).isEqualTo("Martin Kleppmann");
        assertThat(book.tagLine()).isEqualTo("Tagline");
        assertThat(book.coverColor()).isEqualTo("#059669");
        assertThat(book.coverImageUrl()).isEqualTo("/covers/ddia.svg");
        assertThat(book.description()).isEqualTo("Desc");
        assertThat(book.chapters()).containsExactly(chapter);

        assertThat(chapter.id()).isEqualTo("ddia-cap-03");
        assertThat(chapter.number()).isEqualTo(3);
        assertThat(chapter.title()).isEqualTo("Capítulo 3");
        assertThat(chapter.subtitle()).isEqualTo("Subtítulo");
        assertThat(chapter.summary()).isEqualTo("Sumário Cap");
        assertThat(chapter.labs()).containsExactly(lab);
    }

    @Test
    @DisplayName("Deve instanciar Lab com propriedades consistentes")
    void shouldValidateLabEntity() {
        Lab lab = new Lab(
                "ddia-cap-03-lab-02", 2, "grafos", "Grafos", "Sumário Lab 2",
                List.of("Property Graphs"), "tbl_graphs", "MATCH (n) DETACH DELETE n;",
                List.of()
        );

        assertThat(lab.id()).isEqualTo("ddia-cap-03-lab-02");
        assertThat(lab.number()).isEqualTo(2);
        assertThat(lab.slug()).isEqualTo("grafos");
        assertThat(lab.title()).isEqualTo("Grafos");
        assertThat(lab.summary()).isEqualTo("Sumário Lab 2");
        assertThat(lab.keyConcepts()).containsExactly("Property Graphs");
        assertThat(lab.databaseName()).isEqualTo("tbl_graphs");
        assertThat(lab.resetSchemaSql()).isEqualTo("MATCH (n) DETACH DELETE n;");
        assertThat(lab.challenges()).isEmpty();
    }

    @Test
    @DisplayName("Deve validar construtores canônico e sobrecarregados de Challenge")
    void shouldValidateChallengeConstructors() {
        Challenge c1 = new Challenge(
                "ch-1", 1, "Titulo", "Desc", "Cenário",
                "STARTER", "SAVED", List.of("G1"), "Prompt"
        );
        assertThat(c1.savedCode()).isEqualTo("SAVED");

        Challenge c2 = new Challenge(
                "ch-2", 2, "Titulo 2", "Desc 2", "Cenário 2",
                "STARTER 2", List.of("G2"), "Prompt 2"
        );
        assertThat(c2.savedCode()).isNull();

        Challenge c3 = new Challenge(
                "ch-3", 3, "Titulo 3", "Desc 3", "Cenário 3",
                "STARTER 3", List.of("G3"), "Prompt 3"
        );
        assertThat(c3.savedCode()).isNull();
    }

    @Test
    @DisplayName("Deve resolver AssessmentLanguage com valores válidos, defaults e disparar exceção para inválidos")
    void shouldValidateAssessmentLanguageResolution() {
        assertThat(AssessmentLanguage.from("pt")).isEqualTo(AssessmentLanguage.PT);
        assertThat(AssessmentLanguage.from("pt-BR")).isEqualTo(AssessmentLanguage.PT);
        assertThat(AssessmentLanguage.from("PT")).isEqualTo(AssessmentLanguage.PT);

        assertThat(AssessmentLanguage.from("en")).isEqualTo(AssessmentLanguage.EN);
        assertThat(AssessmentLanguage.from("en-US")).isEqualTo(AssessmentLanguage.EN);
        assertThat(AssessmentLanguage.from("EN")).isEqualTo(AssessmentLanguage.EN);

        assertThat(AssessmentLanguage.from(null)).isEqualTo(AssessmentLanguage.PT);
        assertThat(AssessmentLanguage.from("")).isEqualTo(AssessmentLanguage.PT);
        assertThat(AssessmentLanguage.from("   ")).isEqualTo(AssessmentLanguage.PT);

        assertThatThrownBy(() -> AssessmentLanguage.from("es"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Idioma 'es' não suportado");
    }

    @Test
    @DisplayName("Deve validar exceção de domínio DomainValidationException")
    void shouldValidateDomainExceptions() {
        DomainValidationException dve = new DomainValidationException("Erro de domínio");
        assertThat(dve.getMessage()).isEqualTo("Erro de domínio");
    }
}
