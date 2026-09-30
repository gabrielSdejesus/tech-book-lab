import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Navbar } from './Navbar';
import type { InfraStatus } from '../types';
import { LanguageProvider } from '../i18n/LanguageContext';

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

  it('não deve exibir indicador global de motores na barra de navegação superior', () => {
    render(
      <Navbar
        onOpenSettings={vi.fn()}
        selectedBookTitle="DDIA"
        theme="dark"
        onToggleTheme={vi.fn()}
      />
    );

    expect(screen.queryByText(/MOTORES:/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/PG:5432/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/NEO4J:7687/i)).not.toBeInTheDocument();
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

  it('deve renderizar o seletor de idiomas PT e EN', () => {
    render(
      <Navbar
        infraStatus={defaultStatus}
        loadingInfra={false}
        onRefreshInfra={vi.fn()}
        onOpenSettings={vi.fn()}
        selectedBookTitle="DDIA"
        theme="dark"
        onToggleTheme={vi.fn()}
      />
    );

    const ptButton = screen.getByRole('button', { name: /^PT$/i });
    const enButton = screen.getByRole('button', { name: /^EN$/i });

    expect(ptButton).toBeInTheDocument();
    expect(enButton).toBeInTheDocument();
  });

  it('deve disparar onToggleSidebar ao clicar no ícone do livro no cabeçalho em modo workspace', () => {
    const handleToggleSidebar = vi.fn();
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
        isSidebarOpen={true}
        onToggleSidebar={handleToggleSidebar}
      />
    );

    const toggleSidebarBtn = screen.getByRole('button', { name: /Alternar tábua de matérias/i });
    expect(toggleSidebarBtn).toHaveAttribute('aria-expanded', 'true');
    expect(toggleSidebarBtn).toHaveAttribute('title', 'Recolher tábua de matérias');

    fireEvent.click(toggleSidebarBtn);
    expect(handleToggleSidebar).toHaveBeenCalledTimes(1);
  });

  it('deve exibir título de expandir quando a barra lateral estiver recolhida', () => {
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
        isSidebarOpen={false}
        onToggleSidebar={vi.fn()}
      />
    );

    const toggleSidebarBtn = screen.getByRole('button', { name: /Alternar tábua de matérias/i });
    expect(toggleSidebarBtn).toHaveAttribute('aria-expanded', 'false');
    expect(toggleSidebarBtn).toHaveAttribute('title', 'Expandir tábua de matérias');
  });

  it('deve exibir títulos em inglês para alternar barra lateral quando o idioma for en', () => {
    localStorage.setItem('tbl_locale', 'en');
    render(
      <LanguageProvider>
        <Navbar
          infraStatus={defaultStatus}
          loadingInfra={false}
          onRefreshInfra={vi.fn()}
          onOpenSettings={vi.fn()}
          selectedBookTitle="DDIA"
          theme="dark"
          onToggleTheme={vi.fn()}
          isBookshelfActive={false}
          isSidebarOpen={true}
          onToggleSidebar={vi.fn()}
        />
      </LanguageProvider>
    );

    const toggleSidebarBtn = screen.getByRole('button', { name: /Toggle table of contents/i });
    expect(toggleSidebarBtn).toHaveAttribute('title', 'Collapse table of contents');
  });

  it('deve permitir navegar para estante via teclado com Enter e Space no container da marca', () => {
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

    const brandBtn = screen.getByRole('button', { name: /Ir para a Estante/i });
    expect(brandBtn).toHaveAttribute('tabIndex', '0');

    fireEvent.keyDown(brandBtn, { key: 'Enter' });
    expect(handleNavigate).toHaveBeenCalledTimes(1);

    fireEvent.keyDown(brandBtn, { key: ' ' });
    expect(handleNavigate).toHaveBeenCalledTimes(2);
  });

  it('deve exibir disabled e disabled:cursor-not-allowed no botão de atualização de infraestrutura quando estiver carregando', () => {
    render(
      <Navbar
        infraStatus={defaultStatus}
        loadingInfra={true}
        onRefreshInfra={vi.fn()}
        onOpenSettings={vi.fn()}
        selectedBookTitle="DDIA"
        theme="dark"
        onToggleTheme={vi.fn()}
      />
    );

    const refreshBtn = screen.getByTitle(/Sondar conectividade/i);
    expect(refreshBtn).toBeDisabled();
    expect(refreshBtn).toHaveClass('disabled:cursor-not-allowed');
  });
});

