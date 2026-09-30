import { describe, it, expect } from 'vitest';
import { questions } from './questions';
import type { StudyQuestion } from '../types';

describe('study questions data', () => {
	it('Contains all 67 exam questions numbered 1 to 67 in order, so none was lost in the document conversion', () => {
		// When / Then
		expect(questions.map((q) => q.n)).toEqual(
			Array.from({ length: 67 }, (_, i) => i + 1),
		);
	});

	it('Every question has both a short and an extended answer, since the page is built around the two-level study mode', () => {
		// When
		const incomplete = questions.filter((q) => !q.short.length || !q.long.length);

		// Then
		expect(incomplete).toEqual([]);
	});

	it('Every question has 3 to 5 short memory cues for learned mode, so a learned question shows prompts to recall and not the full answer again', () => {
		// When
		const invalid = questions
			.filter(
				(q) =>
					!Array.isArray(q.hints) ||
					q.hints.length < 3 ||
					q.hints.length > 5 ||
					q.hints.some((hint) => !hint.trim() || hint.length > 90),
			)
			.map((q) => q.n);

		// Then
		expect(invalid).toEqual([]);
	});

	it('The number-systems example converts correctly in every base and uses an easy-to-remember number, so it holds up when recited at the defence', () => {
		// Given
		const [first] = questions;
		const short = JSON.stringify(first.short);

		// When
		const match = short.match(/(\d+)\(10\) = ([01]+)\(2\) = ([0-7]+)\(8\) = ([0-9A-F]+)\(16\)/);

		// Then
		expect(match).not.toBeNull();
		const [, dec, bin, oct, hex] = match!;
		expect(dec).toBe('255');
		expect([parseInt(bin, 2), parseInt(oct, 8), parseInt(hex, 16)]).toEqual([255, 255, 255]);
		expect(JSON.stringify(first)).not.toContain('202');
	});

	it('The stack-and-queue answer treats the dynamic LIFO stack and the dynamic FIFO queue separately and compares them, since that is what the question asks', () => {
		// Given
		const q9 = questions.find((q) => q.n === 9)!;
		const headings = q9.long.flatMap((b) => (b.k === 'h3' ? [b.c.join('')] : []));

		// When
		const comparison = q9.long.find(
			(b) =>
				b.k === 'table' &&
				b.rows[0].join('|') === 'Cecha|Dynamiczny stos (LIFO)|Dynamiczna kolejka (FIFO)',
		);

		// Then
		expect(headings.some((h) => h.startsWith('Dynamiczny stos'))).toBe(true);
		expect(headings.some((h) => h.startsWith('Dynamiczna kolejka'))).toBe(true);
		expect(comparison).toBeDefined();
	});

	it('Contains no leftover Markdown backticks, so code names render as text and not as raw markup', () => {
		// When / Then
		expect(JSON.stringify(questions)).not.toContain('`');
	});

	it('Contains no raw Markdown heading markers in text blocks, so section titles render as headings and not as "###"', () => {
		// When
		const withMarkers = questions.flatMap((q) =>
			[...q.short, ...q.long]
				.filter(
					(block) =>
						block.k !== 'table' &&
						block.k !== 'code' &&
						block.c.some((part) => /(^|\s)#{2,}\s/.test(typeof part === 'string' ? part : part.b)),
				)
				.map(() => q.n),
		);

		// Then
		expect(withMarkers).toEqual([]);
	});

	it('Mentions the diploma project only in a closing "Z projektu" note, so the answers themselves stay general facts', () => {
		// Given
		const text = (blocks: StudyQuestion['long']) => JSON.stringify(blocks);

		// When
		const violations = questions
			.filter((q) => {
				const noteAt = q.long.findIndex((b) => b.k === 'h3' && b.c[0] === 'Z projektu');
				const body = noteAt === -1 ? q.long : q.long.slice(0, noteAt);
				const noteIsLast = noteAt === -1 || noteAt === q.long.length - 2;
				return /PhotoDrive/.test(text(q.short)) || /PhotoDrive/.test(text(body)) || !noteIsLast;
			})
			.map((q) => q.n);

		// Then
		expect(violations).toEqual([]);
	});

	it('The TCP/IP answer calls IP best effort and spells out the missing guarantees, so "unreliable" is not heard as "faulty"', () => {
		// Given
		const q23 = questions.find((q) => q.n === 23)!;

		// When
		const short = JSON.stringify(q23.short);

		// Then
		expect(short).toContain('best effort');
		expect(short).toContain('bez gwarancji dostarczenia, kolejności ani braku duplikatów');
		expect(JSON.stringify(q23)).not.toMatch(/zapewnia bezpołączeniowe, zawodne/);
	});

	it('The virtual-memory answer walks through a numeric translation that adds up, so the page-to-frame mapping can be recited with concrete numbers', () => {
		// Given
		const long = JSON.stringify(questions.find((q) => q.n === 12)!.long);

		// When
		const virtual = long.match(/adres wirtualny (\d+) = (\d+) · (\d+) \+ (\d+)/);
		const physical = long.match(/adres fizyczny = (\d+) · (\d+) \+ (\d+) = (\d+)/);

		// Then
		expect(virtual).not.toBeNull();
		expect(physical).not.toBeNull();
		const [address, page, pageSize, offset] = virtual!.slice(1).map(Number);
		const [frame, frameSize, frameOffset, result] = physical!.slice(1).map(Number);
		expect(page * pageSize + offset).toBe(address);
		expect(frame * frameSize + frameOffset).toBe(result);
		expect([frameSize, frameOffset]).toEqual([pageSize, offset]);
		expect(offset).toBeLessThan(pageSize);
		expect(frame).not.toBe(page);
	});

	it('Learned-mode cues spell concepts out in words and keep only proper names as abbreviations, since a bare acronym does not trigger recall', () => {
		// Given
		const properNames = new Set([
			'.NET', 'ACM', 'AGPL', 'ANSI', 'SPARC', 'ATM', 'AVIF', 'BMP', 'DOCSIS', 'ES', 'FF', 'FTTx', 'PON',
			'GIF', 'GPL', 'GSM', 'HTTP', 'IP', 'IS-IS', 'JPEG', 'LINQ', 'MIT', 'MPLS', 'NoSQL', 'OSPF', 'OpenGL',
			'PNG', 'RIP', 'RODO', 'RSA', 'SD-WAN', 'SQL', 'SVG', 'TCP', 'TIFF', 'RAW', 'UDP', 'UML', 'WebGL',
			'WebGPU', 'WebXR', 'gRPC', 'glTF', 'GLB', 'xDSL',
		]);

		// When
		const abbreviations = questions.flatMap((q) =>
			q.hints
				.flatMap((hint) => hint.match(/[\w.#+-]*[A-Z]{2,}[\w.#+/-]*/g) ?? [])
				.flatMap((token) => token.split('/'))
				.filter((token) => /[A-Z]{2,}/.test(token) && !properNames.has(token))
				.map((token) => `Q${q.n}: ${token}`),
		);

		// Then
		expect(abbreviations).toEqual([]);
	});

	it('Both licensing answers treat only a publicly available container image as distribution, so they do not contradict each other', () => {
		// Given
		const projectNote = (n: number) => JSON.stringify(questions.find((q) => q.n === n)!.long.at(-1));

		// When
		const notes = [projectNote(59), projectNote(67)];

		// Then
		notes.forEach((note) => expect(note).toMatch(/[Pp]ublicznie dostępny obraz|publiczne udostępnienie obrazu/));
		notes.forEach((note) => expect(note).not.toMatch(/bez dystrybucji programu nie rodzi|publikacja obrazów w rejestrze oznacza dystrybucję/));
	});
});
