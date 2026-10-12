package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.Book;
import com.dataintensive.lab.domain.Lab;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock
    private CatalogRepository catalogRepository;

    private CatalogService catalogService;

    @BeforeEach
    void setUp() {
        catalogService = new CatalogService(catalogRepository);
    }

    @Test
    @DisplayName("Deve delegar ao repositório para listar todos os livros")
    void shouldListAllBooks() {
        Book book = new Book("ddia", "Designing Data-Intensive Applications", "Martin Kleppmann", "Tag", "#059669", "/covers/ddia.svg", "Desc", List.of());
        when(catalogRepository.findAllBooks()).thenReturn(List.of(book));

        List<Book> books = catalogService.getAllBooks();

        assertThat(books).containsExactly(book);
        verify(catalogRepository).findAllBooks();
    }

    @Test
    @DisplayName("Deve delegar ao repositório para buscar livro existente")
    void shouldFindBookByIdWhenExists() {
        Book book = new Book("ddia", "Designing Data-Intensive Applications", "Martin Kleppmann", "Tag", "#059669", "/covers/ddia.svg", "Desc", List.of());
        when(catalogRepository.findBookById("ddia")).thenReturn(Optional.of(book));

        Optional<Book> result = catalogService.findBookById("ddia");

        assertThat(result).contains(book);
        verify(catalogRepository).findBookById("ddia");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para retornar vazio ao buscar livro inexistente")
    void shouldReturnEmptyWhenBookDoesNotExist() {
        when(catalogRepository.findBookById("livro-inexistente")).thenReturn(Optional.empty());

        Optional<Book> result = catalogService.findBookById("livro-inexistente");

        assertThat(result).isEmpty();
        verify(catalogRepository).findBookById("livro-inexistente");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para encontrar Lab por ID")
    void shouldFindLabById() {
        Lab lab = new Lab("ddia-cap-03-lab-01", 1, "relacional-vs-documentos", "Relacional", "Sum", List.of(), "tbl_lab", "DROP TABLE...", List.of());
        when(catalogRepository.findLabById("ddia-cap-03-lab-01")).thenReturn(Optional.of(lab));

        Optional<Lab> result = catalogService.findLabById("ddia-cap-03-lab-01");

        assertThat(result).contains(lab);
        verify(catalogRepository).findLabById("ddia-cap-03-lab-01");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para encontrar Lab por slug")
    void shouldFindLabBySlug() {
        Lab lab = new Lab("ddia-cap-03-lab-01", 1, "relacional-vs-documentos", "Relacional", "Sum", List.of(), "tbl_lab", "DROP TABLE...", List.of());
        when(catalogRepository.findLabById("relacional-vs-documentos")).thenReturn(Optional.of(lab));

        Optional<Lab> result = catalogService.findLabById("relacional-vs-documentos");

        assertThat(result).contains(lab);
        verify(catalogRepository).findLabById("relacional-vs-documentos");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para listar todos os livros no idioma especificado")
    void shouldListAllBooksWithLocale() {
        Book book = new Book("ddia", "Designing Data-Intensive Applications", "Martin Kleppmann", "Tag", "#059669", "/covers/ddia.svg", "Desc", List.of());
        when(catalogRepository.findAllBooks("en")).thenReturn(List.of(book));

        List<Book> books = catalogService.getAllBooks("en");

        assertThat(books).containsExactly(book);
        verify(catalogRepository).findAllBooks("en");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para buscar livro por ID no idioma especificado")
    void shouldFindBookByIdWithLocale() {
        Book book = new Book("ddia", "Designing Data-Intensive Applications", "Martin Kleppmann", "Tag", "#059669", "/covers/ddia.svg", "Desc", List.of());
        when(catalogRepository.findBookById("ddia", "en")).thenReturn(Optional.of(book));

        Optional<Book> result = catalogService.findBookById("ddia", "en");

        assertThat(result).contains(book);
        verify(catalogRepository).findBookById("ddia", "en");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para encontrar Lab por ID no idioma especificado")
    void shouldFindLabByIdWithLocale() {
        Lab lab = new Lab("ddia-cap-03-lab-01", 1, "relacional-vs-documentos", "Relational", "Sum", List.of(), "tbl_lab", "DROP TABLE...", List.of());
        when(catalogRepository.findLabById("ddia-cap-03-lab-01", "en")).thenReturn(Optional.of(lab));

        Optional<Lab> result = catalogService.findLabById("ddia-cap-03-lab-01", "en");

        assertThat(result).contains(lab);
        verify(catalogRepository).findLabById("ddia-cap-03-lab-01", "en");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para salvar solução de desafio")
    void shouldDelegateSaveChallengeSolution() {
        catalogService.saveChallengeSolution("lab-01-ch-1", "SELECT 1;");
        verify(catalogRepository).saveChallengeSolution("lab-01-ch-1", "SELECT 1;", null);
    }

    @Test
    @DisplayName("Deve delegar ao repositório para salvar solução de desafio com userReflection")
    void shouldDelegateSaveChallengeSolutionWithReflection() {
        catalogService.saveChallengeSolution("lab-01-ch-1", "SELECT 1;", "Minha reflexão");
        verify(catalogRepository).saveChallengeSolution("lab-01-ch-1", "SELECT 1;", "Minha reflexão");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para deletar solução de desafio")
    void shouldDelegateDeleteChallengeSolution() {
        catalogService.deleteChallengeSolution("lab-01-ch-1");
        verify(catalogRepository).deleteChallengeSolution("lab-01-ch-1");
    }

    @Test
    @DisplayName("Deve preservar os metadados de expectedReflection nos desafios ao retornar Lab por ID")
    void shouldPreserveExpectedReflectionInLabChallenges() {
        var challenge = new com.dataintensive.lab.domain.Challenge("ch-1", 1, "Titulo", "Desc", "Cenario", "SELECT 1;", null, null, List.of(), "Reflexao", "Gabarito oficial de trade-off");
        var lab = new Lab("lab-1", 1, "slug", "Lab 1", "Sum", List.of(), "db", "SQL", List.of(challenge));

        when(catalogRepository.findLabById("lab-1", "pt")).thenReturn(Optional.of(lab));

        Optional<Lab> result = catalogService.findLabById("lab-1", "pt");

        assertThat(result).isPresent();
        assertThat(result.get().challenges().get(0).expectedReflection()).isEqualTo("Gabarito oficial de trade-off");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para buscar cenários de um laboratório")
    void shouldFindScenariosByLabId() {
        var scenario = new com.dataintensive.lab.domain.InteractiveScenario(
                "sc-1", "lab-1", com.dataintensive.lab.domain.ScenarioType.CANVAS_MODELING,
                com.dataintensive.lab.domain.Difficulty.MEDIUM, 1,
                "Cenário 1", "Descrição", "Briefing", "Gabarito",
                "{}", "{}", "{}", "{}", "{}", "IN_PROGRESS"
        );
        when(catalogRepository.findScenariosByLabId("lab-1", "pt")).thenReturn(List.of(scenario));

        List<com.dataintensive.lab.domain.InteractiveScenario> result = catalogService.getScenariosByLabId("lab-1", "pt");

        assertThat(result).containsExactly(scenario);
        verify(catalogRepository).findScenariosByLabId("lab-1", "pt");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para buscar cenário por ID")
    void shouldFindScenarioById() {
        var scenario = new com.dataintensive.lab.domain.InteractiveScenario(
                "sc-1", "lab-1", com.dataintensive.lab.domain.ScenarioType.CANVAS_MODELING,
                com.dataintensive.lab.domain.Difficulty.MEDIUM, 1,
                "Cenário 1", "Descrição", "Briefing", "Gabarito",
                "{}", "{}", "{}", "{}", "{}", "IN_PROGRESS"
        );
        when(catalogRepository.findScenarioById("sc-1", "en")).thenReturn(Optional.of(scenario));

        Optional<com.dataintensive.lab.domain.InteractiveScenario> result = catalogService.getScenarioById("sc-1", "en");

        assertThat(result).contains(scenario);
        verify(catalogRepository).findScenarioById("sc-1", "en");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para salvar solução de cenário interativo")
    void shouldSaveScenarioSolution() {
        var request = new com.dataintensive.lab.catalog.dto.ScenarioSolutionRequest("{\"nodes\":[]}", "Minha justificativa técnica");

        catalogService.saveScenarioSolution("sc-1", request);

        verify(catalogRepository).saveScenarioSolution("sc-1", "{\"nodes\":[]}", "Minha justificativa técnica", "IN_PROGRESS");
    }

    @Test
    @DisplayName("Deve delegar ao repositório para buscar quiz de capítulo")
    void shouldFindChapterQuiz() {
        var quiz = new com.dataintensive.lab.domain.ChapterQuiz("chap-1", List.of(), 0, 0, 0);
        when(catalogRepository.findQuizByChapterId("chap-1", "pt")).thenReturn(Optional.of(quiz));

        Optional<com.dataintensive.lab.domain.ChapterQuiz> result = catalogService.getChapterQuiz("chap-1", "pt");

        assertThat(result).contains(quiz);
        verify(catalogRepository).findQuizByChapterId("chap-1", "pt");
    }

    @Test
    @DisplayName("Deve submeter quiz de capítulo e calcular pontuação corretamente")
    void shouldSubmitChapterQuizAndCalculateScore() {
        var opt1 = new com.dataintensive.lab.domain.QuizOption("opt-1", "q-1", "A", "Opção 1", true);
        var opt2 = new com.dataintensive.lab.domain.QuizOption("opt-2", "q-1", "B", "Opção 2", false);
        var q1 = new com.dataintensive.lab.domain.QuizQuestion(
                "q-1", "chap-1", 1, com.dataintensive.lab.domain.Difficulty.EASY,
                "Pergunta 1", "Explicação 1", List.of(opt1, opt2), null, null
        );

        var opt3 = new com.dataintensive.lab.domain.QuizOption("opt-3", "q-2", "A", "Opção 3", true);
        var opt4 = new com.dataintensive.lab.domain.QuizOption("opt-4", "q-2", "B", "Opção 4", false);
        var q2 = new com.dataintensive.lab.domain.QuizQuestion(
                "q-2", "chap-1", 2, com.dataintensive.lab.domain.Difficulty.MEDIUM,
                "Pergunta 2", "Explicação 2", List.of(opt3, opt4), null, null
        );

        var quiz = new com.dataintensive.lab.domain.ChapterQuiz("chap-1", List.of(q1, q2), 2, 0, 0);
        when(catalogRepository.findQuizByChapterId("chap-1", "pt")).thenReturn(Optional.of(quiz));

        var submission = new com.dataintensive.lab.catalog.dto.QuizSubmissionRequest(
                java.util.Map.of("q-1", "opt-1", "q-2", "opt-4")
        );

        var response = catalogService.submitChapterQuiz("chap-1", submission);

        assertThat(response.chapterId()).isEqualTo("chap-1");
        assertThat(response.totalQuestions()).isEqualTo(2);
        assertThat(response.correctAnswers()).isEqualTo(1);
        assertThat(response.scorePercentage()).isEqualTo(50.0);
        assertThat(response.results()).hasSize(2);

        var res1 = response.results().stream().filter(r -> r.questionId().equals("q-1")).findFirst().get();
        assertThat(res1.isCorrect()).isTrue();
        assertThat(res1.selectedOptionId()).isEqualTo("opt-1");
        assertThat(res1.correctOptionId()).isEqualTo("opt-1");
        assertThat(res1.explanation()).isEqualTo("Explicação 1");

        var res2 = response.results().stream().filter(r -> r.questionId().equals("q-2")).findFirst().get();
        assertThat(res2.isCorrect()).isFalse();
        assertThat(res2.selectedOptionId()).isEqualTo("opt-4");
        assertThat(res2.correctOptionId()).isEqualTo("opt-3");
        assertThat(res2.explanation()).isEqualTo("Explicação 2");

        verify(catalogRepository).saveQuizSubmission("chap-1", "q-1", "opt-1", true);
        verify(catalogRepository).saveQuizSubmission("chap-1", "q-2", "opt-4", false);
    }

    @Test
    @DisplayName("Deve lançar DomainValidationException ao submeter quiz inexistente")
    void shouldThrowExceptionWhenSubmittingNonExistentQuiz() {
        when(catalogRepository.findQuizByChapterId("chap-inexistente", "pt")).thenReturn(Optional.empty());

        var submission = new com.dataintensive.lab.catalog.dto.QuizSubmissionRequest(java.util.Map.of());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> catalogService.submitChapterQuiz("chap-inexistente", submission))
                .isInstanceOf(com.dataintensive.lab.domain.DomainValidationException.class)
                .hasMessageContaining("Capítulo não encontrado ou não possui simulado");
    }
}

