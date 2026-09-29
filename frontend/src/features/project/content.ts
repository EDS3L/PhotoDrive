export const SECTIONS = [
	{ id: 'pitch', label: 'Pitch 5 min' },
	{ id: 'wyzwania', label: 'Wyzwania' },
	{ id: 'liczby', label: 'Liczby' },
	{ id: 'architektura', label: 'Architektura' },
	{ id: 'decyzje', label: 'Decyzje' },
	{ id: 'problemy', label: 'Problemy' },
	{ id: 'ograniczenia', label: 'Ograniczenia' },
	{ id: 'pytania', label: 'Pytania komisji' },
	{ id: 'demo', label: 'Demo' },
] as const;

export type SectionId = (typeof SECTIONS)[number]['id'];

export const SLIDES = [
	'PhotoDrive',
	'Cel projektu',
	'Kluczowe decyzje techniczne',
	'Kluczowe reguły domeny',
	'Bezpieczeństwo',
	'Aplikacja PhotoDrive',
	'Jakość: testy i CI/CD',
	'Wnioski i dalszy rozwój',
] as const;

export interface PitchSegment {
	slide: number;
	from: string;
	to: string;
	text: string[];
}

export const PITCH: PitchSegment[] = [
	{
		slide: 1,
		from: '0:00',
		to: '0:20',
		text: [
			'Dzień dobry. Nazywam się Bartłomiej Banakiewicz, a tematem mojej pracy jest PhotoDrive — platforma do zarządzania i dystrybucji sesji fotograficznych. System nie jest prototypem: działa produkcyjnie pod adresem photodrive.dev i obsługuje trzy role — administratora, fotografa i klienta.',
		],
	},
	{
		slide: 2,
		from: '0:20',
		to: '1:00',
		text: [
			'Problem jest prosty: fotograf po sesji wysyła klientowi link albo archiwum i traci kontrolę. Nie wybiera ujęć, nie ma znaku wodnego, nie decyduje, jak długo materiał jest dostępny, a prywatne zdjęcia klienta i portfolio mieszają się w jednym miejscu.',
			'PhotoDrive prowadzi materiał jednym przepływem: upload, kuratorowanie, udostępnienie, strefa klienta i pobranie w ZIP-ie. Administrator zarządza kontami i publikuje portfolio, fotograf wgrywa i kuratoruje zdjęcia, a klient widzi i pobiera wyłącznie to, co mu udostępniono.',
		],
	},
	{
		slide: 3,
		from: '1:00',
		to: '1:45',
		text: [
			'Backend napisałem w Javie 21 i Spring Boot 3.5, w architekturze heksagonalnej z elementami DDD. Cztery warstwy — prezentacja, aplikacja, domena i infrastruktura — a zależności wskazują do środka: domena, czyli agregaty Album, User i File, nie zna ani Springa, ani bazy danych. Dzięki temu każdą regułę biznesową testuję w milisekundach, bez uruchamiania frameworka.',
			'Warstwa aplikacji definiuje porty, a infrastruktura dostarcza adaptery: MySQL, dysk na zdjęcia, pocztę i JWT. Frontend to aplikacja SPA w React 19 i TypeScript, z React Query do danych z serwera. Całość działa w Dockerze, za nginx-em i Traefikiem z certyfikatem TLS.',
		],
	},
	{
		slide: 4,
		from: '1:45',
		to: '2:35',
		text: [
			'Najważniejsze zachowania produktu wymuszają agregaty, a nie dyscyplina użytkownika. Nowe zdjęcie w albumie klienta jest domyślnie ukryte — i nie jest chowane w interfejsie, tylko w ogóle nie wychodzi z API. Znak wodny to flaga na pliku: wersja oznakowana powstaje w locie i trafia do cache, więc oryginał zostaje nietknięty, a znaku nie da się obejść innym rozmiarem ani archiwum ZIP.',
			'Album klienta ma czas życia — scheduler usuwa go po terminie. Z dwóch reguł wynika niezmiennik: zdjęcie w portfolio nigdy nie ma znaku wodnego. A spójność pliku i bazy zapewniają zdarzenia domenowe: operacje plikowe przed zatwierdzeniem transakcji, maile dopiero po nim.',
		],
	},
	{
		slide: 5,
		from: '2:35',
		to: '3:20',
		text: [
			'O własności zasobu decyduje reguła w domenie, a nie konfiguracja Springa. Sesja to token JWT w ciasteczku HttpOnly, niedostępny dla JavaScriptu, z walidacją nagłówka Origin przeciw CSRF. Hasło startowe generuje serwer i wysyła mailem — twórca konta go nie zna — a do czasu jego zmiany filtr odrzuca każde inne żądanie, więc ominięcie interfejsu nic nie daje.',
			'Reset hasła odpowiada identycznie na każdą porażkę, a logowanie i reset mają limity prób. Konsekwentnie rozróżniam 403 — „to nie twoje” — od 400 — „popraw dane”. Pilnuje tego macierz autoryzacji: 63 przypadki testu integracyjnego na prawdziwym torze HTTP i prawdziwej bazie.',
		],
	},
	{
		slide: 6,
		from: '3:20',
		to: '3:45',
		text: [
			'Tak wygląda aplikacja: jedna SPA, trzy doświadczenia — strona publiczna z portfolio, panel pracy fotografa i administratora oraz strefa klienta. Zakres obejmuje konta z automatycznym hasłem, kuratorowanie widoczności, znak wodny, czas życia albumu, portfolio ze stroną wizytówką, formularz kontaktowy, pięć szablonów maili i pobieranie ZIP.',
		],
	},
	{
		slide: 7,
		from: '3:45',
		to: '4:30',
		text: [
			'Testy pilnują reguł, nie procentów. W obu częściach jest 987 testów: 683 w backendzie i 304 we frontendzie. Pokrycie linii to 90,8% w backendzie i 77,9% we frontendzie, a próg 70% jest egzekwowany w CI — spadek blokuje wdrożenie. Testy integracyjne działają na prawdziwym MySQL w kontenerze, bo baza w pamięci testowałaby siebie, a nie produkcję.',
			'Jakość testów sprawdzałem sabotażem: celowo psułem reguły i sprawdzałem, czy testy padają. Pisanie testów wykryło też realne błędy, na przykład wyciek deskryptorów plików. Każdy push na main przechodzi testy obu części, budowę obrazów i deploy na serwer.',
		],
	},
	{
		slide: 8,
		from: '4:30',
		to: '5:00',
		text: [
			'Projekt osiągnął cel: bezpieczne dostarczanie sesji działa produkcyjnie. Najważniejszy wniosek jest taki, że reguły w domenie sprawiają, iż kluczowe własności systemu są wymuszane przez model i potwierdzone testami.',
			'Świadome ograniczenia to brak unieważniania tokenów, synchroniczne przetwarzanie obrazów i pliki na dysku jednej instancji. Kierunki rozwoju: ścieżki plików po identyfikatorach, kolejka asynchroniczna i testy end-to-end w Playwright. Dziękuję za uwagę.',
		],
	},
];

