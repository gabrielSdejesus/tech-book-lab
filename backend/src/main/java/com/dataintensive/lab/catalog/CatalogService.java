package com.dataintensive.lab.catalog;

import com.dataintensive.lab.domain.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CatalogService {

    private final List<Book> books = new ArrayList<>();

    public CatalogService() {
        initCatalog();
    }

    public List<Book> getAllBooks() {
        return books;
    }

    public Optional<Book> findBookById(String bookId) {
        return books.stream().filter(b -> b.id().equalsIgnoreCase(bookId)).findFirst();
    }

    public Optional<Lab> findLabById(String labId) {
        return books.stream()
                .flatMap(b -> b.chapters().stream())
                .flatMap(c -> c.labs().stream())
                .filter(l -> l.id().equalsIgnoreCase(labId) || l.slug().equalsIgnoreCase(labId))
                .findFirst();
    }

    private void initCatalog() {
        // Book: Designing Data-Intensive Applications
        List<Lab> ch3Labs = List.of(
            createLab1(),
            createLab2(),
            createLab3(),
            createLab4()
        );

        Chapter ch3 = new Chapter(
            "ddia-cap-03",
            3,
            "Armazenamento e Recuperação de Dados",
            "Modelos de Dados, Grafos, OLAP e CQRS",
            "Explore as estruturas fundamentais que diferenciam bancos relacionais de documentos, travessia em grafos vs SQL recursivo, modelagem dimensional para analytics e CQRS baseado em log de eventos.",
            ch3Labs
        );

        Book ddia = new Book(
            "ddia",
            "Designing Data-Intensive Applications",
            "Martin Kleppmann",
            "O guia definitivo para arquitetar sistemas distribuídos, confiáveis e escaláveis.",
            "#059669",
            "Aprenda na prática os trade-offs fundamentais por trás dos motores de banco de dados, índices, replicação, particionamento e consistência.",
            List.of(ch3)
        );

        books.add(ddia);
    }

    private Lab createLab1() {
        String resetSql = """
            DROP TABLE IF EXISTS experiencias_profissionais CASCADE;
            DROP TABLE IF EXISTS formacoes_academicas CASCADE;
            DROP TABLE IF EXISTS usuarios CASCADE;
            DROP TABLE IF EXISTS usuarios_documento CASCADE;
            """;

        List<Challenge> challenges = List.of(
            new Challenge(
                "lab-01-ch-1",
                1,
                "Modelagem 3NF (Relacional Estrito)",
                "Crie o modelo 3NF para o perfil profissional (estilo LinkedIn) com as tabelas normalizadas e popule dados de teste.",
                "Cada usuário tem nome, bio, múltiplas experiências profissionais (cargo, empresa, ano início e fim) e formações acadêmicas.",
                """
                -- 1. Crie as tabelas normalizadas (usuarios, experiencias_profissionais, formacoes_academicas)
                CREATE TABLE usuarios (
                    id INT PRIMARY KEY,
                    nome VARCHAR(255),
                    bio VARCHAR(500)
                );

                -- Escreva aqui as tabelas dependentes e os INSERTs de teste...
                """,
                List.of(
                    "Modele chaves primárias e estrangeiras coerentes",
                    "Popule ao menos 2 usuários com experiências e formações",
                    "Escreva a query com LEFT JOIN para reconstruir o perfil completo e note a repetição dos campos do usuário"
                ),
                "Se uma empresa mudar de nome institucional, qual o impacto no modelo 3NF comparado a um modelo desnormalizado?"
            ),
            new Challenge(
                "lab-01-ch-2",
                2,
                "Modelagem Orientada a Documento (Postgres JSONB)",
                "Crie uma tabela com coluna JSONB e armazene todo o perfil de forma embutida aproveitando a localidade em disco.",
                "Compare a facilidade de busca direta por ID sem nenhum JOIN versus a complexidade de busca por elementos internos.",
                """
                -- Crie a tabela usuarios_documento (id, perfil JSONB)
                CREATE TABLE usuarios_documento (
                    id INT PRIMARY KEY,
                    perfil JSONB
                );

                -- Insira perfis contendo arrays de experiências e formações...
                """,
                List.of(
                    "Insira ao menos 2 perfis em formato JSONB estruturado",
                    "Faça uma busca direta por ID e compare o plano com os múltiplos joins do modelo relacional",
                    "Escreva uma consulta filtrando usuários que trabalharam em uma empresa específica usando operadores JSONB (ex: @> ou jsonb_array_elements)"
                ),
                "Qual é o trade-off entre localidade de leitura (ler tudo de uma vez) e custo de atualização de um dado compartilhado?"
            )
        );

        return new Lab(
            "ddia-cap-03-lab-01",
            1,
            "relacional-vs-documentos",
            "Relacional vs Documentos e Localidade de Armazenamento",
            "Analise a incompatibilidade objeto-relacional (impedance mismatch) e o contraste entre tabelas 3NF e colunas JSONB.",
            List.of("Impedance Mismatch", "Normalização 3NF", "Localidade de Armazenamento", "PostgreSQL JSONB"),
            EngineType.POSTGRES,
            "ddia_lab",
            resetSql,
            challenges
        );
    }

    private Lab createLab2() {
        String resetNeo4j = """
            MATCH (n) DETACH DELETE n;
            """;

        List<Challenge> challenges = List.of(
            new Challenge(
                "lab-02-ch-1",
                1,
                "Grafos de Propriedades com Cypher (Neo4j)",
                "Modele uma hierarquia geográfica de nós (Cidade -> Estado -> Região -> País) e conexões de amizade entre pessoas.",
                "Responda à pergunta: 'Quais amigos de Gabriel nasceram no Brasil?' sem fixar a quantidade de saltos na hierarquia.",
                """
                // Crie os nós de Person e Location e as arestas [:WITHIN], [:BORN_IN], [:FRIENDS_WITH]
                CREATE (c:Location {name: 'Sao Paulo', type: 'Cidade'}),
                       (s:Location {name: 'SP', type: 'Estado'}),
                       (b:Location {name: 'Brasil', type: 'Pais'})
                // Conecte com WITHIN e adicione Persons...
                """,
                List.of(
                    "Utilize arestas direcionadas para representar continência geográfica",
                    "Crie relacionamentos de amizade entre pessoas",
                    "Escreva uma consulta Cypher usando operador de profundidade variável (*1..5 ou *) para encontrar o país"
                ),
                "Por que o livro defende que modelos de grafos de propriedades são cognitivamente superiores para relações com número indefinido de saltos?"
            ),
            new Challenge(
                "lab-02-ch-2",
                2,
                "SQL Recursivo (Postgres WITH RECURSIVE)",
                "Resolva o mesmo problema hierárquico no PostgreSQL utilizando uma tabela auto-relacionada e Common Table Expression recursiva.",
                "Descubra se Gabriel nasceu no Brasil utilizando busca recursiva ascendente.",
                """
                -- Tabela auto-relacionada para hierarquia
                CREATE TABLE IF NOT EXISTS locais (
                    id INT PRIMARY KEY,
                    nome VARCHAR(100),
                    parent_id INT REFERENCES locais(id)
                );
                -- Escreva a CTE WITH RECURSIVE...
                """,
                List.of(
                    "Crie tabela de locais e pessoas associadas à sua cidade natal",
                    "Escreva uma consulta recursiva que suba de filho a pai até o nó raiz",
                    "Compare a legibilidade da query com a versão em Cypher"
                ),
                "Quais os desafios de performance e manutenção ao usar CTE recursiva para grafos densos em bancos relacionais?"
            )
        );

        return new Lab(
            "ddia-cap-03-lab-02",
            2,
            "grafos-propriedades",
            "Grafos de Propriedades vs SQL Recursivo",
            "Entenda como modelar relações complexas N:N e travessias com profundidade dinâmica em Neo4j vs PostgreSQL.",
            List.of("Property Graphs", "Cypher", "Fechamento Transitivo", "CTE Recursiva SQL"),
            EngineType.NEO4J,
            "neo4j",
            resetNeo4j,
            challenges
        );
    }

    private Lab createLab3() {
        String resetSql = """
            DROP TABLE IF EXISTS fato_vendas CASCADE;
            DROP TABLE IF EXISTS dim_tempo CASCADE;
            DROP TABLE IF EXISTS dim_produto CASCADE;
            DROP TABLE IF EXISTS dim_loja CASCADE;
            """;

        List<Challenge> challenges = List.of(
            new Challenge(
                "lab-03-ch-1",
                1,
                "Modelagem Dimensional (Star Schema)",
                "Crie as tabelas de dimensões desnormalizadas (dim_tempo, dim_produto, dim_loja) e a tabela de fatos central (fato_vendas).",
                "Construa o modelo de Data Warehouse voltado para análises rápidas cruzando tempo, produto e loja.",
                """
                -- Crie as tabelas de dimensões desnormalizadas
                CREATE TABLE dim_tempo (
                    id INT PRIMARY KEY,
                    data_completa DATE,
                    mes INT,
                    ano INT,
                    trimestre INT
                );
                -- Complete com dim_produto, dim_loja e fato_vendas...
                """,
                List.of(
                    "Utilize chaves substitutas (surrogate keys) nas dimensões",
                    "Na tabela fato, adicione chaves estrangeiras e métricas aditivas (quantidade, valor_total)",
                    "Insira dados de teste simulando vendas em diferentes períodos e lojas"
                ),
                "Por que no OLTP normalizamos dados, enquanto no Star Schema desnormalizamos intencionalmente as tabelas de dimensão?"
            ),
            new Challenge(
                "lab-03-ch-2",
                2,
                "Consultas Analíticas (Slice & Dice)",
                "Escreva consultas agregadoras típicas de BI sobre o Star Schema gerado.",
                "Agrupe faturamento total e quantidade vendida por ano, estado da loja e categoria de produto.",
                """
                -- Escreva a consulta analítica cruzando a tabela fato com as 3 dimensões
                SELECT dt.ano, dl.estado, dp.categoria, 
                       SUM(fv.valor_total) as faturamento_total,
                       SUM(fv.quantidade) as total_itens
                FROM fato_vendas fv
                -- Complete os JOINs e GROUP BY...
                """,
                List.of(
                    "Execute a query e analise o plano de execução (EXPLAIN ANALYZE)",
                    "Observe a simplicidade dos JOINs em estrela contra a complexidade de navegar um modelo OLTP 3NF",
                    "Teste a agregação com cláusula ROLLUP ou CUBE se desejar"
                ),
                "Como os bancos analíticos colunares (ex: ClickHouse, DuckDB, Redshift) otimizam ainda mais esse tipo de consulta em relação ao Postgres por linha?"
            )
        );

        return new Lab(
            "ddia-cap-03-lab-03",
            3,
            "modelagem-dimensional-olap",
            "Modelagem Dimensional para Análise (Star Schema / OLAP)",
            "Descubra as diferenças de projeto entre bancos transacionais (OLTP) e sistemas analíticos (OLAP) com Star Schema.",
            List.of("OLTP vs OLAP", "Star Schema", "Tabelas de Fatos", "Tabelas de Dimensões", "Slice and Dice"),
            EngineType.POSTGRES,
            "ddia_lab",
            resetSql,
            challenges
        );
    }

    private Lab createLab4() {
        String resetSql = """
            DROP MATERIALIZED VIEW IF EXISTS pedidos_resumo_leitura CASCADE;
            DROP TABLE IF EXISTS pedidos_resumo_leitura CASCADE;
            DROP TABLE IF EXISTS pedidos_eventos CASCADE;
            """;

        List<Challenge> challenges = List.of(
            new Challenge(
                "lab-04-ch-1",
                1,
                "Append-Only Event Store (Log de Eventos)",
                "Crie uma tabela de eventos imutáveis para registrar cada ação do ciclo de vida de um pedido.",
                "Grave eventos como PEDIDO_CRIADO, ITEM_ADICIONADO, DESCONTO_APLICADO, PAGAMENTO_CONFIRMADO e CANCELADO.",
                """
                -- Crie a tabela pedidos_eventos estritamente append-only
                CREATE TABLE pedidos_eventos (
                    id SERIAL PRIMARY KEY,
                    pedido_id INT NOT NULL,
                    tipo_evento VARCHAR(50) NOT NULL,
                    dados_evento JSONB NOT NULL,
                    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );

                -- Insira uma sequência realista de eventos para múltiplos pedidos...
                """,
                List.of(
                    "Garanta que a tabela represente fatos passados no tempo",
                    "Use JSONB para conter o payload flexível de cada tipo de evento",
                    "Simule múltiplos eventos evoluindo o estado do pedido"
                ),
                "Por que a imutabilidade do log de eventos simplifica auditoria e resolução de concorrência em sistemas distribuídos?"
            ),
            new Challenge(
                "lab-04-ch-2",
                2,
                "Projeção Derivada Otimizada para Leitura (CQRS)",
                "Crie uma Materialized View ou tabela de projeção que consolide os eventos em um estado final pronto para leitura rápida.",
                "Simule um novo evento no log, atualize a projeção e comprove a velocidade de leitura direta.",
                """
                -- Crie uma Materialized View agregando os eventos do log
                CREATE MATERIALIZED VIEW pedidos_resumo_leitura AS
                SELECT 
                    pedido_id,
                    -- Agregue as informações finais a partir do histórico de eventos...
                FROM pedidos_eventos
                GROUP BY pedido_id;
                """,
                List.of(
                    "Defina campos otimizados para exibição em tela (valor_total, status_final, itens)",
                    "Simule a inserção de um evento de cancelamento ou pagamento no log",
                    "Execute REFRESH MATERIALIZED VIEW e observe a consistência eventual"
                ),
                "De que forma essa arquitetura atende à definição formal de CQRS apresentada no livro de Martin Kleppmann?"
            )
        );

        return new Lab(
            "ddia-cap-03-lab-04",
            4,
            "cqrs-e-event-sourcing",
            "CQRS e Projeções Derivadas de um Log de Eventos",
            "Implemente o padrão de segregação de responsabilidade de leitura e escrita a partir de um log imutável de eventos.",
            List.of("Event Sourcing", "CQRS", "Append-Only Log", "Materialized Views", "Consistência Eventual"),
            EngineType.POSTGRES,
            "ddia_lab",
            resetSql,
            challenges
        );
    }
}
