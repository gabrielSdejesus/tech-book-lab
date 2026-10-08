import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { Bookshelf } from './Bookshelf';
import { LanguageProvider } from '../i18n/LanguageContext';
import type { Book } from '../types';


const mockBooks: Book[] = [
  {
    id: 'ddia',
    title: 'Designing Data-Intensive Applications',
    author: 'Martin Kleppmann',
    tagLine: 'O guia definitivo para arquitetar sistemas distribuídos, confiáveis e escaláveis.',
    coverColor: '#059669',
    coverImageUrl: '/covers/ddia.svg',
    description: 'Aprenda na prática os trade-offs fundamentais por trás dos motores de banco de dados.',
    chapters: [
      {
        id: 'ddia-cap-03',
        number: 3,
        title: 'Modelos de Dados e Linguagens de Consulta',
        subtitle: 'Modelos de Dados, Grafos, OLAP e CQRS',
        summary: 'Explore as estruturas fundamentais.',
        labs: [
          {
            id: 'ddia-cap-03-lab-01',
            number: 1,
            slug: 'relacional-vs-documentos',
            title: 'Relacional vs Documentos',
            summary: 'Analise a incompatibilidade.',
            keyConcepts: ['Impedance Mismatch'],
            engineType: 'POSTGRES',
            databaseName: 'tbl_lab',
            resetSchemaSql: 'DROP TABLE...',
            challenges: [],
          },
        ],
      },
    ],
  },
];

describe('Bookshelf Component', () => {
  it('deve renderizar o cabeçalho e estatísticas da biblioteca', () => {
    render(<Bookshelf books={mockBooks} onSelectBook={vi.fn()} />);

    expect(screen.getByText(/Biblioteca de Livros Técnicos/i)).toBeInTheDocument();
    expect(screen.getByText(/1 Livro Disponível/i)).toBeInTheDocument();
  });

  it('deve exibir informações do livro e capa clássica', () => {
    render(<Bookshelf books={mockBooks} onSelectBook={vi.fn()} />);

    expect(screen.getByText('Designing Data-Intensive Applications')).toBeInTheDocument();
    expect(screen.getByText(/Martin Kleppmann/i)).toBeInTheDocument();
    expect(screen.getByText(/O guia definitivo para arquitetar sistemas/i)).toBeInTheDocument();
  });

  it('deve disparar onSelectBook ao clicar no card ou botão do livro', () => {
    const handleSelectBook = vi.fn();
    render(<Bookshelf books={mockBooks} onSelectBook={handleSelectBook} />);

    const openButton = screen.getByRole('button', { name: /Abrir Caderno de Laboratório/i });
    fireEvent.click(openButton);

    expect(handleSelectBook).toHaveBeenCalledTimes(1);
    expect(handleSelectBook).toHaveBeenCalledWith(mockBooks[0]);
  });

  it('deve renderizar os textos em inglês quando o idioma for en', () => {
    localStorage.setItem('tbl_locale', 'en');

    render(
      <LanguageProvider>
        <Bookshelf books={mockBooks} onSelectBook={vi.fn()} />
      </LanguageProvider>
    );

    expect(screen.getByText(/Technical Books Library/i)).toBeInTheDocument();
    expect(screen.getByText(/1 Available Book/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Open Lab Notebook/i })).toBeInTheDocument();
  });

  it('deve possuir role="button", tabIndex={0} e permitir seleção de livro via teclado com Enter e Space', () => {
    const handleSelectBook = vi.fn();
    render(<Bookshelf books={mockBooks} onSelectBook={handleSelectBook} />);

    const bookCards = screen.getAllByRole('button', { name: new RegExp(mockBooks[0].title, 'i') });
    const card = bookCards[0];

    expect(card).toHaveAttribute('tabIndex', '0');

    fireEvent.keyDown(card, { key: 'Enter' });
    expect(handleSelectBook).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(card, { key: ' ' });
    expect(handleSelectBook).toHaveBeenCalledTimes(2);
  });

  it('deve estilizar badges de motores dinamicamente a partir do engineConfig, incluindo novos motores', () => {
    const customBook: Book = {
      ...mockBooks[0],
      chapters: [
        {
          ...mockBooks[0].chapters[0],
          labs: [
            ...mockBooks[0].chapters[0].labs,
            {
              ...mockBooks[0].chapters[0].labs[0],
              id: 'custom-lab-redis',
              engineType: 'REDIS' as any,
            },
          ],
        },
      ],
    };

    render(<Bookshelf books={[customBook]} onSelectBook={vi.fn()} />);

    const redisBadge = screen.getByText('REDIS');
    expect(redisBadge).toBeInTheDocument();
    expect(redisBadge).toHaveClass('bg-stone-100');
    expect(redisBadge).not.toHaveClass('bg-[#e5ebe4]');
  });

  it('deve exibir mensagem de catálogo vazio quando a lista de livros for vazia', () => {
    render(<Bookshelf books={[]} onSelectBook={vi.fn()} />);

    expect(screen.getByText(/Nenhum livro técnico encontrado no catálogo/i)).toBeInTheDocument();
    expect(screen.getByText(/0 Livros Disponíveis/i)).toBeInTheDocument();
  });
});



