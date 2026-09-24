import type { ReactNode } from 'react';
import { PageHeader } from '@/shared/components/layout/PageHeader';
import { PITCH, SECTIONS, type SectionId } from './content';

function Section({ id, title, lead, children }: { id: SectionId; title: string; lead?: string; children: ReactNode }) {
	return (
		<section id={id} aria-labelledby={`${id}-title`} className='mt-16 scroll-mt-28'>
			<h2 id={`${id}-title`} className='font-serif text-4xl text-foreground'>
				{title}
			</h2>
			{lead && <p className='mt-2 text-muted'>{lead}</p>}
			<div className='mt-6'>{children}</div>
		</section>
	);
}

function Card({ title, children }: { title: string; children: ReactNode }) {
	return (
		<div className='border border-border bg-surface p-5 sm:p-6'>
			<h3 className='font-serif text-xl text-accent'>{title}</h3>
			<div className='mt-3 space-y-2 text-[15px] leading-relaxed text-foreground/80'>{children}</div>
		</div>
	);
}

function Row({ label, children }: { label: string; children: ReactNode }) {
	return (
		<p>
			<span className='text-xs uppercase tracking-widest text-muted mr-2'>{label}</span>
			{children}
		</p>
	);
}

const FACTS: [string, string][] = [
	['683', 'testy backendu (JUnit 5, BDDMockito, Testcontainers)'],
	['90,8%', 'pokrycia linii backendu — domena 94,5%'],
	['300+', 'testów frontendu (Vitest + React Testing Library)'],
	['~78%', 'pokrycia linii frontendu'],
	['70%', 'próg pokrycia egzekwowany w CI w obu częściach'],
	['63', 'przypadki w macierzy autoryzacji (endpoint × rola)'],
	['~50', 'endpointów REST w 8 kontrolerach'],
	['3', 'role: administrator, fotograf, klient'],
	['25', 'udokumentowanych decyzji projektowych (ADR)'],
	['2560 px', 'maksymalny rozmiar zdjęcia dla gościa'],
	['60 min', 'sesja JWT z automatycznym przedłużaniem'],
	['10 / 15 min', 'limit prób logowania na adres IP'],
];

