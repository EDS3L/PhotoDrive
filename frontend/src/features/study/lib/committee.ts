import type { StudyQuestion } from '../types';

export interface CommitteeMember {
	key: string;
	name: string;
	topic: string;
	questions: number[];
}

export const COMMITTEE: CommitteeMember[] = [
	{
		key: 'duraj',
		name: 'Duraj',
		topic: 'Sztuczna inteligencja i UML',
		questions: [35, 36, 37, 38, 39, 64, 46, 47, 48],
	},
	{
		key: 'kasprowicz',
		name: 'Kasprowicz',
		topic: 'Bazy danych',
		questions: [40, 41, 42, 43, 44, 45],
	},
	{
		key: 'rychlik',
		name: 'Rychlik',
		topic: 'Programowanie',
		questions: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 25, 26, 27, 28, 29],
	},
];

export interface CommitteeGroup {
	key: string;
	name: string;
	topic: string;
	questions: StudyQuestion[];
}

export function groupByCommittee(questions: StudyQuestion[]): CommitteeGroup[] {
	const assigned = new Set(COMMITTEE.flatMap((m) => m.questions));
	const byNumber = (a: StudyQuestion, b: StudyQuestion) => a.n - b.n;

	const groups: CommitteeGroup[] = COMMITTEE.map((m) => ({
		key: m.key,
		name: m.name,
		topic: m.topic,
		questions: questions.filter((q) => m.questions.includes(q.n)).sort(byNumber),
	}));
	groups.push({
		key: 'pozostale',
		name: 'Pozostałe',
		topic: 'Pytania spoza specjalizacji komisji',
		questions: questions.filter((q) => !assigned.has(q.n)).sort(byNumber),
	});

	return groups.filter((g) => g.questions.length > 0);
}
