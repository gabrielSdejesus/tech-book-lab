export type Locale = 'pt' | 'en';

export interface TranslationSchema {
  common: {
    bookshelf: string;
    goToBookshelf: string;
    returnToBookshelf: string;
    toggleTheme: string;
    configureAi: string;
    connected: string;
    offline: string;
    cancel: string;
    save: string;
    close: string;
    loading: string;
    error: string;
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
    notebookStructure: string;
    chapter: string;
    practicalLabs: string;
    keyConcepts: string;
  };
  lab: {
    laboratory: string;
    labWorksheet: string;
    engineeringScenario: string;
    practicalChallenge: string;
    requirementsAndConstraints: string;
    reflectiveQuestion: string;
    conceptualReflection: string;
    reflectionPlaceholder: string;
    queryEditor: string;
    executeQuery: string;
    resetDatabase: string;
    evaluateWithAi: string;
    evaluating: string;
    executing: string;
    resetting: string;
    queryResult: string;
    rowsInMs: (count: number, ms: number) => string;
    noResult: string;
    aiEvaluation: string;
    approved: string;
    needsRevision: string;
    discussion: string;
    tabs: {
      diagnosis: string;
      tradeOffs: string;
      performance: string;
      alternatives: string;
    };
    confirmReset: string;
    executionError: string;
  };
  aiModal: {
    title: string;
    provider: string;
    apiKey: string;
    model: string;
    testConnection: string;
    saveSettings: string;
    testing: string;
    geminiDescription: string;
    ollamaDescription: string;
    modelPlaceholder: string;
  };
}
