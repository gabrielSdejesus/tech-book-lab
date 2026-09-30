import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { BookCover } from './BookCover';
import type { Book } from '../types';

const sampleBook: Book = {
  id: 'ddia',
  title: 'Designing Data-Intensive Applications',
  author: 'Martin Kleppmann',
  tagLine: 'O guia definitivo.',
  coverColor: '#059669',
  coverImageUrl: '/covers/ddia.svg',
  description: 'Aprenda na prática.',
  chapters: [],
};

describe('BookCover Component', () => {
  it('deve renderizar a imagem de capa quando coverImageUrl estiver presente', () => {
    render(<BookCover book={sampleBook} />);

    const img = screen.getByRole('img', { name: /Capa do livro Designing Data-Intensive Applications/i });
    expect(img).toBeInTheDocument();
    expect(img).toHaveAttribute('src', '/covers/ddia.svg');
  });

  it('deve renderizar o fallback tipográfico quando a imagem falhar ao carregar', () => {
    render(<BookCover book={sampleBook} />);

    const img = screen.getByRole('img', { name: /Capa do livro Designing Data-Intensive Applications/i });
    fireEvent.error(img);

    expect(screen.getByText('Designing Data-Intensive Applications')).toBeInTheDocument();
    expect(screen.getByText('Martin Kleppmann')).toBeInTheDocument();
  });

  it('deve renderizar o fallback quando coverImageUrl não for informado', () => {
    const bookWithoutImage: Book = { ...sampleBook, coverImageUrl: undefined };
    render(<BookCover book={bookWithoutImage} />);

    expect(screen.getByText('Designing Data-Intensive Applications')).toBeInTheDocument();
    expect(screen.getByText('Martin Kleppmann')).toBeInTheDocument();
  });
});
