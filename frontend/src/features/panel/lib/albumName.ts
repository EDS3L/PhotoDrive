/**
 * Lustro reguły backendu (`AlbumPath`): nazwa albumu jest ścieżką folderu, więc wolno jej
 * zawierać tylko ASCII. Walidujemy we froncie, żeby admin dostał polski komunikat inline
 * zamiast surowego angielskiego 400 z domeny w toaście (B.34). Backend zostaje ostatnią
 * linią obrony — ta funkcja może być tylko RÓWNIE restrykcyjna co `AlbumPath`, nigdy mniej.
 */
const ALBUM_NAME_PATTERN = /^[a-zA-Z0-9\-_/ .@+]+$/;

/** Komunikat błędu dla nazwy technicznej albumu; `undefined` = nazwa poprawna (lub pusta). */
export function albumNameError(name: string): string | undefined {
	if (name.trim() === '' || ALBUM_NAME_PATTERN.test(name)) return undefined;
	return 'Nazwa techniczna nie może zawierać polskich znaków — dozwolone są litery a–z, cyfry, spacje i - _ . @ + / . Polską nazwę („Śluby") ustawisz po utworzeniu jako etykietę zakładki.';
}