const DECISIONS: { title: string; what: string; why: string; cost: string }[] = [
	{
		title: 'Architektura heksagonalna + DDD',
		what: 'Domena (Album, User) w centrum, bez zależności od Springa i JPA; porty w warstwie aplikacji, adaptery w infrastrukturze.',
		why: 'Reguły biznesowe nie rozpełzają się po serwisach i kontrolerach, a domenę testuję w milisekundach, bez kontekstu Springa.',
		cost: 'Ręczne mapowanie encja ↔ model domenowy i więcej plików na tę samą funkcję.',
	},
	{
		title: 'Autoryzacja własności w domenie',
		what: 'Spring Security pilnuje ról, a to, czy album należy do użytkownika, sprawdza agregat (canAccess / canRead).',
		why: 'Konfiguracja Springa nie potrafi powiedzieć „ten album jest tego fotografa” — to reguła biznesowa.',
		cost: 'Żeby odpowiedzieć „kto ma dostęp do X”, trzeba przeczytać dwa miejsca.',
	},
	{
		title: 'Zdarzenia domenowe z wyborem fazy transakcji',
		what: 'Operacje plikowe w BEFORE_COMMIT, maile i usuwanie folderów w AFTER_COMMIT.',
		why: 'Awaria dysku wycofuje rekord w bazie (nie powstaje rekord bez pliku), a mail nie wychodzi dla transakcji, która padła.',
		cost: 'Brak ponowień — nieudany mail przepada (zostaje w logu). Alternatywa to wzorzec outbox z kolejką.',
	},
	{
		title: 'Pliki na dysku, metadane w bazie',
		what: 'Oryginały i miniatury na wolumenie, metadane w MySQL. Wyjątek: logo znaku wodnego i zdjęcia sekcji strony jako BLOB.',
		why: 'Setki zdjęć po kilkanaście MB nie powinny obciążać bazy ani jej kopii zapasowych.',
		cost: 'Aplikacja jest stanowa — skalowanie poziome wymaga wspólnego wolumenu albo magazynu S3.',
	},
	{
		title: 'Znak wodny komponowany w locie',
		what: 'Na pliku jest tylko flaga; wersja ze znakiem powstaje przy odczycie (kafelki) i trafia do cache. Oryginał nietknięty.',
		why: 'Znak można zdjąć w każdej chwili i jest na każdym rozmiarze oraz w ZIP-ie — nie da się go obejść parametrem ?width.',
		cost: 'Pierwsze żądanie wariantu obciąża procesor; pełną kompozycję chroni semafor przed wysyceniem pamięci.',
	},
	{
		title: 'Portfolio: cap 2560 px i nigdy znak wodny',
		what: 'Gość nigdy nie dostaje oryginału. Znaku nie da się ustawić na albumie portfolio, a przenoszenie zdjęć działa tylko portfolio ↔ portfolio.',
		why: 'Z tych dwóch reguł wynika niezmiennik „plik portfolio nigdy nie ma znaku wodnego” — wynika z kodu, nie z dyscypliny użytkownika.',
		cost: 'Portfolio nie opublikuje zdjęcia w pełnej rozdzielczości.',
	},
	{
		title: 'JWT w ciasteczku HttpOnly',
		what: 'Cookie HttpOnly + Secure + SameSite=Strict, TTL 60 min z przedłużaniem przy aktywności, filtr sprawdzający nagłówek Origin.',
		why: 'Token w localStorage wyniósłby każdy atak XSS — tu JavaScript w ogóle nie ma do niego dostępu.',
		cost: 'Ciasteczko otwiera ryzyko CSRF, dlatego SameSite plus własny filtr Origin (obrona w głąb).',
	},
	{
		title: '403 dla odmowy, 400 dla złamanej reguły',
		what: 'Dwie rodziny wyjątków: „to nie twoje” (403) i „popraw dane” (400), zapięte testem w obie strony.',
		why: 'Klient API musi wiedzieć, czy ponowić z innymi danymi; 400 w miejscu 403 maskuje próby włamania w logach.',
		cost: 'Konwencji trzeba pilnować przy każdym nowym wyjątku — dlatego test odwrotny: właściciel łamiący regułę nadal dostaje 400, a nie 403.',
	},
	{
		title: 'Hasło startowe generuje serwer',
		what: 'Nikt nie wpisuje hasła za użytkownika; przychodzi mailem, a zmiana przy pierwszym logowaniu jest wymuszona w filtrze serwera.',
		why: 'Wcześniej twórca konta znał hasło i zwykle było słabe; blokada tylko w UI dawała się obejść curl-em.',
		cost: 'Bez działającej poczty konto jest bezużyteczne — świadomie.',
	},
	{
		title: 'Testcontainers zamiast H2',
		what: 'Testy integracyjne startują prawdziwy MySQL 8 w kontenerze (jeden na cały przebieg).',
		why: 'H2 ma inne typy i semantykę zapytań — test na nim sprawdzałby H2, a nie produkcję.',
		cost: 'Testy wymagają Dockera i trwają ~1,5 min zamiast kilkunastu sekund.',
	},
];

