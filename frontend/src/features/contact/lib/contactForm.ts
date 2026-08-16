import type { ContactRequest } from '@/lib/publicApi';

export const SESSION_OPTIONS: { value: string; label: string }[] = [
	{ value: '', label: 'Wybierz rodzaj sesji' },
	{ value: 'slub', label: 'Fotografia ślubna' },
	{ value: 'plener', label: 'Sesja plenerowa' },
	{ value: 'portret', label: 'Sesja portretowa' },
	{ value: 'reportaz', label: 'Reportaż' },
	{ value: 'inne', label: 'Inne' },
];

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
