import React, { useState, useEffect } from 'react';
import type { Lab, Challenge, QueryResult, AiAssessmentResponse, InfraStatus } from '../types';
import { executeQuery, resetLab, assessWithAi, getInfraStatus } from '../services/api';
import {
  provisionLab,
  getLabStatus,
  sendHeartbeat,
  teardownLab,
  type LabProvisionStatus
} from '../services/labProvisioning';
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
import { useLanguage } from '../i18n/LanguageContext';
import { formatSectionNumber } from '../utils/formatters';
import { getEngineConfig, getConnectionLabel } from '../config/engineConfig';

interface Props {
  lab: Lab;
  chapterNumber?: number;
  apiKey: string;
  provider: string;
  model: string;
}

export const LabWorkspace: React.FC<Props> = ({ lab, chapterNumber, apiKey, provider, model }) => {
  const { t, locale } = useLanguage();
  const [selectedChallenge, setSelectedChallenge] = useState<Challenge>(lab.challenges[0]);

  const [queryCode, setQueryCode] = useState<string>(lab.challenges[0]?.starterTemplate || '');
  const [userReflection, setUserReflection] = useState<string>('');

  const [executing, setExecuting] = useState(false);
  const [queryResult, setQueryResult] = useState<QueryResult | null>(null);

  const [assessing, setAssessing] = useState(false);
  const [aiResponse, setAiResponse] = useState<AiAssessmentResponse | null>(null);

  const [activeTab, setActiveTab] = useState<'result' | 'ai' | 'json'>('result');
  const [resetting, setResetting] = useState(false);
  const [feedbackToast, setFeedbackToast] = useState<string | null>(null);

  const [provisionStatus, setProvisionStatus] = useState<LabProvisionStatus>('READY');
  const [provisionMessage, setProvisionMessage] = useState<string>('');
  const [infraStatus, setInfraStatus] = useState<InfraStatus | null>(null);

  const activeEngine = selectedChallenge?.engineType || lab.engineType;
  const activeEngineConfig = getEngineConfig(activeEngine);
  const activePort =
    infraStatus?.[activeEngine.toLowerCase()]?.port ?? (activeEngineConfig.port > 0 ? activeEngineConfig.port : null);

  useEffect(() => {
    let isMounted = true;
    getInfraStatus()
      .then((status) => {
        if (isMounted) {
          setInfraStatus(status);
        }
      })
      .catch(() => {
        // dynamic infra status optional fallback
      });
    return () => {
      isMounted = false;
    };
  }, []);

  const [prevLabId, setPrevLabId] = useState(lab.id);
  if (prevLabId !== lab.id) {
    setPrevLabId(lab.id);
    if (lab.challenges.length > 0) {
      const first = lab.challenges[0];
      setSelectedChallenge(first);
      setQueryCode(first.starterTemplate || '');
      setUserReflection('');
      setQueryResult(null);
      setAiResponse(null);
      setActiveTab('result');
    }
  }

  useEffect(() => {
    return () => {
      teardownLab(lab.id).catch(() => {});
    };
  }, [lab.id]);

  useEffect(() => {
    let isMounted = true;
    let pollTimer: any = null;
    let heartbeatTimer: any = null;

    const start = async () => {
      try {
        setProvisionStatus('PROVISIONING');
        setProvisionMessage('Provisionando ambiente de laboratório para a tarefa...');
        const init = await provisionLab(lab.id, selectedChallenge?.id);
        if (!isMounted) return;
        setProvisionStatus(init.status);
        setProvisionMessage(init.message || '');

        const heartbeatIntervalMs = (init.heartbeatIntervalSeconds || 30) * 1000;

        if (init.status === 'READY') {
          heartbeatTimer = setInterval(() => {
            sendHeartbeat(lab.id).catch(() => {});
          }, heartbeatIntervalMs);
        } else if (init.status === 'PROVISIONING') {
          pollTimer = setInterval(async () => {
            try {
              const statusRes = await getLabStatus(lab.id, selectedChallenge?.id);
              if (!isMounted) return;
              if (statusRes.status === 'READY') {
                clearInterval(pollTimer);
                setProvisionStatus('READY');
                heartbeatTimer = setInterval(() => {
                  sendHeartbeat(lab.id).catch(() => {});
                }, heartbeatIntervalMs);
              } else if (statusRes.status === 'ERROR') {
                clearInterval(pollTimer);
                setProvisionStatus('ERROR');
                setProvisionMessage(statusRes.errorMessage || 'Falha ao inicializar o ambiente.');
              }
            } catch {
              // fallback polling
            }
          }, 2000);
        }
      } catch {
        if (isMounted) setProvisionStatus('READY');
      }
    };

    start();

    return () => {
      isMounted = false;
      if (pollTimer) clearInterval(pollTimer);
      if (heartbeatTimer) clearInterval(heartbeatTimer);
    };
  }, [lab.id, selectedChallenge?.id]);

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
      const res = await executeQuery(queryCode, activeEngine, lab.id);
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
    if (!confirm(t.lab.confirmReset)) return;
    setResetting(true);
    try {
      const res = await resetLab(lab.id);
      if (res.success) {
        setFeedbackToast(
          locale === 'pt'
            ? 'Esquema restaurado para o estado original com sucesso!'
            : 'Schema restored to original state successfully!'
        );
        setTimeout(() => setFeedbackToast(null), 3500);
      } else {
        alert((locale === 'pt' ? 'Erro ao restaurar banco: ' : 'Error resetting database: ') + res.errorMessage);
      }
    } catch (err: any) {
      alert((locale === 'pt' ? 'Falha ao restaurar banco: ' : 'Failed to reset database: ') + err.message);
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
        feedback:
          locale === 'pt'
            ? 'Nenhuma implementação submetida. O editor de código está vazio ou contém apenas o template de comentários inicial. Escreva os comandos do exercício e execute-os antes de solicitar a avaliação do Tutor.'
            : 'No implementation submitted. The code editor is empty or contains only the initial comment template. Write the exercise commands and run them before requesting the Tutor evaluation.',
        tradeOffAnalysis:
          locale === 'pt'
            ? 'Para examinar os trade-offs descritos no livro de Martin Kleppmann, você deve executar a consulta e comparar o comportamento das estruturas.'
            : 'To examine the trade-offs described in Martin Kleppmann\'s book, you should execute the query and compare the behavior of the structures.',
        efficiencyNotes: locale === 'pt' ? 'Nenhuma instrução executada.' : 'No instructions executed.',
        alternativeApproaches: [
          locale === 'pt'
            ? 'Leia os requisitos do exercício na coluna à esquerda.'
            : 'Read the exercise requirements in the column on the left.'
        ],
        modelUsed: locale === 'pt' ? 'Validador Local de Submissão' : 'Local Submission Validator'
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
        modelOverride: model,
        language: locale
      });

      setAiResponse(response);
    } catch (err: any) {
      setAiResponse({
        status: 'NEEDS_REVISION',
        feedback:
          locale === 'pt'
            ? 'Falha ao contatar o Tutor de IA: ' + err.message + '. Verifique sua API Key ou conexão de rede.'
            : 'Failed to contact AI Tutor: ' + err.message + '. Check your API Key or network connection.',
        tradeOffAnalysis:
          locale === 'pt'
            ? 'Análise de trade-offs não disponível devido a erro de comunicação.'
            : 'Trade-off analysis not available due to communication error.',
        efficiencyNotes: locale === 'pt' ? 'Erro de comunicação.' : 'Communication error.',
        alternativeApproaches: [
          locale === 'pt'
            ? 'Clique no botão "CONFIGURAR TUTOR IA" no cabeçalho para testar a chave.'
            : 'Click the "CONFIGURE AI TUTOR" button in the header to test your key.'
        ],
        modelUsed: locale === 'pt' ? 'Erro de Conexão' : 'Connection Error'
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
              {t.lab.laboratory} &sect;&nbsp;{formatSectionNumber(chapterNumber, lab.number)}
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
              {locale === 'pt' ? 'Exercício' : 'Exercise'} {ch.order}
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
              <span>{t.lab.engineeringScenario}</span>
            </span>
            <p className="text-xs font-serif text-stone-800 dark:text-stone-200 leading-relaxed italic">
              "{selectedChallenge.scenario}"
            </p>
          </div>

          {/* Guidelines Checklist */}
          <div className="space-y-2">
            <h4 className="text-[11px] font-mono font-bold uppercase tracking-wider text-stone-700 dark:text-stone-300">
              {t.lab.requirementsAndConstraints}
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
              <span>{t.lab.reflectiveQuestion}</span>
            </label>
            <p className="text-xs font-serif italic text-stone-700 dark:text-stone-300 bg-[#efebe1] dark:bg-[#1e1c19] p-3 border border-stone-300 dark:border-stone-700">
              "{selectedChallenge.reflectionPrompt}"
            </p>
            <textarea
              value={userReflection}
              onChange={(e) => setUserReflection(e.target.value)}
              placeholder={t.lab.reflectionPlaceholder}
              rows={4}
              className="w-full bg-[#fdfcf9] dark:bg-[#181715] border-2 border-stone-700 dark:border-stone-600 p-3 text-xs font-mono text-stone-900 dark:text-stone-100 placeholder-stone-400 dark:placeholder-stone-500 focus:outline-none focus:border-stone-900 dark:focus:border-stone-300 leading-relaxed book-shadow-sm"
            />
          </div>

          {/* Action Buttons */}
          <div className="flex items-center gap-3 pt-2">
            <button
              onClick={handleAssessWithAi}
              disabled={assessing || provisionStatus !== 'READY'}
              className="flex-1 flex items-center justify-center gap-2 py-2.5 px-4 bg-[#8f1d1d] hover:bg-[#771818] dark:bg-[#991b1b] dark:hover:bg-[#7f1d1d] border-2 border-stone-900 dark:border-stone-600 text-white text-xs font-mono font-bold uppercase tracking-wider book-shadow book-shadow-pressed transition-all cursor-pointer disabled:cursor-not-allowed disabled:opacity-50"
            >
              {assessing ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  <span>{t.lab.evaluating}</span>
                </>
              ) : (
                <>
                  <Sparkles className="w-4 h-4 text-amber-200" />
                  <span>{t.lab.evaluateWithAi}</span>
                </>
              )}
            </button>


            <button
              onClick={handleReset}
              disabled={resetting || provisionStatus !== 'READY'}
              title={t.lab.resetTooltip}
              className="flex items-center gap-1.5 py-2.5 px-3 bg-[#eee8db] dark:bg-[#252320] hover:bg-[#ded7c8] dark:hover:bg-[#302c28] border-2 border-stone-800 dark:border-stone-600 text-stone-900 dark:text-stone-200 text-xs font-mono font-bold uppercase book-shadow-sm book-shadow-pressed transition-all cursor-pointer disabled:cursor-not-allowed disabled:opacity-50"
            >
              <RotateCcw className={`w-3.5 h-3.5 ${resetting ? 'animate-spin' : ''}`} />
              <span>{t.lab.reset}</span>
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
              className={`px-2 py-0.5 text-[10px] font-mono font-bold uppercase border-2 ${activeEngineConfig.toolbarClass}`}
            >
              {activeEngine === 'NEO4J' ? t.lab.engineNeo4j : activeEngine === 'POSTGRES' ? t.lab.enginePostgres : activeEngineConfig.name}
            </span>
            <span className="text-[10px] font-mono text-stone-500 dark:text-stone-400 hidden sm:inline">
              {t.lab.ctrlEnterHint}
            </span>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setQueryCode(selectedChallenge.starterTemplate || '')}
              className="text-[11px] font-mono font-semibold text-stone-600 dark:text-stone-400 hover:text-stone-950 dark:hover:text-stone-100 px-2 py-1 border border-stone-400 dark:border-stone-700 bg-[#efebe1] dark:bg-[#1f1d1a] hover:bg-[#ded7c8] dark:hover:bg-[#2a2723] transition-colors cursor-pointer"
            >
              {t.lab.reloadTemplate}
            </button>

            <button
              onClick={handleExecute}
              disabled={executing || provisionStatus !== 'READY' || !queryCode.trim()}
              title={
                provisionStatus === 'PROVISIONING'
                  ? (locale === 'pt' ? 'Aguarde o provisionamento do banco de dados...' : 'Waiting for database provisioning...')
                  : provisionStatus === 'ERROR'
                  ? (locale === 'pt' ? 'Banco de dados não disponível' : 'Database unavailable')
                  : t.lab.execute
              }
              className="flex items-center gap-1.5 px-4 py-1.5 bg-[#1c1917] hover:bg-[#33302e] dark:bg-[#e6e2d8] dark:hover:bg-[#f3f0e8] dark:text-stone-950 border-2 border-stone-900 text-white text-xs font-mono font-bold uppercase book-shadow-sm book-shadow-pressed transition-all cursor-pointer disabled:cursor-not-allowed disabled:opacity-50"
            >
              {executing ? (
                <Loader2 className="w-3.5 h-3.5 animate-spin" />
              ) : (
                <Play className="w-3.5 h-3.5 fill-current" />
              )}
              <span>{t.lab.execute}</span>
            </button>
          </div>
        </div>

        {provisionStatus === 'PROVISIONING' && (
          <div className="bg-amber-100 dark:bg-amber-950 border-b-2 border-amber-800 dark:border-amber-600 px-4 py-2 flex items-center gap-2 text-xs font-mono text-amber-900 dark:text-amber-200">
            <Loader2 className="w-4 h-4 animate-spin text-amber-700 dark:text-amber-400" />
            <span>
              {provisionMessage || (locale === 'pt'
                ? `Provisionando ambiente isolado de laboratório (${activeEngineConfig.name})... Aguarde para executar consultas.`
                : `Provisioning isolated lab environment (${activeEngineConfig.name})... Please wait before running queries.`)}
            </span>
          </div>
        )}

        {provisionStatus === 'ERROR' && (
          <div className="bg-red-100 dark:bg-red-950 border-b-2 border-red-800 dark:border-red-600 px-4 py-2 flex items-center justify-between text-xs font-mono text-red-900 dark:text-red-200">
            <div className="flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-red-600 dark:text-red-400" />
              <span>{provisionMessage || 'Falha ao inicializar contêiner do laboratório.'}</span>
            </div>
            <button
              onClick={() => {
                setProvisionStatus('PROVISIONING');
                provisionLab(lab.id, selectedChallenge?.id).then(r => setProvisionStatus(r.status)).catch(() => setProvisionStatus('ERROR'));
              }}
              className="px-2 py-1 bg-red-800 hover:bg-red-700 text-white text-[11px] font-bold uppercase transition-colors"
            >
              Tentar Novamente
            </button>
          </div>
        )}

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
            placeholder={t.lab.queryPlaceholder}
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
              <span>{t.lab.tabs.results}</span>
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
              <span>{t.lab.tabs.aiTutor}</span>
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
              <span>{t.lab.tabs.json}</span>
            </button>
          </div>

          <div className="flex items-center gap-3">
            {/* Contextual Active Engine Badge */}
            <div
              data-testid="active-engine-badge"
              className="flex items-center gap-1.5 px-2 py-0.5 border border-stone-400 dark:border-stone-700 bg-[#f7f4ec] dark:bg-[#1a1917] text-[10px] font-mono"
            >
              <span
                className={`w-2 h-2 rounded-full ${
                  provisionStatus === 'READY'
                    ? 'bg-emerald-500 animate-pulse'
                    : provisionStatus === 'PROVISIONING'
                    ? 'bg-amber-500 animate-spin'
                    : 'bg-red-500'
                }`}
              />
              <span className="font-bold text-stone-700 dark:text-stone-300">
                {getConnectionLabel(activeEngineConfig, activePort)}
              </span>
              <span className="text-stone-500 dark:text-stone-400 uppercase text-[9px]">
                {provisionStatus === 'READY'
                  ? 'ON'
                  : provisionStatus === 'PROVISIONING'
                  ? 'BOOTING'
                  : 'OFF'}
              </span>
            </div>

            {queryResult && (
              <div className="flex items-center gap-1.5 text-[11px] text-stone-600 dark:text-stone-400 font-mono">
                <Clock className="w-3.5 h-3.5 text-stone-500" />
                <span>{queryResult.executionTimeMs}ms</span>
              </div>
            )}
          </div>
        </div>

        {/* Output Panel Content */}
        <div className="flex-1 overflow-auto p-4 bg-[#fbf9f4] dark:bg-[#141312] font-mono text-xs">
          {activeTab === 'result' && (
            <div>
              {!queryResult && (
                <div className="h-44 flex flex-col items-center justify-center text-stone-500 dark:text-stone-400 gap-2 font-serif italic text-sm">
                  <span>{t.lab.emptyResultsPrompt}</span>
                </div>
              )}

              {queryResult && !queryResult.success && (
                <div className="p-4 bg-[#fee2e2] dark:bg-[#381616] border-2 border-[#b91c1c] text-[#7f1d1d] dark:text-[#fca5a5] space-y-2 book-shadow-sm">
                  <div className="flex items-center gap-2 font-bold font-mono text-xs uppercase tracking-wider">
                    <AlertTriangle className="w-4 h-4 text-[#b91c1c] dark:text-[#f87171]" />
                    <span>{t.lab.executionErrorTitle}</span>
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
                    {t.lab.aiPromptInstruction}
                  </span>
                </div>
              )}

              {assessing && (
                <div className="h-44 flex flex-col items-center justify-center text-[#8f1d1d] dark:text-[#df4444] gap-3 font-mono font-bold text-xs uppercase">
                  <Loader2 className="w-8 h-8 animate-spin" />
                  <span>{t.lab.aiExamining}</span>
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
                          <span>{t.lab.solutionApproved}</span>
                        </span>
                      ) : (
                        <span className="flex items-center gap-1.5 px-3 py-1 text-xs font-mono font-bold uppercase bg-[#fee2e2] dark:bg-[#381616] text-[#991b1b] dark:text-[#fca5a5] border border-[#b91c1c] dark:border-[#7f1d1d]">
                          <AlertTriangle className="w-4 h-4 text-[#b91c1c] dark:text-[#f87171]" />
                          <span>{t.lab.revisionNeeded}</span>
                        </span>
                      )}
                    </div>
                    <span className="text-[11px] text-stone-600 dark:text-stone-400 font-mono">
                      {t.lab.modelLabel} <strong className="text-stone-900 dark:text-stone-200">{aiResponse.modelUsed}</strong>
                    </span>
                  </div>

                  {/* Pedagogical Critique */}
                  <div className="p-4 bg-[#fdfcf9] dark:bg-[#181715] border-2 border-stone-800 dark:border-stone-700 space-y-2 book-shadow-sm">
                    <h4 className="font-mono font-bold text-xs uppercase tracking-wider text-[#8f1d1d] dark:text-[#df4444] flex items-center gap-1.5">
                      <Sparkles className="w-3.5 h-3.5" />
                      <span>{t.lab.criticalAnalysis}</span>
                    </h4>
                    <p className="text-stone-900 dark:text-stone-200 leading-relaxed whitespace-pre-line text-xs font-serif">
                      {aiResponse.feedback}
                    </p>
                  </div>

                  {/* Trade-off Analysis */}
                  <div className="p-4 bg-[#f5f0e4] dark:bg-[#1e1c19] border-l-4 border-l-[#8f1d1d] dark:border-l-[#df4444] border border-stone-300 dark:border-stone-700 space-y-2 book-shadow-sm">
                    <h4 className="font-mono font-bold text-xs uppercase tracking-wider text-stone-800 dark:text-stone-200">
                      {t.lab.theoreticalTradeOffs}
                    </h4>
                    <p className="text-stone-800 dark:text-stone-300 leading-relaxed text-xs font-serif italic">
                      {aiResponse.tradeOffAnalysis}
                    </p>
                  </div>

                  {/* Efficiency Notes */}
                  {aiResponse.efficiencyNotes && (
                    <div className="p-3.5 bg-[#fbf9f4] dark:bg-[#141312] border border-stone-400 dark:border-stone-700 space-y-1">
                      <h4 className="font-mono font-bold text-[11px] uppercase tracking-wider text-stone-600 dark:text-stone-400">
                        {t.lab.performanceNotes}
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
                        {t.lab.alternativeApproaches}
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
                {queryResult ? JSON.stringify(queryResult, null, 2) : t.lab.noResult}
              </pre>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