const PROBLEMS: { title: string; problem: string; fix: string; lesson: string }[] = [
	{
		title: 'Wyciek deskryptorów plików',
		problem: 'Każde wgrane zdjęcie i każde zapytanie o zmniejszony wariant zostawiało otwarty plik — Files.copy(InputStream) i ImageIO.read(InputStream) nie zamykają źródła.',
		fix: 'Strumienie zawsze w try-with-resources. Drugi wyciek znalazł test: na Windowsie JUnit nie mógł usunąć katalogu tymczasowego z otwartym uchwytem.',
		lesson: 'Na Linuksie ten błąd jest niewidoczny — testy znajdują rzeczy, których przegląd kodu nie widzi.',
	},
	{
		title: 'Zielony deploy bez nowego obrazu',
		problem: 'Pipeline budował tylko zmieniony stack, liczony względem poprzedniego pusha. Przy serii pushy anulowany przebieg niósł zmianę backendu, następny już nie — i wdrożył stary obraz, meldując sukces.',
		fix: 'Usunięte selektywne budowanie: każdy deploy buduje oba stacki z HEAD-a, plus ręczne uruchomienie (workflow_dispatch).',
		lesson: 'Optymalizacja może być źródłem ryzyka, a zielony status nie jest dowodem poprawności.',
	},
	{
		title: 'Próg pokrycia, który niczego nie blokował',
		problem: 'Konfiguracja deklarowała 70%, ale zadanie weryfikacji nikt nie wołał, a wykluczenia zawyżały wynik. Uczciwy pomiar dał 56%.',
		fix: 'Jedna lista wykluczeń (tylko kod bez logiki), bramka wpięta w gradlew check i w CI, dopisane testy — dziś 90,8%.',
		lesson: 'Wykluczenie klasy z regułami z raportu to nie optymalizacja, tylko fałszowanie wyniku.',
	},
	{
		title: 'Znak wodny wypalany w plik',
		problem: 'Oryginał był nadpisywany, a zdjęcie w innym rozmiarze (?width) powstawało z oryginału — bez znaku.',
		fix: 'Flaga na pliku + kompozycja w locie do kasowalnego cache; globalne logo jako BLOB w bazie.',
		lesson: 'Stan, którego baza nie zna (folder z logo), to przyszły błąd.',
	},
	{
		title: 'Przenoszenie zdjęć — ciche nadpisanie i brak kontroli właściciela',
		problem: 'Na Linuksie przeniesienie pliku podmieniało istniejący plik o tej samej nazwie, a dowolny fotograf mógł przenosić pliki między cudzymi albumami.',
		fix: 'Kontrola własności obu albumów, odrzucenie kolizji nazw w domenie, okno zmiany nazwy w interfejsie; potem reguła: tylko portfolio ↔ portfolio.',
		lesson: 'Najgroźniejsze błędy to te, które nie dają żadnego komunikatu.',
	},
	{
		title: 'Wymuszona zmiana hasła tylko w interfejsie',
		problem: 'Ekran zmiany hasła blokował panel, ale zapytanie curl-em przechodziło.',
		fix: 'Token niesie flagę, filtr serwera odpowiada 403 na wszystko poza zmianą hasła; po zmianie wydawane jest nowe, czyste ciasteczko.',
		lesson: 'Interfejs to wygoda, bezpieczeństwo musi być na serwerze.',
	},
	{
		title: 'Rozmyte miniatury i utrata jakości',
		problem: 'Zmniejszanie jednym krokiem gubiło detale (banding), a przeniesione zdjęcie traciło miniaturę i jakość.',
		fix: 'Skalowanie schodkowe — po połowie na krok; przy przenoszeniu wędruje też miniatura.',
		lesson: 'Algorytm skalowania ma znaczenie widoczne gołym okiem.',
	},
	{
		title: 'Literówka w kodzie resetu = błąd 500',
		problem: 'Kod z maila był typu UUID, więc literówka wywracała deserializację zanim żądanie dotarło do serwisu.',
		fix: 'Kod przyjmowany jako tekst i parsowany w serwisie — każda porażka daje ten sam błąd 400 (anty-enumeracja kont).',
		lesson: 'Złe dane wejściowe to normalny przebieg, a nie wyjątek serwera.',
	},
];

