import type { Block, Inline, StudyQuestion } from '../types';

export function normalize(text: string): string {
	return text
		.toLowerCase()
		.replace(/ł/g, 'l')
		.normalize('NFD')
		.replace(/\p{Diacritic}/gu, '');
}

function inlineText(content: Inline[]): string {
	return content.map((c) => (typeof c === 'string' ? c : c.b)).join('');
}

function blocksText(blocks: Block[]): string {
	return blocks
		.map((b) => (b.k === 'table' ? b.rows.flat().join(' ') : inlineText(b.c)))
		.join(' ');
}

export function filterQuestions(
	questions: StudyQuestion[],
	query: string,
): StudyQuestion[] {
	const trimmed = query.trim();
	if (!trimmed) return questions;

	if (/^\d+$/.test(trimmed)) {
		const byNumber = questions.filter((q) => q.n === Number(trimmed));
		if (byNumber.length) return byNumber;
	}

	const needle = normalize(trimmed);
	return questions.filter((q) =>
		normalize(`${q.title} ${blocksText(q.short)} ${blocksText(q.long)}`).includes(
			needle,
		),
	);
}
