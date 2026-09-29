import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Sidebar } from './Sidebar';
import type { Book, Lab } from '../types';

describe('Sidebar Component', () => {
  const mockLab1: Lab = {
    id: 'ddia-cap-03-lab-01',
    number: 1,
    slug: 'relacional-vs-documentos',
    title: 'Relacional vs Documentos',
    summary: 'Comparativo de modelos',
    keyConcepts: ['Impedance Mismatch'],
    engineType: 'POSTGRES',
    databaseName: 'ddia_lab',
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
        title: 'Armazenamento e Recuperação de Dados',
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
});