const LIMITS: { title: string; say: string }[] = [
	{
		title: 'Brak narzędzia migracji schematu (Flyway / Liquibase)',
		say: 'Świadoma decyzja: przy jednej instancji i jednym autorze koszt przewyższał korzyść. Schemat odtwarzam z encji, skryptu V1 i idempotentnych łatek. Pierwsza rzecz do dodania przy zespole lub drugim środowisku.',
	},
	{
		title: 'Brak unieważniania tokenów',
		say: 'Wylogowanie kasuje ciasteczko, ale token jest ważny do wygaśnięcia, a dezaktywacja konta nie kończy trwającej sesji. Sensowny fix to lista unieważnień lub sprawdzanie aktywności przy każdym żądaniu — zmiana mechanizmu sesji, nie łatka.',
	},
	{
		title: 'Synchroniczne przetwarzanie obrazów i ZIP',
		say: 'Skalowanie, znak wodny i ZIP liczą się w wątku żądania przy limicie 768 MB. Łagodzą to upload w paczkach, semafor i cache wariantów; docelowo kolejka i przetwarzanie asynchroniczne.',
	},
	{
		title: 'Pliki na lokalnym dysku',
		say: 'Aplikacja jest stanowa — kopia zapasowa musi objąć dysk i bazę, a skalowanie poziome wymaga magazynu obiektowego (S3 / MinIO).',
	},
	{
		title: 'Ręcznie utrzymywany kontrakt API',
		say: 'Typy frontu i DTO backendu żyją osobno; kształt żądań przypinają testy. Docelowo generowanie klienta z OpenAPI (springdoc już jest w projekcie).',
	},
	{
		title: 'Nazwa albumu tylko ASCII',
		say: 'Z nazwy wywodzi się ścieżka na dysku, więc walidacja chroni przed path traversal. Polska nazwa zakładki żyje w osobnym polu displayName. Docelowo ścieżki po identyfikatorach.',
	},
	{
		title: 'Formaty tylko JPG i PNG',
		say: 'Standardowe ImageIO nie czyta WebP ani HEIC — zamiast udawać wsparcie, takie pliki są odrzucane przy uploadzie z czytelnym komunikatem.',
	},
];

const QUESTIONS: [string, string][] = [
	['Gdzie trzymasz token sesji?', 'W ciasteczku HttpOnly, Secure, SameSite=Strict — nie w localStorage, bo tam każdy XSS wynosi sesję. Ryzyko CSRF zamyka SameSite plus filtr sprawdzający Origin.'],
	['Dlaczego heksagonalna, a nie zwykłe warstwy?', 'Bo reguły siedzą w agregatach i testują się bez Springa — domena ma 94,5% pokrycia testami jednostkowymi. Kosztem jest ręczne mapowanie encja ↔ domena.'],
	['Jak plik na dysku i rekord w bazie się nie rozjeżdżają?', 'Zapis pliku w zdarzeniu BEFORE_COMMIT — awaria dysku wycofuje transakcję. Maile po zatwierdzeniu, bo ich nie da się cofnąć.'],
	['Czemu nie ma Flyway?', 'Świadomie: jedna instancja, jeden autor, a narzędzia, którego nie umiałbym obronić, nie wprowadzałem. To nazwane ograniczenie — pierwsze do dodania przy zespole.'],
	['Co się stanie, gdy zablokuję użytkownika w trakcie pracy?', 'Nie zostanie odcięty od razu — logowanie sprawdza aktywność, filtr sesji już nie. Znany, opisany przypadek; poprawka to zmiana mechanizmu sesji.'],
	['Czy testy nie są pisane pod procent?', 'Próg jest egzekwowany, ale jakość sprawdzam sabotażem — psuję regułę i test musi paść. Tak znalazłem test, który przechodził zawsze, bo jsdom nie modeluje pola wyboru pliku.'],
	['Czy testy coś realnie złapały?', 'Tak: wyciek deskryptora przy skalowaniu zdjęć i wywrócenie listy klientów przez jeden osierocony rekord — oba przy pisaniu testów.'],
	['Co z wydajnością przy dużych sesjach?', 'Przetwarzanie jest synchroniczne w 768 MB pamięci; łagodzą to paczki uploadu, semafor i cache. Kierunek to kolejka i przetwarzanie asynchroniczne.'],
	['Jak to jest wdrażane?', 'Push na main → GitHub Actions: testy obu części, obrazy do Docker Hub, deploy SSH na VPS za Traefikiem z TLS. Czerwony test blokuje wdrożenie.'],
	['Co byś zrobił inaczej?', 'Generowałbym klienta TypeScript z OpenAPI i od początku projektował ścieżki plików po identyfikatorach, a nie po nazwach.'],
];

