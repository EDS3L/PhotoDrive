import { describe, it, expect } from 'vitest';
import { buildContactPayload, sessionTypeLabel } from './contactForm';

describe('contact form payload (8.1)', () => {
	it('sends the human-readable session label, not the internal code, so the studio email is readable', () => {
		// Given - the form holds the select code
		const values = {
			name: 'Jan',
			email: 'jan@example.com',
			sessionType: 'slub',
			message: 'Dzień dobry, pytanie o termin.',
		};

		// When
		const payload = buildContactPayload(values);

		// Then - the backend receives "Fotografia ślubna", not "slub"
		expect(payload.sessionType).toBe('Fotografia ślubna');
	});

	it('drops an empty phone to undefined, so an optional field is not sent as a blank string', () => {
		// Given
		const values = {
			name: 'Jan',
			email: 'jan@example.com',
			phone: '   ',
			sessionType: 'inne',
			message: 'Dłuższa wiadomość testowa.',
		};

		// When
		const payload = buildContactPayload(values);

		// Then
		expect(payload.phone).toBeUndefined();
	});

	it('trims the free-text fields, so leading/trailing whitespace never reaches the mailbox', () => {
		// When
		const payload = buildContactPayload({
			name: '  Jan  ',
			email: '  jan@example.com  ',
			sessionType: 'inne',
			message: '  Wiadomość z odstępami.  ',
		});

		// Then
		expect(payload.name).toBe('Jan');
		expect(payload.email).toBe('jan@example.com');
		expect(payload.message).toBe('Wiadomość z odstępami.');
	});

	it('falls back to the raw code for an unknown session type, so nothing is silently lost', () => {
		// When / Then
		expect(sessionTypeLabel('nieistniejacy-kod')).toBe('nieistniejacy-kod');
	});
});
