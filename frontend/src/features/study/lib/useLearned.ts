import { useEffect, useState } from 'react';
import { readProgressFromHash } from './progressLink';

export const LEARNED_STORAGE_KEY = 'photodrive.study.learned';

export type LinkImport = { status: 'loaded'; count: number } | { status: 'invalid' } | null;

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

function initialState(): { learned: Set<number>; linkImport: LinkImport } {
	const fromLink = readProgressFromHash(window.location.hash);
	if (fromLink) return { learned: fromLink, linkImport: { status: 'loaded', count: fromLink.size } };
	return { learned: readLearned(), linkImport: fromLink === null ? { status: 'invalid' } : null };
}

export function useLearned() {
	const [initial] = useState(initialState);
	const [learned, setLearned] = useState<Set<number>>(initial.learned);

	useEffect(() => writeLearned(learned), [learned]);

	useEffect(() => {
		if (initial.linkImport) {
			const { pathname, search } = window.location;
			window.history.replaceState(window.history.state, '', pathname + search);
		}
	}, [initial.linkImport]);

	const toggleLearned = (n: number) =>
		setLearned((prev) => {
			const next = new Set(prev);
			if (next.has(n)) next.delete(n);
			else next.add(n);
			return next;
		});

	return { learned, toggleLearned, linkImport: initial.linkImport };
}
