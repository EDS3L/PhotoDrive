import type { ContactRequest } from '@/lib/publicApi';

/**
 * Jedyne źródło rodzajów sesji: zasila zarówno {@code <Select>} w formularzu, jak i mapowanie
 * kodu na etykietę przy wysyłce. Formularz przechowuje KOD (`slub`), ale do studia leci już
 * czytelna ETYKIETA (`Fotografia ślubna`), więc backend nie musi znać taksonomii frontu.
 */
export const SESSION_OPTIONS: { value: string; label: string }[] = [
	{ value: '', label: 'Wybierz rodzaj sesji' },
	{ value: 'slub', label: 'Fotografia ślubna' },
	{ value: 'plener', label: 'Sesja plenerowa' },
	{ value: 'portret', label: 'Sesja portretowa' },
	{ value: 'reportaz', label: 'Reportaż' },
	{ value: 'inne', label: 'Inne' },
];

/** Kod rodzaju sesji → etykieta widoczna dla człowieka; nieznany kod przechodzi bez zmian. */
export function sessionTypeLabel(code: string): string {
	return SESSION_OPTIONS.find((option) => option.value === code)?.label ?? code;
}

export interface ContactFormValues {
	name: string;
	email: string;
	phone?: string;
	sessionType: string;
	message: string;
}

/** Buduje payload dla backendu: przycina pola, gubi pusty telefon i podmienia kod sesji na etykietę. */
export function buildContactPayload(values: ContactFormValues): ContactRequest {
	const phone = values.phone?.trim();
	return {
		name: values.name.trim(),
		email: values.email.trim(),
		phone: phone ? phone : undefined,
		sessionType: sessionTypeLabel(values.sessionType),
		message: values.message.trim(),
	};
}
