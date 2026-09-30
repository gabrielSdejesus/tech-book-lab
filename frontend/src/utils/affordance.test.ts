import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';

describe('Global Interactive Affordance & Accessibility CSS Rules', () => {
  it('deve possuir regras globais na @layer base atribuindo cursor pointer a elementos interativos', () => {
    const cssPath = path.resolve(process.cwd(), 'src/index.css');
    const css = fs.readFileSync(cssPath, 'utf-8');

    // Valida a presença de cursor: pointer para botões habilitados e role=button
    expect(css).toMatch(/button:not\(:disabled\)/);
    expect(css).toMatch(/\[role="button"\]:not\(\[aria-disabled="true"\]\)/);
    expect(css).toMatch(/cursor:\s*pointer;/);
  });

  it('deve possuir regras globais na @layer base atribuindo cursor not-allowed a elementos desabilitados', () => {
    const cssPath = path.resolve(process.cwd(), 'src/index.css');
    const css = fs.readFileSync(cssPath, 'utf-8');

    // Valida a presença de cursor: not-allowed para botões e inputs desabilitados
    expect(css).toMatch(/button:disabled/);
    expect(css).toMatch(/\[role="button"\]\[aria-disabled="true"\]/);
    expect(css).toMatch(/input:disabled/);
    expect(css).toMatch(/cursor:\s*not-allowed;/);
  });
});
