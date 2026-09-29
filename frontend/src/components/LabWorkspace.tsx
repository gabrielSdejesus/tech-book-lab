import React, { useState, useEffect } from 'react';
import type { Lab, Challenge, QueryResult, AiAssessmentResponse } from '../types';
import { executeQuery, resetLab, assessWithAi } from '../services/api';
import {
  Play,
  RotateCcw,
  Sparkles,
  Table as TableIcon,
  Code2,
  CheckCircle2,
  AlertTriangle,
  FileText,
  Clock,
  Loader2,
  BookOpen
} from 'lucide-react';

interface Props {
  lab: Lab;
  apiKey: string;
  provider: string;
  model: string;
}

export const LabWorkspace: React.FC<Props> = ({ lab, apiKey, provider, model }) => {
  const [selectedChallenge, setSelectedChallenge] = useState<Challenge>(lab.challenges[0]);
  const [queryCode, setQueryCode] = useState<string>('');
  const [userReflection, setUserReflection] = useState<string>('');

  const [executing, setExecuting] = useState(false);
  const [queryResult, setQueryResult] = useState<QueryResult | null>(null);

  const [assessing, setAssessing] = useState(false);
  const [aiResponse, setAiResponse] = useState<AiAssessmentResponse | null>(null);

  const [activeTab, setActiveTab] = useState<'result' | 'ai' | 'json'>('result');
  const [resetting, setResetting] = useState(false);
  const [feedbackToast, setFeedbackToast] = useState<string | null>(null);

  useEffect(() => {
    if (lab.challenges.length > 0) {
      const first = lab.challenges[0];
      setSelectedChallenge(first);
      setQueryCode(first.starterTemplate || '');
      setUserReflection('');
      setQueryResult(null);
      setAiResponse(null);
      setActiveTab('result');
    }
  }, [lab.id]);

  const handleSelectChallenge = (ch: Challenge) => {
    setSelectedChallenge(ch);
    setQueryCode(ch.starterTemplate || '');
    setUserReflection('');
    setQueryResult(null);
    setAiResponse(null);
    setActiveTab('result');
  };

  const handleExecute = async () => {
    if (!queryCode.trim()) return;
    setExecuting(true);
    setFeedbackToast(null);
    try {
      const res = await executeQuery(queryCode, lab.engineType, lab.id);
      setQueryResult(res);
      setActiveTab('result');
    } catch (err: any) {
      setQueryResult({
        success: false,
        message: 'Erro ao executar consulta',
        columns: [],
        rows: [],
        rowCount: 0,
        executionTimeMs: 0,
        errorMessage: err.message || 'Erro inesperado'
      });
      setActiveTab('result');
    } finally {
      setExecuting(false);
    }
  };

  const handleReset = async () => {
    if (!confirm('Deseja realmente restaurar o banco deste laboratório para o estado inicial?')) return;
    setResetting(true);
    try {
      const res = await resetLab(lab.id);
      if (res.success) {
        setFeedbackToast('Esquema restaurado para o estado original com sucesso!');
        setTimeout(() => setFeedbackToast(null), 3500);
      } else {
        alert('Erro ao restaurar banco: ' + res.errorMessage);
      }
    } catch (err: any) {
      alert('Falha ao restaurar banco: ' + err.message);
    } finally {
      setResetting(false);
    }
  };

  const handleAssessWithAi = async () => {
    // Validação local: impede falso positivo se o usuário não escreveu nada ou deixou apenas o template inalterado
    const cleanUser = queryCode
      .replace(/--.*$|\/\/.*$/gm, '')
      .replace(/\/\*[\s\S]*?\*\//g, '')
      .replace(/\s+/g, ' ')
      .trim();

    const cleanTemplate = (selectedChallenge.starterTemplate || '')
      .replace(/--.*$|\/\/.*$/gm, '')
      .replace(/\/\*[\s\S]*?\*\//g, '')
      .replace(/\s+/g, ' ')
      .trim();

    if (!cleanUser || cleanUser === cleanTemplate) {
      setActiveTab('ai');
      setAiResponse({
        status: 'NEEDS_REVISION',
        feedback: 'Nenhuma implementação submetida. O editor de código está vazio ou contém apenas o template de comentários inicial. Escreva os comandos do exercício e execute-os antes de solicitar a avaliação do Tutor.',
        tradeOffAnalysis: 'Para examinar os trade-offs descritos no livro de Martin Kleppmann, você deve executar a consulta e comparar o comportamento das estruturas.',
        efficiencyNotes: 'Nenhuma instrução executada.',
        alternativeApproaches: ['Leia os requisitos do exercício na coluna à esquerda.'],
        modelUsed: 'Validador Local de Submissão'
      });
      return;
    }

    setAssessing(true);
    setActiveTab('ai');
    try {
      const executionSummary = queryResult
        ? queryResult.success
          ? `Sucesso. Linhas afetadas/retornadas: ${queryResult.rowCount}, Tempo: ${queryResult.executionTimeMs}ms. Amostra: ${JSON.stringify(
              queryResult.rows.slice(0, 3)
            )}`
          : `Erro na execução do banco: ${queryResult.errorMessage}`
        : 'Consulta ainda não foi executada no banco';

      const response = await assessWithAi({
        labId: lab.id,
        challengeId: selectedChallenge.id,
        userQuery: queryCode,
        executionSummary,
        userReflection,
        apiKeyOverride: apiKey,
        providerOverride: provider,
        modelOverride: model
      });

      setAiResponse(response);
    } catch (err: any) {
      setAiResponse({
        status: 'NEEDS_REVISION',
        feedback: 'Falha ao contatar o Tutor de IA: ' + err.message + '. Verifique sua API Key ou conexão de rede.',
        tradeOffAnalysis: 'Análise de trade-offs não disponível devido a erro de comunicação.',
        efficiencyNotes: 'Erro de comunicação.',
        alternativeApproaches: ['Clique no botão "CONFIGURAR TUTOR IA" no cabeçalho para testar a chave.'],
        modelUsed: 'Erro de Conexão'
      });
    } finally {
      setAssessing(false);
    }
  };

  return (
    <div className="flex-1 flex flex-col md:flex-row h-[calc(100vh-4.5rem)] overflow-hidden bg-[#fbf9f4] dark:bg-[#141312] transition-colors">
      {/* Toast Alert */}
      {feedbackToast && (
        <div className="absolute top-22 right-8 z-50 bg-[#efebe1] dark:bg-[#252320] border-2 border-stone-800 dark:border-stone-600 text-stone-900 dark:text-stone-100 px-4 py-2 text-xs font-mono font-bold book-shadow flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-[#15803d] dark:text-[#4ade80]" />
          <span>{feedbackToast}</span>
        </div>
      )}

      {/* Left Panel: Laboratory Worksheet (Folha de Laboratório) */}
      <div className="w-full md:w-5/12 border-r-2 border-stone-800 dark:border-stone-700 flex flex-col h-full bg-[#fbf9f4] dark:bg-[#141312] overflow-y-auto">
        {/* Lab Header */}
        <div className="p-6 border-b-2 border-stone-800 dark:border-stone-700 bg-[#f7f4ec] dark:bg-[#1a1917]">
          <div className="flex items-center gap-2 mb-1.5">
            <span className="text-[10px] font-mono font-bold uppercase tracking-widest text-[#8f1d1d] dark:text-[#df4444]">
              LABORATÓRIO &sect;&nbsp;3.{lab.number}
            </span>
            <span className="text-stone-400 dark:text-stone-600">&bull;</span>
            <span className="text-xs font-mono text-stone-500 dark:text-stone-400 font-semibold">{lab.slug}</span>
          </div>

          <h2 className="text-xl font-serif font-black text-stone-900 dark:text-stone-100 leading-tight">
            {lab.title}
          </h2>
          <p className="text-xs font-serif text-stone-700 dark:text-stone-300 mt-2 leading-relaxed">
            {lab.summary}
          </p>

          <div className="flex flex-wrap gap-1.5 mt-3 pt-3 border-t border-stone-300 dark:border-stone-700">
            {lab.keyConcepts.map((concept, idx) => (
              <span
                key={idx}
                className="px-2 py-0.5 text-[10px] font-mono font-bold uppercase border border-stone-400 dark:border-stone-700 bg-[#eee8db] dark:bg-[#23211e] text-stone-800 dark:text-stone-300"
              >
                {concept}
              </span>
            ))}
          </div>
        </div>

        {/* Physical Exercise Tabs */}
        <div className="flex border-b-2 border-stone-800 dark:border-stone-700 bg-[#efebe1] dark:bg-[#1f1d1a] px-4 pt-2 gap-1">
          {lab.challenges.map((ch) => (
            <button
              key={ch.id}
              onClick={() => handleSelectChallenge(ch)}
              className={`px-4 py-2 text-xs font-mono font-bold uppercase border-2 transition-all ${
                selectedChallenge.id === ch.id
                  ? 'bg-[#fbf9f4] dark:bg-[#141312] border-stone-800 dark:border-stone-700 border-b-[#fbf9f4] dark:border-b-[#141312] -mb-[2px] text-stone-950 dark:text-stone-100 font-black'
                  : 'bg-[#e5dfd2] dark:bg-[#252320] border-transparent text-stone-600 dark:text-stone-400 hover:text-stone-900 dark:hover:text-stone-200 hover:bg-[#ded7c8] dark:hover:bg-[#2d2a26]'
              }`}
            >
              Exercício {ch.order}
            </button>
          ))}
        </div>

        {/* Exercise Body */}
        <div className="p-6 space-y-6 flex-1">
          <div>
            <h3 className="text-base font-serif font-bold text-stone-900 dark:text-stone-100">
              {selectedChallenge.title}
            </h3>
            <p className="text-xs font-serif text-stone-700 dark:text-stone-300 mt-1.5 leading-relaxed">
              {selectedChallenge.description}
            </p>
          </div>

          {/* Case Study Box (Cenário de Negócio) */}
          <div className="p-4 bg-[#f5f0e4] dark:bg-[#1e1c19] border-l-4 border-l-[#8f1d1d] dark:border-l-[#df4444] border border-stone-300 dark:border-stone-700 space-y-1.5 book-shadow-sm">
            <span className="text-[10px] font-mono font-bold uppercase tracking-wider text-[#8f1d1d] dark:text-[#df4444] flex items-center gap-1.5">
              <BookOpen className="w-3.5 h-3.5" />
              <span>Cenário Prático de Engenharia</span>
            </span>
            <p className="text-xs font-serif text-stone-800 dark:text-stone-200 leading-relaxed italic">
              "{selectedChallenge.scenario}"
            </p>
          </div>

          {/* Guidelines Checklist */}
          <div className="space-y-2">
            <h4 className="text-[11px] font-mono font-bold uppercase tracking-wider text-stone-700 dark:text-stone-300">
              Requisitos de Implementação
            </h4>
            <ul className="space-y-2">
              {selectedChallenge.guidelines.map((guide, idx) => (
                <li key={idx} className="flex items-start gap-2.5 text-xs font-serif text-stone-800 dark:text-stone-200">
                  <span className="font-mono text-[#8f1d1d] dark:text-[#df4444] font-black text-xs shrink-0">&rsaquo;</span>
                  <span className="leading-snug">{guide}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Marginalia / Conceptual Reflection */}
          <div className="space-y-2 pt-4 border-t-2 border-stone-300 dark:border-stone-700">
            <label className="text-[11px] font-mono font-bold uppercase tracking-wider text-[#8f1d1d] dark:text-[#df4444] flex items-center gap-1.5">
              <FileText className="w-3.5 h-3.5" />
              <span>Reflexão Teórica (Trade-offs de Martin Kleppmann)</span>
            </label>
            <p className="text-xs font-serif italic text-stone-700 dark:text-stone-300 bg-[#efebe1] dark:bg-[#1e1c19] p-3 border border-stone-300 dark:border-stone-700">
              "{selectedChallenge.reflectionPrompt}"
            </p>
            <textarea
              value={userReflection}
              onChange={(e) => setUserReflection(e.target.value)}
              placeholder="Digite aqui sua análise sobre os trade-offs de modelagem, localidade e leitura vs escrita..."
              rows={4}
              className="w-full bg-[#fdfcf9] dark:bg-[#181715] border-2 border-stone-700 dark:border-stone-600 p-3 text-xs font-mono text-stone-900 dark:text-stone-100 placeholder-stone-400 dark:placeholder-stone-500 focus:outline-none focus:border-stone-900 dark:focus:border-stone-300 leading-relaxed book-shadow-sm"
            />
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-3 pt-2">
            <button
              onClick={handleAssessWithAi}
              disabled={assessing}
              className="flex-1 flex items-center justify-center gap-2 py-2.5 px-4 bg-[#8f1d1d] hover:bg-[#771818] dark:bg-[#991b1b] dark:hover:bg-[#7f1d1d] border-2 border-stone-900 dark:border-stone-600 text-white text-xs font-mono font-bold uppercase tracking-wider book-shadow book-shadow-pressed transition-all disabled:opacity-50"
            >
              {assessing ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>Avaliando...</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4 text-amber-200" />
                  <span>Submeter ao Tutor IA</span>
                </>
              )}
            </button>

            <button
              onClick={handleReset}
              disabled={resetting}
              title="Restaurar tabelas e esquemas para o estado inicial limpo"
              className="flex items-center gap-1.5 py-2.5 px-3 bg-[#eee8db] dark:bg-[#252320] hover:bg-[#ded7c8] dark:hover:bg-[#302c28] border-2 border-stone-800 dark:border-stone-600 text-stone-900 dark:text-stone-200 text-xs font-mono font-bold uppercase book-shadow-sm book-shadow-pressed transition-all"
            >
              <RotateCcw className={`w-3.5 h-3.5 ${resetting ? 'animate-spin' : ''}`} />
              <span>Restaurar</span>
            </button>
          </div>
        </div>
      </div>

      {/* Right Panel: Technical Workbench (Bancada de Consultas e Resultados) */}
      <div className="w-full md:w-7/12 flex flex-col h-full bg-[#fbf9f4] dark:bg-[#141312]">
        {/* Editor Toolbar */}
        <div className="h-12 border-b-2 border-stone-800 dark:border-stone-700 bg-[#f7f4ec] dark:bg-[#1a1917] px-4 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <span
              className={`px-2 py-0.5 text-[10px] font-mono font-bold uppercase border-2 ${
                lab.engineType === 'NEO4J'
                  ? 'bg-[#efe3d5] dark:bg-[#2d2419] text-[#713f12] dark:text-[#fde047] border-stone-800 dark:border-stone-600'
                  : 'bg-[#e5ebe4] dark:bg-[#1a2e1d] text-[#14532d] dark:text-[#86efac] border-stone-800 dark:border-stone-600'
              }`}
            >
              {lab.engineType === 'NEO4J' ? 'MOTOR: NEO4J 5 (CYPHER)' : 'MOTOR: POSTGRESQL 16 (SQL)'}
            </span>
            <span className="text-[10px] font-mono text-stone-500 dark:text-stone-400 hidden sm:inline">
              [ Ctrl + Enter para rodar ]
            </span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setQueryCode(selectedChallenge.starterTemplate || '')}
              className="text-[11px] font-mono font-semibold text-stone-600 dark:text-stone-400 hover:text-stone-950 dark:hover:text-stone-100 px-2 py-1 border border-stone-400 dark:border-stone-700 bg-[#efebe1] dark:bg-[#1f1d1a] hover:bg-[#ded7c8] dark:hover:bg-[#2a2723] transition-colors"
            >
              Recarregar Template
            </button>

            <button
              onClick={handleExecute}
              disabled={executing}
              className="flex items-center gap-1.5 px-4 py-1.5 bg-[#1c1917] hover:bg-[#33302e] dark:bg-[#e6e2d8] dark:hover:bg-[#f3f0e8] dark:text-stone-950 border-2 border-stone-900 text-white text-xs font-mono font-bold uppercase book-shadow-sm book-shadow-pressed transition-all disabled:opacity-50"
            >
              {executing ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
              ) : (
                <Play className="w-3.5 h-3.5 fill-current" />
              )}
              <span>Executar</span>
            </button>
          </div>
        </div>

        {/* Code Workbench */}
        <div className="h-1/2 border-b-2 border-stone-800 dark:border-stone-700 relative flex flex-col bg-[#fdfcf9] dark:bg-[#161513]">
          <textarea
            value={queryCode}
            onChange={(e) => setQueryCode(e.target.value)}
            onKeyDown={(e) => {
              if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
                e.preventDefault();
                handleExecute();
              }
            }}
            placeholder="-- Digite aqui sua instrução SQL ou Cypher..."
            spellCheck={false}
            className="flex-1 w-full p-4 bg-transparent text-stone-950 dark:text-stone-100 font-mono text-xs leading-relaxed resize-none focus:outline-none selection:bg-[#8f1d1d] selection:text-white"
          />
        </div>

        {/* Output Panel Header & Tabs */}
        <div className="h-10 border-b-2 border-stone-800 dark:border-stone-700 bg-[#efebe1] dark:bg-[#1f1d1a] px-4 flex items-center justify-between">
          <div className="flex items-center gap-1">
            <button
              onClick={() => setActiveTab('result')}
              className={`flex items-center gap-1.5 px-3 py-1 text-xs font-mono font-bold uppercase border-t-2 border-r-2 border-l-2 transition-colors ${
                activeTab === 'result'
                  ? 'bg-[#fbf9f4] dark:bg-[#141312] border-stone-800 dark:border-stone-700 -mb-[2px] text-stone-950 dark:text-stone-100'
                  : 'bg-transparent border-transparent text-stone-600 dark:text-stone-400 hover:text-stone-900 dark:hover:text-stone-200'
              }`}
            >
              <TableIcon className="w-3.5 h-3.5" />
              <span>Resultados</span>
              {queryResult?.rowCount !== undefined && (
                <span className="text-[10px] bg-stone-200 dark:bg-stone-800 px-1 border border-stone-400 dark:border-stone-600 font-mono text-stone-900 dark:text-stone-200">
                  {queryResult.rowCount}
                </span>
              )}
            </button>

            <button
              onClick={() => setActiveTab('ai')}
              className={`flex items-center gap-1.5 px-3 py-1 text-xs font-mono font-bold uppercase border-t-2 border-r-2 border-l-2 transition-colors ${
                activeTab === 'ai'
                  ? 'bg-[#fbf9f4] dark:bg-[#141312] border-stone-800 dark:border-stone-700 -mb-[2px] text-[#8f1d1d] dark:text-[#df4444]'
                  : 'bg-transparent border-transparent text-stone-600 dark:text-stone-400 hover:text-stone-900 dark:hover:text-stone-200'
              }`}
            >
              <Sparkles className="w-3.5 h-3.5 text-[#8f1d1d] dark:text-[#df4444]" />
              <span>Parecer do Tutor IA</span>
              {aiResponse && (
                <span className="w-2 h-2 rounded-full bg-[#8f1d1d] dark:bg-[#df4444]" />
              )}
            </button>

            <button
              onClick={() => setActiveTab('json')}
              className={`flex items-center gap-1.5 px-3 py-1 text-xs font-mono font-bold uppercase border-t-2 border-r-2 border-l-2 transition-colors ${
                activeTab === 'json'
                  ? 'bg-[#fbf9f4] dark:bg-[#141312] border-stone-800 dark:border-stone-700 -mb-[2px] text-stone-950 dark:text-stone-100'
                  : 'bg-transparent border-transparent text-stone-600 dark:text-stone-400 hover:text-stone-900 dark:hover:text-stone-200'
              }`}
            >
              <Code2 className="w-3.5 h-3.5" />
              <span>JSON</span>
            </button>
          </div>

          {queryResult && (
            <div className="flex items-center gap-1.5 text-[11px] text-stone-600 dark:text-stone-400 font-mono">
              <Clock className="w-3 h-3 text-stone-500" />
              <span>{queryResult.executionTimeMs}ms</span>
            </div>
          )}
        </div>

        {/* Output Panel Content */}
        <div className="flex-1 overflow-auto p-4 bg-[#fbf9f4] dark:bg-[#141312] font-mono text-xs">
          {activeTab === 'result' && (
            <div>
              {!queryResult && (
                <div className="h-44 flex flex-col items-center justify-center text-stone-500 dark:text-stone-400 gap-2 font-serif italic text-sm">
                  <span>Execute uma consulta para inspecionar os dados retornados pelo motor.</span>
                </div>
              )}

              {queryResult && !queryResult.success && (
                <div className="p-4 bg-[#fee2e2] dark:bg-[#381616] border-2 border-[#b91c1c] text-[#7f1d1d] dark:text-[#fca5a5] space-y-2 book-shadow-sm">
                  <div className="flex items-center gap-2 font-bold font-mono text-xs uppercase tracking-wider">
                    <AlertTriangle className="w-4 h-4 text-[#b91c1c] dark:text-[#f87171]" />
                    <span>Erro de Execução no Banco</span>
                  </div>
                  <pre className="text-xs text-stone-900 dark:text-stone-100 whitespace-pre-wrap font-mono bg-white dark:bg-[#201212] p-3 border border-[#fca5a5] dark:border-[#7f1d1d]">
                    {queryResult.errorMessage}
                  </pre>
                </div>
              )}

              {queryResult && queryResult.success && queryResult.columns.length === 0 && (
                <div className="p-4 bg-[#eef5ee] dark:bg-[#152e18] border-2 border-[#15803d] text-[#14532d] dark:text-[#86efac] font-mono font-semibold book-shadow-sm">
                  {queryResult.message}
                </div>
              )}

              {queryResult && queryResult.success && queryResult.columns.length > 0 && (
                <div className="border-2 border-stone-800 dark:border-stone-700 overflow-x-auto book-shadow-sm">
                  <table className="w-full text-left border-collapse text-xs">
                    <thead>
                      <tr className="bg-[#ede7da] dark:bg-[#201e1b] border-b-2 border-stone-800 dark:border-stone-700 text-stone-900 dark:text-stone-100 font-mono">
                        {queryResult.columns.map((col, idx) => (
                          <th key={idx} className="p-2.5 font-bold border-r border-stone-400 dark:border-stone-700 last:border-r-0 uppercase">
                            {col}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-stone-300 dark:divide-stone-800 font-mono">
                      {queryResult.rows.map((row, rowIdx) => (
                        <tr
                          key={rowIdx}
                          className="hover:bg-[#f3eee2] dark:hover:bg-[#201e1a] transition-colors text-stone-900 dark:text-stone-100 odd:bg-[#fbf9f4] odd:dark:bg-[#141312] even:bg-[#f6f2e8] even:dark:bg-[#1a1917]"
                        >
                          {queryResult.columns.map((col, colIdx) => (
                            <td key={colIdx} className="p-2.5 border-r border-stone-300 dark:border-stone-800 last:border-r-0">
                              {typeof row[col] === 'object' && row[col] !== null
                                ? JSON.stringify(row[col])
                                : String(row[col] ?? 'null')}
                            </td>
                          ))}
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}

          {activeTab === 'ai' && (
            <div className="space-y-4 font-serif text-xs">
              {!aiResponse && !assessing && (
                <div className="h-44 flex flex-col items-center justify-center text-stone-500 dark:text-stone-400 gap-2 font-serif italic text-sm">
                  <Sparkles className="w-6 h-6 stroke-1 text-[#8f1d1d] dark:text-[#df4444]" />
                  <span>
                    Clique em <strong>"Submeter ao Tutor IA"</strong> para obter a crítica conceitual e a análise dos trade-offs da sua solução.
                  </span>
                </div>
              )}

              {assessing && (
                <div className="h-44 flex flex-col items-center justify-center text-[#8f1d1d] dark:text-[#df4444] gap-3 font-mono font-bold text-xs uppercase">
                  <Loader2 className="w-8 h-8 animate-spin" />
                  <span>O Tutor de IA está examinando sua modelagem e trade-offs...</span>
                </div>
              )}

              {aiResponse && !assessing && (
                <div className="space-y-4">
                  {/* Status Card (Parecer Técnico) */}
                  <div className="flex items-center justify-between p-3.5 bg-[#efebe1] dark:bg-[#1f1d1a] border-2 border-stone-800 dark:border-stone-700 book-shadow-sm">
                    <div className="flex items-center gap-2.5">
                      {aiResponse.status === 'APPROVED' ? (
                        <span className="flex items-center gap-1.5 px-3 py-1 text-xs font-mono font-bold uppercase bg-[#dcfce7] dark:bg-[#152e18] text-[#14532d] dark:text-[#86efac] border border-[#166534] dark:border-[#15803d]">
                          <CheckCircle2 className="w-4 h-4 text-[#15803d] dark:text-[#4ade80]" />
                          <span>PARECER: SOLUÇÃO APROVADA</span>
                        </span>
                      ) : (
                        <span className="flex items-center gap-1.5 px-3 py-1 text-xs font-mono font-bold uppercase bg-[#fee2e2] dark:bg-[#381616] text-[#991b1b] dark:text-[#fca5a5] border border-[#b91c1c] dark:border-[#7f1d1d]">
                          <AlertTriangle className="w-4 h-4 text-[#b91c1c] dark:text-[#f87171]" />
                          <span>PARECER: REVISÃO NECESSÁRIA</span>
                        </span>
                      )}
                    </div>
                    <span className="text-[11px] text-stone-600 dark:text-stone-400 font-mono">
                      Modelo: <strong className="text-stone-900 dark:text-stone-200">{aiResponse.modelUsed}</strong>
                    </span>
                  </div>

                  {/* Pedagogical Critique */}
                  <div className="p-4 bg-[#fdfcf9] dark:bg-[#181715] border-2 border-stone-800 dark:border-stone-700 space-y-2 book-shadow-sm">
                    <h4 className="font-mono font-bold text-xs uppercase tracking-wider text-[#8f1d1d] dark:text-[#df4444] flex items-center gap-1.5">
                      <Sparkles className="w-3.5 h-3.5" />
                      <span>Análise Crítica do Tutor</span>
                    </h4>
                    <p className="text-stone-900 dark:text-stone-200 leading-relaxed whitespace-pre-line text-xs font-serif">
                      {aiResponse.feedback}
                    </p>
                  </div>

                  {/* Trade-off Analysis */}
                  <div className="p-4 bg-[#f5f0e4] dark:bg-[#1e1c19] border-l-4 border-l-[#8f1d1d] dark:border-l-[#df4444] border border-stone-300 dark:border-stone-700 space-y-2 book-shadow-sm">
                    <h4 className="font-mono font-bold text-xs uppercase tracking-wider text-stone-800 dark:text-stone-200">
                      Trade-offs Teóricos (Martin Kleppmann - DDIA)
                    </h4>
                    <p className="text-stone-800 dark:text-stone-300 leading-relaxed text-xs font-serif italic">
                      {aiResponse.tradeOffAnalysis}
                    </p>
                  </div>

                  {/* Efficiency Notes */}
                  {aiResponse.efficiencyNotes && (
                    <div className="p-3.5 bg-[#fbf9f4] dark:bg-[#141312] border border-stone-400 dark:border-stone-700 space-y-1">
                      <h4 className="font-mono font-bold text-[11px] uppercase tracking-wider text-stone-600 dark:text-stone-400">
                        Observações de Desempenho & Custo Computacional
                      </h4>
                      <p className="text-stone-800 dark:text-stone-300 text-xs font-mono leading-relaxed">
                        {aiResponse.efficiencyNotes}
                      </p>
                    </div>
                  )}

                  {/* Alternative Approaches */}
                  {aiResponse.alternativeApproaches?.length > 0 && (
                    <div className="p-4 bg-[#efebe1] dark:bg-[#1e1c19] border border-stone-400 dark:border-stone-700 space-y-2">
                      <h4 className="font-mono font-bold text-xs uppercase tracking-wider text-stone-700 dark:text-stone-300">
                        Abordagens Alternativas Válidas
                      </h4>
                      <ul className="space-y-1.5 font-serif">
                        {aiResponse.alternativeApproaches.map((alt, idx) => (
                          <li key={idx} className="flex items-start gap-2 text-stone-800 dark:text-stone-200 text-xs">
                            <span className="font-mono text-[#8f1d1d] dark:text-[#df4444] font-bold">&bull;</span>
                            <span>{alt}</span>
                          </li>
                        ))}
                      </ul>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {activeTab === 'json' && (
            <div>
              <pre className="text-stone-900 dark:text-stone-200 font-mono text-xs whitespace-pre-wrap bg-[#fdfcf9] dark:bg-[#181715] p-4 border border-stone-300 dark:border-stone-700">
                {queryResult ? JSON.stringify(queryResult, null, 2) : '// Nenhum resultado disponível'}
              </pre>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
