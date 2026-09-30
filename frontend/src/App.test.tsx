import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { App } from './App';
import * as api from './services/api';
import type { Book, InfraStatus } from './types';

const mockBook: Book = {
  id: 'ddia',
  title: 'Designing Data-Intensive Applications',
  author: 'Martin Kleppmann',
  tagLine: 'O guia definitivo.',
  coverColor: '#059669',
  coverImageUrl: '/covers/ddia.svg',
  description: 'Aprenda na prática.',
  chapters: [
    {
      id: 'ddia-cap-03',
      number: 3,
      title: 'Modelos de Dados',
      subtitle: 'Subtítulo',
      summary: 'Resumo',
      labs: [
        {
          id: 'ddia-cap-03-lab-01',
          number: 1,
          slug: 'relacional-vs-documentos',
          title: 'Relacional vs Documentos',
          summary: 'Lab summary',
          keyConcepts: ['Concept1'],
          engineType: 'POSTGRES',
          databaseName: 'tbl_lab',
          resetSchemaSql: 'DROP TABLE...',
          challenges: [
            {
              id: 'ch-1',
              order: 1,
              title: 'Desafio 1',
              description: 'Desc',
              scenario: 'Cenário',
              starterTemplate: 'SELECT 1;',
              guidelines: [],
              reflectionPrompt: 'Prompt',
            },
          ],
        },
      ],
    },
  ],
};

const mockInfra: InfraStatus = {
  postgresReady: true,
  postgresMessage: 'Postgres OK',
  neo4jReady: true,
  neo4jMessage: 'Neo4j OK',
  timestamp: 123456789,
};

describe('App Component Flow', () => {
  beforeEach(() => {
    vi.spyOn(api, 'getBooks').mockResolvedValue([mockBook]);
    vi.spyOn(api, 'getInfraStatus').mockResolvedValue(mockInfra);
  });

  it('deve carregar inicialmente na Bookshelf e permitir navegar para o LabWorkspace e retornar', async () => {
    render(<App />);

    // Waits for books to load and displays Bookshelf
    await waitFor(() => {
      expect(screen.getByText(/Biblioteca de Livros Técnicos/i)).toBeInTheDocument();
    });

    expect(screen.getByText('Designing Data-Intensive Applications')).toBeInTheDocument();

    // Click "Abrir Caderno de Laboratório"
    const openBtn = screen.getByRole('button', { name: /Abrir Caderno de Laboratório/i });
    fireEvent.click(openBtn);

    // Verify it transitioned to Workspace view (Sidebar is rendered)
    await waitFor(() => {
      expect(screen.getByText(/TÁBUA DE MATÉRIAS/i)).toBeInTheDocument();
    });

    // In Workspace, Navbar should have "ESTANTE DE LIVROS"
    const backToBookshelfBtn = screen.getByRole('button', { name: /ESTANTE DE LIVROS/i });
    expect(backToBookshelfBtn).toBeInTheDocument();

    // Click to return to Bookshelf
    fireEvent.click(backToBookshelfBtn);

    // Verify we are back on Bookshelf
    await waitFor(() => {
      expect(screen.getByText(/Biblioteca de Livros Técnicos/i)).toBeInTheDocument();
    });
  });
});
