/**
 * Formata o número contextual de uma seção/laboratório a partir do capítulo pai e do número do laboratório.
 * Exemplo: (3, 1) => "3.1", (1, 2) => "1.2", (undefined, 5) => "5"
 */
export function formatSectionNumber(
  chapterNumber?: number | null,
  labNumber?: number | null
): string {
  if (chapterNumber != null && labNumber != null) {
    return `${chapterNumber}.${labNumber}`;
  }
  if (labNumber != null) {
    return `${labNumber}`;
  }
  if (chapterNumber != null) {
    return `${chapterNumber}`;
  }
  return '';
}
