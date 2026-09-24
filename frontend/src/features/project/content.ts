export const SECTIONS = [
	{ id: 'pitch', label: 'Pitch 5 min' },
	{ id: 'liczby', label: 'Liczby' },
	{ id: 'architektura', label: 'Architektura' },
	{ id: 'decyzje', label: 'Decyzje' },
	{ id: 'problemy', label: 'Problemy' },
	{ id: 'ograniczenia', label: 'Ograniczenia' },
	{ id: 'pytania', label: 'Pytania komisji' },
	{ id: 'demo', label: 'Demo' },
] as const;

export type SectionId = (typeof SECTIONS)[number]['id'];

export interface PitchSegment {
	from: string;
	to: string;
	title: string;
	text: string[];
}

export const PITCH: PitchSegment[] = [
	{
		from: '0:00',
		to: '0:40',
		title: 'Problem i produkt',
		text: [
			'PhotoDrive to platforma do przekazywania zdjęć z sesji fotograficznych. Rozwiązuje realny problem fotografa: jak bezpiecznie oddać klientowi gotowe zdjęcia, kontrolując, co i kiedy klient widzi — i jednocześnie prowadzić publiczne portfolio na stronie wizytówce.',
			'Jedna aplikacja obsługuje trzy role. Administrator zarządza użytkownikami i portfolio. Fotograf zakłada klientom albumy, wgrywa zdjęcia i decyduje o ich widoczności, znaku wodnym i terminie usunięcia. Klient loguje się i pobiera wyłącznie to, co mu udostępniono. System działa produkcyjnie pod adresem photodrive.dev.',
		],
	},
	{
		from: '0:40',
		to: '1:40',
		title: 'Architektura',
		text: [
			'Backend napisałem w Javie 21 i Spring Boot, w architekturze heksagonalnej z elementami DDD. W centrum jest domena — agregaty Album i User — która nie zależy ani od Springa, ani od bazy danych. To w agregatach, a nie w kontrolerach, żyją reguły biznesowe i autoryzacja własności, na przykład „ten album należy do tego fotografa”. Dzięki temu domenę testuję bez uruchamiania Springa — ma około 95% pokrycia testami jednostkowymi.',
			'Warstwa aplikacji definiuje porty, a infrastruktura dostarcza adaptery: MySQL przez JPA, dysk na pliki zdjęć, serwer poczty. Frontend to aplikacja jednostronicowa w React 19 i TypeScript, z React Query do danych z serwera. Całość działa w kontenerach Dockera za Traefikiem z certyfikatem TLS.',
		],
	},
	{
		from: '1:40',
		to: '2:50',
		title: 'Trzy decyzje, z których jestem najbardziej zadowolony',
		text: [
			'Po pierwsze — spójność pliku i bazy. Zdjęcie leży na dysku, metadane w MySQL, a wspólnej transakcji nie ma. Rozwiązałem to zdarzeniami domenowymi z jawnie wybraną fazą transakcji: zapis pliku dzieje się przed zatwierdzeniem, więc awaria dysku wycofuje rekord, a mail wychodzi po zatwierdzeniu, bo wysłanego maila nie da się cofnąć.',
			'Po drugie — znak wodny. Pierwsza wersja wypalała go w plik: oryginał był tracony, a prośba o inny rozmiar zdjęcia omijała ochronę. Przeprojektowałem to — na pliku jest tylko flaga, a oznakowana wersja jest komponowana w locie i cache’owana. Oryginał zostaje nietknięty, a znak jest na każdym rozmiarze i w archiwum ZIP.',
			'Po trzecie — bezpieczeństwo. Token JWT w ciasteczku HttpOnly, niedostępny dla JavaScriptu, ochrona przed CSRF, limit prób logowania, hasło startowe generowane przez serwer i wymuszona zmiana hasła egzekwowana na serwerze, a nie tylko w interfejsie. Konsekwentnie rozróżniam 403 — „to nie twoje” — od 400 — „popraw dane”.',
		],
	},
	{
		from: '2:50',
		to: '3:50',
		title: 'Jakość i CI/CD',
		text: [
			'Backend ma 683 testy i około 91% pokrycia, frontend ponad 300 testów i około 78%. Testy integracyjne działają na prawdziwym MySQL w kontenerze — Testcontainers — bo baza w pamięci testowałaby siebie, a nie produkcję. Jest też macierz autoryzacji: każdy chroniony endpoint razy każda rola, na prawdziwym torze HTTP.',
			'Próg 70% pokrycia jest egzekwowany w obu częściach, a pipeline w GitHub Actions blokuje wdrożenie, gdy którykolwiek test jest czerwony. Procentu nie traktuję jako celu — jakość testów sprawdzam sabotażem: psuję regułę w kodzie i sprawdzam, czy test pada.',
		],
	},
	{
		from: '3:50',
		to: '4:30',
		title: 'Problemy, które rozwiązałem',
		text: [
			'Najciekawsze były błędy, których nie widać w interfejsie. Pisząc test, znalazłem wyciek deskryptorów plików — strumienie przy uploadzie i skalowaniu zdjęć nie były zamykane. Na Linuksie nic tego nie zdradza; wyszło dopiero wtedy, gdy test na Windowsie nie mógł skasować katalogu tymczasowego.',
			'Innym razem pipeline zameldował udane wdrożenie, choć nie zbudował nowego obrazu backendu — filtr zmian liczył różnicę względem poprzedniego pusha, a nie względem produkcji. Usunąłem selektywne budowanie i przyjąłem zasadę: produkcja zawsze odpowiada wierzchołkowi gałęzi main.',
		],
	},
	{
		from: '4:30',
		to: '5:00',
		title: 'Ograniczenia i kierunki rozwoju',
		text: [
			'Świadomie nazywam ograniczenia: nie ma narzędzia do migracji schematu bazy ani unieważniania tokenów — dezaktywacja konta nie kończy trwającej sesji. Przetwarzanie obrazów jest synchroniczne, a pliki leżą na lokalnym dysku, co utrudnia skalowanie poziome.',
			'Kierunki rozwoju to magazyn obiektowy typu S3, przetwarzanie asynchroniczne, generowanie klienta API z OpenAPI i odświeżane tokeny. Dziękuję — chętnie pokażę system na żywo.',
		],
	},
];

export function toSeconds(time: string): number {
	const [m, s] = time.split(':').map(Number);
	return m * 60 + s;
}
