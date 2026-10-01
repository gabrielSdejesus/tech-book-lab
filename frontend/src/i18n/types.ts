export type Locale = 'pt' | 'en';

export interface TranslationSchema {
  common: {
    bookshelf: string;
    goToBookshelf: string;
    returnToBookshelf: string;
    toggleTheme: string;
    configureAi: string;
  };
  bookshelf: {
    catalogTag: string;
    version: string;
    title: string;
    subtitle: string;
    booksAvailable: (count: number) => string;
    chaptersCount: (count: number) => string;
    labsActive: (count: number) => string;
    classicCollection: string;
    clickToEnter: string;
    notebookStructure: (chapters: number, labs: number) => string;
    openNotebook: string;
    emptyCatalog: string;
  };
  sidebar: {
    tableOfContents: string;
    collapseSidebar: string;
    expandSidebar: string;
    toggleSidebar: string;
  };
  lab: {
    laboratory: string;
    engineeringScenario: string;
    requirementsAndConstraints: string;
    reflectiveQuestion: string;
    reflectionPlaceholder: string;
    evaluateWithAi: string;
    evaluating: string;
    noResult: string;
    tabs: {
      results: string;
      aiTutor: string;
      json: string;
    };
    confirmReset: string;
    engineNeo4j: string;
    enginePostgres: string;
    ctrlEnterHint: string;
    reloadTemplate: string;
    execute: string;
    reset: string;
    resetTooltip: string;
    queryPlaceholder: string;
    emptyResultsPrompt: string;
    executionErrorTitle: string;
    aiPromptInstruction: string;
    aiExamining: string;
    solutionApproved: string;
    revisionNeeded: string;
    modelLabel: string;
    criticalAnalysis: string;
    theoreticalTradeOffs: string;
    performanceNotes: string;
    alternativeApproaches: string;
  };
  aiModal: {
    title: string;
    saveSettings: string;
    testing: string;
    description: string;
    providerLabel: string;
    modelVersion: string;
    recommendedLatest: string;
    getFreeKey: string;
    apiKeyPlaceholder: string;
    test: string;
    ollamaModel: string;
    ollamaNotice: string;
    testOllama: string;
    keyValidatedSuccess: string;
    validationFailed: string;
    activeVersion: (model: string, latencyMs: number) => string;
    contactError: string;
    cancel: string;
    saved: string;
  };
}
