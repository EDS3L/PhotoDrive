const ALBUM_NAME_PATTERN = /^[a-zA-Z0-9\-_/ .@+]+$/;

export function albumNameError(name: string): string | undefined {
	if (name.trim() === '' || ALBUM_NAME_PATTERN.test(name)) return undefined;
	return 'Nazwa techniczna nie może zawierać polskich znaków — dozwolone są litery a–z, cyfry, spacje i - _ . @ + / . Polską nazwę („Śluby") ustawisz po utworzeniu jako etykietę zakładki.';
}
