export const SYNC_HASH_PARAM = 'sync';

const CODE_PATTERN = /^[A-Za-z0-9_-]{22}$/;

export function generateSyncCode(): string {
	const bytes = crypto.getRandomValues(new Uint8Array(16));
	return btoa(String.fromCharCode(...bytes))
		.replace(/\+/g, '-')
		.replace(/\//g, '_')
		.replace(/=+$/, '');
}

export function isSyncCode(value: string): boolean {
	return CODE_PATTERN.test(value);
}

export function readSyncCodeFromHash(hash: string): string | null | undefined {
	const code = new URLSearchParams(hash.replace(/^#/, '')).get(SYNC_HASH_PARAM);
	if (code === null) return undefined;
	return isSyncCode(code) ? code : null;
}

export function parseSyncInput(input: string): string | null {
	const trimmed = input.trim();
	if (isSyncCode(trimmed)) return trimmed;
	const hashAt = trimmed.indexOf('#');
	return hashAt === -1 ? null : (readSyncCodeFromHash(trimmed.slice(hashAt)) ?? null);
}

export function syncUrl(code: string, base: string): string {
	return `${base.split('#')[0]}#${SYNC_HASH_PARAM}=${code}`;
}
