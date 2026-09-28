export const PROGRESS_HASH_PARAM = 'p';

export function encodeProgress(learned: Set<number>): string {
	const max = Math.max(0, ...learned);
	const bytes = new Uint8Array(Math.ceil(max / 8));
	for (const n of learned) {
		if (Number.isInteger(n) && n >= 1) bytes[(n - 1) >> 3] |= 1 << ((n - 1) & 7);
	}
	return btoa(String.fromCharCode(...bytes))
		.replace(/\+/g, '-')
		.replace(/\//g, '_')
		.replace(/=+$/, '');
}

export function decodeProgress(code: string): Set<number> | null {
	if (!/^[A-Za-z0-9_-]*$/.test(code)) return null;
	let binary: string;
	try {
		binary = atob(code.replace(/-/g, '+').replace(/_/g, '/'));
	} catch {
		return null;
	}
	const learned = new Set<number>();
	for (let i = 0; i < binary.length; i++) {
		const byte = binary.charCodeAt(i);
		for (let bit = 0; bit < 8; bit++) {
			if (byte & (1 << bit)) learned.add(i * 8 + bit + 1);
		}
	}
	return learned;
}

export function readProgressFromHash(hash: string): Set<number> | null | undefined {
	const code = new URLSearchParams(hash.replace(/^#/, '')).get(PROGRESS_HASH_PARAM);
	return code === null ? undefined : decodeProgress(code);
}

export function progressUrl(learned: Set<number>, base: string): string {
	return `${base.split('#')[0]}#${PROGRESS_HASH_PARAM}=${encodeProgress(learned)}`;
}