const DEMO: [string, string][] = [
	['Upload i kuratorowanie', 'Świeżo wgrane zdjęcie jest domyślnie niewidoczne dla klienta; znak wodny nakładany w locie, oryginał nietknięty.'],
	['Strefa klienta', 'Pierwsze logowanie wymusza zmianę hasła (blokada na serwerze); klient widzi i pobiera w ZIP-ie tylko udostępnione zdjęcia.'],
	['Dowód autoryzacji', 'Zalogowany klient woła w pasku adresu zdjęcie z cudzego albumu → 403 od domeny, nie od interfejsu. Adres przygotuj dzień wcześniej.'],
];

export default function ProjectPage() {
	return (
		<div className='max-w-4xl mx-auto px-4 sm:px-6 pb-24'>
			<title>Projekt — PhotoDrive</title>
			<meta name='robots' content='noindex, nofollow' />

			<PageHeader
				eyebrow='Obrona pracy inżynierskiej'
				title='PhotoDrive'
				subtitle='Co musisz wiedzieć o projekcie — gotowa wypowiedź na 5 minut, liczby, decyzje, problemy i ograniczenia.'
			/>

			<nav aria-label='Spis sekcji' className='sticky top-20 z-10 bg-background/95 backdrop-blur py-4 border-b border-border'>
				<ul className='flex flex-wrap gap-x-5 gap-y-2 text-xs uppercase tracking-widest text-muted'>
					{SECTIONS.map((s) => (
						<li key={s.id}>
							<a href={`#${s.id}`} className='hover:text-accent transition-colors'>
								{s.label}
							</a>
						</li>
					))}
				</ul>
			</nav>

			<Section id='pitch' title='Pitch na 5 minut' lead='Gotowa wypowiedź — czytaj na głos z zegarkiem. Nie ucz się na pamięć, ucz się kolejności i zdań-kluczy.'>
				<ol className='space-y-4'>
					{PITCH.map((seg) => (
						<li key={seg.from} className='border-l-2 border-accent/60 bg-surface p-5 sm:p-6'>
							<p className='text-xs uppercase tracking-[0.2em] text-accent'>
								{seg.from}–{seg.to} · {seg.title}
							</p>
							<div className='mt-3 space-y-3 text-[15px] leading-relaxed text-foreground/85'>
								{seg.text.map((t) => (
									<p key={t}>{t}</p>
								))}
							</div>
						</li>
					))}
				</ol>
			</Section>

			<Section id='liczby' title='Liczby do zapamiętania'>
				<dl className='grid grid-cols-2 sm:grid-cols-3 gap-3'>
					{FACTS.map(([value, label]) => (
						<div key={label} className='border border-border bg-surface p-4'>
							<dt className='font-serif text-3xl text-accent'>{value}</dt>
							<dd className='mt-1 text-sm text-foreground/70'>{label}</dd>
						</div>
					))}
				</dl>
			</Section>

			<Section id='architektura' title='Architektura w pigułce'>
				<div className='grid gap-4 md:grid-cols-2'>
					<Card title='Backend — core/'>
						<p>Java 21, Spring Boot 3.5, Spring Security, JPA, MySQL 8. Architektura heksagonalna + DDD + lekkie CQRS.</p>
						<p>
							<strong className='text-foreground'>presentation</strong> (REST, DTO, obsługa wyjątków) →{' '}
							<strong className='text-foreground'>application</strong> (serwisy, komendy, zdarzenia, porty) →{' '}
							<strong className='text-foreground'>domain</strong> (Album, User, File — reguły) ←{' '}
							<strong className='text-foreground'>infrastructure</strong> (JPA, dysk, mail, JWT).
						</p>
						<p>Zależności wskazują do środka — domena nie zna niczego.</p>
					</Card>
					<Card title='Frontend — frontend/'>
						<p>React 19, TypeScript, Vite, Tailwind, React Router, React Query, Zustand.</p>
						<p>Jedna aplikacja SPA, trzy doświadczenia: strona publiczna (portfolio), strefa klienta i panel administratora / fotografa. Struktura feature-based.</p>
						<p>React Query trzyma dane z serwera, Zustand tylko fakt zalogowania — tokenu nie ma w JS.</p>
					</Card>
					<Card title='Infrastruktura'>
						<p>Docker (obrazy wielostopniowe, non-root), docker-compose, nginx serwuje SPA i przekazuje /api do backendu, Traefik z TLS na VPS.</p>
						<p>GitHub Actions: testy obu części → obrazy do Docker Hub → deploy SSH. Czerwony test blokuje wdrożenie.</p>
					</Card>
					<Card title='Przepływ żądania'>
						<p>Przeglądarka → Traefik (TLS) → nginx → Spring Boot: filtr limitu prób → filtr Origin → filtr JWT → kontroler → serwis → agregat → MySQL i dysk.</p>
						<p>Efekty uboczne (pliki, maile) obsługują handlery zdarzeń w odpowiedniej fazie transakcji.</p>
					</Card>
				</div>
			</Section>

			<Section id='decyzje' title='Najważniejsze decyzje' lead='Każdą umiej uzasadnić i podać jej koszt — decyzja bez kosztu brzmi na nieprzemyślaną.'>
				<div className='grid gap-4 md:grid-cols-2'>
					{DECISIONS.map((d) => (
						<Card key={d.title} title={d.title}>
							<Row label='Co'>{d.what}</Row>
							<Row label='Dlaczego'>{d.why}</Row>
							<Row label='Koszt'>{d.cost}</Row>
						</Card>
					))}
				</div>
			</Section>

			<Section id='problemy' title='Problemy, które rozwiązałem' lead='Historie na pytanie „z czym miałeś największy problem?” — opowiadaj: problem → przyczyna → rozwiązanie → wniosek.'>
				<div className='grid gap-4 md:grid-cols-2'>
					{PROBLEMS.map((p) => (
						<Card key={p.title} title={p.title}>
							<Row label='Problem'>{p.problem}</Row>
							<Row label='Rozwiązanie'>{p.fix}</Row>
							<Row label='Wniosek'>{p.lesson}</Row>
						</Card>
					))}
				</div>
			</Section>

			<Section id='ograniczenia' title='Ograniczenia — powiedz o nich sam' lead='Nazwane ograniczenie z uzasadnieniem to dowód świadomości. Ukryte, znalezione przez komisję — to minus.'>
				<div className='space-y-3'>
					{LIMITS.map((l) => (
						<Card key={l.title} title={l.title}>
							<p>{l.say}</p>
						</Card>
					))}
				</div>
			</Section>

			<Section id='pytania' title='Spodziewane pytania komisji'>
				<div className='space-y-3'>
					{QUESTIONS.map(([q, a]) => (
						<details key={q} className='group border border-border bg-surface p-5'>
							<summary className='cursor-pointer font-serif text-xl text-foreground marker:text-accent'>{q}</summary>
							<p className='mt-3 text-[15px] leading-relaxed text-foreground/80'>{a}</p>
						</details>
					))}
				</div>
			</Section>

			<Section id='demo' title='Demo w wersji 5-minutowej' lead='Pokazuj regułę, nie interfejs — każdy akt kończ jednym zdaniem, które ją nazywa. Pełny scenariusz: obsidian/22.'>
				<ol className='space-y-3'>
					{DEMO.map(([title, text], i) => (
						<li key={title} className='border border-border bg-surface p-5'>
							<p className='font-serif text-xl text-foreground'>
								<span className='text-accent mr-2'>{i + 1}.</span>
								{title}
							</p>
							<p className='mt-2 text-[15px] leading-relaxed text-foreground/80'>{text}</p>
						</li>
					))}
				</ol>
				<p className='mt-6 text-sm text-muted'>
					Nie pokazuj na żywo: limitu logowania (10 pomyłek = blokada na 15 min), usuwania po TTD (planista chodzi o północy) ani dużego uploadu.
				</p>
			</Section>
		</div>
	);
}
