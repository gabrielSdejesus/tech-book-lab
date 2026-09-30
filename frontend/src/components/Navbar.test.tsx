import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Navbar } from './Navbar';
import type { InfraStatus } from '../types';

describe('Navbar Component', () => {
  const defaultStatus: InfraStatus = {
    postgresReady: true,
    postgresMessage: 'Postgres ativo',
    neo4jReady: true,
    neo4jMessage: 'Neo4j ativo',
    timestamp: Date.now()
  };

  it('deve renderizar o título do sistema e o livro selecionado', () => {
    render(
      <Navbar
        infraStatus={defaultStatus}
        loadingInfra={false}
        onRefreshInfra={vi.fn()}
        onOpenSettings={vi.fn()}
        selectedBookTitle="Designing Data-Intensive Applications"
        theme="dark"
        onToggleTheme={vi.fn()}
      />
    );

    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/Tech Book/i);
    expect(screen.getByText(/Lab/i)).toBeInTheDocument();
    expect(screen.getByText(/TBL/i)).toBeInTheDocument();
    expect(screen.getByText(/Designing Data-Intensive Applications/i)).toBeInTheDocument();
  });

  it('deve alternar o tema ao clicar no botão de tema', () => {
    const handleToggle = vi.fn();
    render(
      <Navbar
        infraStatus={defaultStatus}
        loadingInfra={false}
        onRefreshInfra={vi.fn()}
        onOpenSettings={vi.fn()}
        selectedBookTitle="DDIA"
        theme="dark"
        onToggleTheme={handleToggle}
      />
    );

    const themeButton = screen.getByRole('button', { name: /ESCURO/i });
    expect(themeButton).toBeInTheDocument();

    fireEvent.click(themeButton);
    expect(handleToggle).toHaveBeenCalledTimes(1);
  });

  it('deve exibir botão CLARO quando o tema for light', () => {
    render(
      <Navbar
        infraStatus={defaultStatus}
        loadingInfra={false}
        onRefreshInfra={vi.fn()}
        onOpenSettings={vi.fn()}
        selectedBookTitle="DDIA"
        theme="light"
        onToggleTheme={vi.fn()}
      />
    );

    expect(screen.getByRole('button', { name: /CLARO/i })).toBeInTheDocument();
  });

  it('deve abrir as configurações de IA ao clicar no botão correspondente', () => {
    const handleOpenSettings = vi.fn();
    render(
      <Navbar
        infraStatus={defaultStatus}
        loadingInfra={false}
        onRefreshInfra={vi.fn()}
        onOpenSettings={handleOpenSettings}
        selectedBookTitle="DDIA"
        theme="dark"
        onToggleTheme={vi.fn()}
      />
    );

    const configButton = screen.getByRole('button', { name: /CONFIGURAR TUTOR IA/i });
    fireEvent.click(configButton);

    expect(handleOpenSettings).toHaveBeenCalledTimes(1);
  });

  it('deve acionar refresh dos motores ao clicar no botão de atualização de infra', () => {
    const handleRefresh = vi.fn();
    render(
      <Navbar
        infraStatus={defaultStatus}
        loadingInfra={false}
        onRefreshInfra={handleRefresh}
        onOpenSettings={vi.fn()}
        selectedBookTitle="DDIA"
        theme="dark"
        onToggleTheme={vi.fn()}
      />
    );

    const refreshBtn = screen.getByTitle(/Sondar conectividade/i);
    fireEvent.click(refreshBtn);

    expect(handleRefresh).toHaveBeenCalledTimes(1);
  });

  it('deve exibir botão de retorno à estante quando isBookshelfActive for false e onNavigateBookshelf for fornecido', () => {
    const handleNavigate = vi.fn();
    render(
      <Navbar
        infraStatus={defaultStatus}
        loadingInfra={false}
        onRefreshInfra={vi.fn()}
        onOpenSettings={vi.fn()}
        selectedBookTitle="DDIA"
        theme="dark"
        onToggleTheme={vi.fn()}
        isBookshelfActive={false}
        onNavigateBookshelf={handleNavigate}
      />
    );

    const bookshelfBtn = screen.getByRole('button', { name: /ESTANTE DE LIVROS/i });
    expect(bookshelfBtn).toBeInTheDocument();

    fireEvent.click(bookshelfBtn);
    expect(handleNavigate).toHaveBeenCalledTimes(1);
  });
});
