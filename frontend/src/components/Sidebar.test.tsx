import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Sidebar } from './Sidebar';
import type { Book, Lab } from '../types';
import { LanguageProvider } from '../i18n/LanguageContext';

describe('Sidebar Component', () => {
  beforeEach(() => {
    localStorage.clear();
  });
  const mockLab1: Lab = {
    id: 'ddia-cap-03-lab-01',
    number: 1,
    slug: 'relacional-vs-documentos',
    title: 'Relacional vs Documentos',
    summary: 'Comparativo de modelos',
    keyConcepts: ['Impedance Mismatch'],
    engineType: 'POSTGRES',
    databaseName: 'tbl_lab',
    resetSchemaSql: 'DROP TABLE IF EXISTS...',
    challenges: []
  };

  const mockLab2: Lab = {
    id: 'ddia-cap-03-lab-02',
    number: 2,
    slug: 'grafos-propriedades',
    title: 'Grafos de Propriedades',
    summary: 'Neo4j vs SQL recursivo',
    keyConcepts: ['Cypher'],
    engineType: 'NEO4J',
    databaseName: 'neo4j',
    resetSchemaSql: 'MATCH (n) DETACH DELETE n;',
    challenges: []
  };

  const mockBook: Book = {
    id: 'ddia',
    title: 'Designing Data-Intensive Applications',
    author: 'Martin Kleppmann',
    tagLine: 'Guia definitivo',
    coverColor: '#059669',
    description: 'Resumo geral',
    chapters: [
      {
        id: 'ddia-cap-03',
        number: 3,
        title: 'Modelos de Dados e Linguagens de Consulta',
        subtitle: 'Modelos de Dados',
        summary: 'Resumo cap 3',
        labs: [mockLab1, mockLab2]
      }
    ]
  };

  it('deve renderizar a Tábua de Matérias e os dados do livro', () => {
    render(<Sidebar books={[mockBook]} selectedLab={mockLab1} onSelectLab={vi.fn()} />);

    expect(screen.getByText(/TÁBUA DE MATÉRIAS/i)).toBeInTheDocument();
    expect(screen.getByText('Designing Data-Intensive Applications')).toBeInTheDocument();
    expect(screen.getByText(/Por Martin Kleppmann/i)).toBeInTheDocument();
    expect(screen.getByText(/CAPÍTULO 3/i)).toBeInTheDocument();
    expect(screen.getByText('Modelos de Dados e Linguagens de Consulta')).toBeInTheDocument();
  });

  it('deve listar os laboratórios e chamar onSelectLab ao clicar', () => {
    const handleSelectLab = vi.fn();
    render(<Sidebar books={[mockBook]} selectedLab={mockLab1} onSelectLab={handleSelectLab} />);

    expect(screen.getByText(/Relacional vs Documentos/i)).toBeInTheDocument();
    const lab2Button = screen.getByText(/Grafos de Propriedades/i);
    expect(lab2Button).toBeInTheDocument();

    fireEvent.click(lab2Button);
    expect(handleSelectLab).toHaveBeenCalledWith(mockLab2);
  });

  it('deve aplicar classes de recolhimento quando isOpen for false', () => {
    render(
      <Sidebar
        books={[mockBook]}
        selectedLab={mockLab1}
        onSelectLab={vi.fn()}
        isOpen={false}
      />
    );

    const aside = screen.getByTestId('sidebar');
    expect(aside).toHaveClass('w-0');
    expect(aside).toHaveAttribute('aria-hidden', 'true');
  });

  it('deve disparar onToggle ao clicar no botão de recolher da barra lateral', () => {
    const handleToggle = vi.fn();
    render(
      <Sidebar
        books={[mockBook]}
        selectedLab={mockLab1}
        onSelectLab={vi.fn()}
        isOpen={true}
        onToggle={handleToggle}
      />
    );

    const collapseBtn = screen.getByRole('button', { name: /Recolher tábua de matérias/i });
    fireEvent.click(collapseBtn);

    expect(handleToggle).toHaveBeenCalledTimes(1);
  });

  it('deve renderizar os textos e acessibilidade em inglês quando o idioma for en', () => {
    localStorage.setItem('tbl_locale', 'en');
    render(
      <LanguageProvider>
        <Sidebar
          books={[mockBook]}
          selectedLab={mockLab1}
          onSelectLab={vi.fn()}
          isOpen={true}
          onToggle={vi.fn()}
        />
      </LanguageProvider>
    );

    expect(screen.getByText('TABLE OF CONTENTS')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Collapse table of contents/i })).toBeInTheDocument();
    expect(screen.getByText(/By Martin Kleppmann/i)).toBeInTheDocument();
    expect(screen.getByText(/CHAPTER 3/i)).toBeInTheDocument();
  });

  it('deve renderizar a numeração de seções dinamicamente conforme o capítulo pai (ex: § 1.1, § 1.2 e § 4.1)', () => {
    const multiChapterBook: Book = {
      id: 'dist-sys',
      title: 'Distributed Systems Principles',
      author: 'Andrew Tanenbaum',
      tagLine: 'Conceitos fundamentais',
      coverColor: '#1d4ed8',
      description: 'Sistemas distribuídos modernos',
      chapters: [
        {
          id: 'ds-cap-01',
          number: 1,
          title: 'Introdução e Arquiteturas',
          subtitle: 'Visão Geral',
          summary: 'Conceitos de sistemas distribuídos',
          labs: [
            { ...mockLab1, id: 'lab-1-1', number: 1, title: 'Modelos de Comunicação' },
            { ...mockLab2, id: 'lab-1-2', number: 2, title: 'Chamadas RPC' }
          ]
        },
        {
          id: 'ds-cap-04',
          number: 4,
          title: 'Replicação e Consistência',
          subtitle: 'Consistência',
          summary: 'Quóruns e Paxos',
          labs: [
            { ...mockLab1, id: 'lab-4-1', number: 1, title: 'Consenso Distribuído' }
          ]
        }
      ]
    };

    render(
      <Sidebar
        books={[multiChapterBook]}
        selectedLab={multiChapterBook.chapters[0].labs[0]}
        onSelectLab={vi.fn()}
      />
    );

    // Valida que os rótulos de seção respeitam o número do capítulo pai
    expect(screen.getByText(/§\s*1\.1/)).toBeInTheDocument();
    expect(screen.getByText(/§\s*1\.2/)).toBeInTheDocument();
    expect(screen.getByText(/§\s*4\.1/)).toBeInTheDocument();

    // Garante ausência de prefixo hardcoded § 3.
    expect(screen.queryByText(/§\s*3\./)).not.toBeInTheDocument();
  });
});
