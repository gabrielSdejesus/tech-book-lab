import { describe, it, expect } from 'vitest';
import {
  formatLatency,
  formatThroughput,
  formatContention,
  getSlaBadgeStatus,
} from './stressMetricsFormatter';

describe('stressMetricsFormatter (TDD Seam)', () => {
  describe('formatLatency', () => {
    it('deve formatar latências menores que 1ms como "< 1 ms"', () => {
      expect(formatLatency(0.4)).toBe('< 1 ms');
      expect(formatLatency(0.8)).toBe('< 1 ms');
    });

    it('deve formatar latências regulares com 1 casa decimal e unidade ms', () => {
      expect(formatLatency(4.5)).toBe('4.5 ms');
      expect(formatLatency(18.23)).toBe('18.2 ms');
      expect(formatLatency(150.0)).toBe('150.0 ms');
    });

    it('deve formatar latências iguais ou superiores a 1000ms em segundos', () => {
      expect(formatLatency(1250)).toBe('1.25 s');
      expect(formatLatency(3000)).toBe('3.00 s');
    });
  });

  describe('formatThroughput', () => {
    it('deve formatar operações por segundo em escala k e M', () => {
      expect(formatThroughput(450)).toBe('450 ops/s');
      expect(formatThroughput(12500)).toBe('12.5k ops/s');
      expect(formatThroughput(200000)).toBe('200.0k ops/s');
      expect(formatThroughput(2500000)).toBe('2.5M ops/s');
    });
  });

  describe('formatContention', () => {
    it('deve formatar índice 0..1 em percentual com 1 casa decimal', () => {
      expect(formatContention(0)).toBe('0.0%');
      expect(formatContention(0.05)).toBe('5.0%');
      expect(formatContention(0.428)).toBe('42.8%');
      expect(formatContention(1.0)).toBe('100.0%');
    });

    it('deve limitar valores fora do intervalo 0..1', () => {
      expect(formatContention(-0.2)).toBe('0.0%');
      expect(formatContention(1.5)).toBe('100.0%');
    });
  });

  describe('getSlaBadgeStatus', () => {
    it('deve retornar status verde quando o SLA for atendido em PT e EN', () => {
      const ptStatus = getSlaBadgeStatus(true, 'pt');
      expect(ptStatus.label).toBe('SLA Atendido');
      expect(ptStatus.isMet).toBe(true);
      expect(ptStatus.colorClass).toContain('emerald');

      const enStatus = getSlaBadgeStatus(true, 'en');
      expect(enStatus.label).toBe('SLA Met');
      expect(enStatus.isMet).toBe(true);
    });

    it('deve retornar status vermelho quando o SLA for violado em PT e EN', () => {
      const ptStatus = getSlaBadgeStatus(false, 'pt');
      expect(ptStatus.label).toBe('SLA Violado');
      expect(ptStatus.isMet).toBe(false);
      expect(ptStatus.colorClass).toContain('rose');

      const enStatus = getSlaBadgeStatus(false, 'en');
      expect(enStatus.label).toBe('SLA Violated');
      expect(enStatus.isMet).toBe(false);
    });
  });
});