export interface ChallengeStory {
	title: string;
	text: string;
}

export const CHALLENGES_INTRO =
	'Największe wyzwania nie polegały na pisaniu funkcji, tylko na błędach, których nie widać w interfejsie. Opowiem o czterech — jednym projektowym, dwóch znalezionych dzięki testom i jednym z wdrażania.';

export const CHALLENGES: ChallengeStory[] = [
	{
		title: 'Znak wodny — przeprojektowany od zera',
		text: 'Pierwsza wersja wypalała znak wodny w plik. To były dwa problemy naraz: oryginał przepadał bezpowrotnie, a zdjęcie w innym rozmiarze, zamawiane parametrem width, powstawało z oryginału — bez znaku, więc ochronę omijała zmiana adresu. Do tego logo leżało w folderze na serwerze, o którym baza nic nie wiedziała. Zatrzymałem pracę i przeprojektowałem to: na pliku jest tylko flaga, logo trzymam w bazie, a wersja ze znakiem powstaje w locie przy odczycie i trafia do kasowalnego cache. Oryginał zostaje nietknięty, a znak jest na każdym rozmiarze i w ZIP-ie. Wniosek: stan, którego baza nie zna, prędzej czy później się rozjedzie.',
	},
	{
		title: 'Próg pokrycia, który niczego nie blokował',
		text: 'Projekt deklarował próg pokrycia 70%, ale zadania, które go sprawdza, nikt nie wywoływał, a wykluczenia jednocześnie chowały nieprzetestowany kod i nie liczyły przetestowanego. Uczciwy pomiar dał 56%. Najbardziej zabolało to, że mechanizm spójności pliku i bazy, którym się chwalę, nie miał ani jednego testu. Poprawiłem pomiar, wpiąłem bramkę w build i w CI, dopisałem testy — dziś backend ma 90,8%. A żeby procent nie stał się celem, jakość testów sprawdzam sabotażem: psuję regułę i test musi paść. Przykład: celowe zepsucie siedmiu reguł frontu wywróciło jedenaście testów.',
	},
	{
		title: 'Wyciek deskryptorów znaleziony przez test',
		text: 'Strumienie przy uploadzie i przy skalowaniu zdjęć nie były zamykane — metody Javy, których użyłem, z kontraktu nie zamykają źródła. Każde wgrane zdjęcie i każda zmniejszona wersja zostawiały otwarty plik. Na Linuksie, czyli na produkcji, nic tego nie zdradza. Wyszło na Windowsie: test nie mógł usunąć katalogu tymczasowego, bo był w nim otwarty plik. Poprawka to try-with-resources, a potem przejrzałem wszystkie podobne wywołania w kodzie — to był ostatni taki wyciek. Ważniejszy jest wniosek: testy łapią rzeczy, których przegląd kodu nie widzi.',
	},
	{
		title: 'Zielony deploy bez nowego obrazu',
		text: 'Pipeline zameldował udane wdrożenie, a na produkcji nie było poprawki. Budowałem tylko tę część, która się zmieniła, liczoną względem poprzedniego pusha. Przy kilku pushach pod rząd GitHub anulował oczekujący przebieg ze zmianą backendu, a następny dotykał tylko frontu — więc backend się nie przebudował, a deploy wypchnął stary obraz na zielono. To zdarzyło się naprawdę: poprawka wycieku przez chwilę nie istniała na produkcji, choć wszystko świeciło na zielono. Usunąłem selektywne budowanie: każde wdrożenie buduje oba stacki z wierzchołka gałęzi main. Wniosek: optymalizacja potrafi być źródłem ryzyka, a zielony status to jeszcze nie dowód.',
	},
];

export const CHALLENGES_OUTRO =
	'Wspólny mianownik: najgroźniejsze były błędy ciche — bez komunikatu i bez objawów w interfejsie. Dlatego w projekcie obowiązuje zasada, że każdy błąd najpierw dostaje test, który go odtwarza, a dopiero potem poprawkę.';

export const TEST_STATS = {
	total: 987,
	backend: 683,
	frontend: 304,
	pyramid: { domain: 296, unit: 286, integration: 101 },
	authorizationMatrix: 63,
} as const;

export function toSeconds(time: string): number {
	const [m, s] = time.split(':').map(Number);
	return m * 60 + s;
}

export function wordCount(texts: readonly string[]): number {
	return texts.join(' ').split(/\s+/).filter(Boolean).length;
}
