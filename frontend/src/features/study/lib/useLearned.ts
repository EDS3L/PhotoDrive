import { useEffect, useState } from 'react';

export const LEARNED_STORAGE_KEY = 'photodrive.study.learned';

function readLearned(): Set<number> {
	try {
		const parsed: unknown = JSON.parse(localStorage.getItem(LEARNED_STORAGE_KEY) ?? '[]');
		return new Set(Array.isArray(parsed) ? parsed.filter((n) => Number.isInteger(n)) : []);
	} catch {
		return new Set();
	}
}

function writeLearned(learned: Set<number>) {
	try {
		localStorage.setItem(LEARNED_STORAGE_KEY, JSON.stringify([...learned]));
	} catch {
		return;
	}
}

export function useLearned() {
	const [learned, setLearned] = useState<Set<number>>(readLearned);

	useEffect(() => writeLearned(learned), [learned]);

	const toggleLearned = (n: number) =>
		setLearned((prev) => {
			const next = new Set(prev);
			if (next.has(n)) next.delete(n);
			else next.add(n);
			return next;
		});

	return { learned, toggleLearned };
}
