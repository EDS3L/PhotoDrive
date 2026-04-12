---
name: agent-introspection-debugging
description: Ustrukturyzowany self-debugging gdy agent się zapętla, ponawia te same tool calls bez postępu, lub dryfuje od celu. 4-fazowa pętla: Capture → Diagnose → Recover → Report.
origin: ECC
---

# Agent Introspection Debugging

Używaj tego skilla gdy sesja agenta się nie udaje w sposób powtarzalny, konsumuje tokeny bez postępu, zapętla się na tych samych narzędziach, lub dryfuje od zamierzonego zadania.

To jest **workflow skill**, nie ukryty runtime. Uczy agenta systematycznego debugowania siebie zanim eskaluje do człowieka.

## Kiedy Aktywować

- Osiągnięto maksymalny limit tool calls / pętla
- Powtarzające się próby bez postępu w przód
- Wzrost kontekstu lub drift promptu degradujący jakość outputu
- Mismatch stanu filesystem lub środowiska między oczekiwaniem a rzeczywistością
- Awarie narzędzi które są potencjalnie odwracalne po diagnozie

## Cztery Fazy

### Faza 1: Capture — Zapisz Awarię

Zanim spróbujesz odzyskać, zapisz awarię precyzyjnie.

```markdown
## Failure Capture

- Sesja / zadanie:
- Cel w toku:
- Błąd:
- Ostatni udany krok:
- Ostatnie nieudane tool / command:
- Widziany powtarzający się wzorzec:
- Założenia środowiskowe do weryfikacji:
```

### Faza 2: Diagnose — Dopasuj do Znanych Wzorców

| Wzorzec                                        | Prawdopodobna Przyczyna                                  | Sprawdź                                           |
| ---------------------------------------------- | -------------------------------------------------------- | ------------------------------------------------- |
| Max tool calls / powtarzanie tej samej komendy | pętla lub ścieżka bez wyjścia                            | sprawdź ostatnie N tool calls pod kątem powtórzeń |
| Context overflow / zdegenerowane rozumowanie   | nieograniczone notatki, powtarzane plany, oversized logi | sprawdź ostatni kontekst pod kątem duplikacji     |
| ECONNREFUSED / timeout                         | serwis niedostępny lub zły port                          | weryfikuj stan serwisu, URL i port                |
| 429 / wyczerpanie quota                        | retry storm lub brakujący backoff                        | policz powtarzające się wywołania                 |
| plik brakuje po write / stale diff             | race, zły cwd, lub branch drift                          | sprawdź path, cwd, git status                     |
| testy nadal failing po "fix"                   | zła hipoteza                                             | izoluj dokładnie failing test i re-derive bug     |

**Pytania diagnostyczne:**

- Czy to logic failure, state failure, environment failure, czy policy failure?
- Czy agent zgubił prawdziwy cel i zaczął optymalizować zły subtask?
- Czy awaria jest deterministyczna czy transient?
- Co to jest najmniejsza odwracalna akcja która by zwalidowała diagnozę?

### Faza 3: Contained Recovery — Minimalna Akcja Odzyskiwania

```markdown
## Recovery Action

- Wybrana diagnoza:
- Podjęta najmniejsza akcja:
- Dlaczego to jest bezpieczne:
- Jaki dowód udowodni że fix zadziałał:
```

**Bezpieczne akcje odzyskiwania:**

- Zatrzymaj powtarzające się retrys i przeredaguj hipotezę
- Przytnij low-signal kontekst, zachowaj tylko aktywny cel, blokery i dowody
- Re-sprawdź aktualny stan filesystem / branch / procesu
- Zawęź zadanie do jednej failing komendy, jednego pliku lub jednego testu
- Przełącz z spekulatywnego rozumowania na bezpośrednią obserwację
- Eskaluj do człowieka gdy awaria jest high-risk lub zewnętrznie zablokowana

### Faza 4: Introspection Report — Raport dla Następnego Agenta

```markdown
## Agent Self-Debug Report

- Sesja / zadanie:
- Awaria:
- Root cause:
- Akcja odzyskiwania:
- Wynik: success | partial | blocked
- Ryzyko spalania tokenów / czasu:
- Potrzebna followup:
- Zapobiegawcza zmiana do zakodowania:
```

## Heurystyki Odzysku (w Kolejności)

1. Przeredaguj prawdziwy cel w jednym zdaniu
2. Weryfikuj stan świata zamiast ufać pamięci
3. Zmniejsz failing scope
4. Uruchom jeden discriminating check
5. Dopiero wtedy ponów próbę

**Zły wzorzec:**

```
Ponów tę samą akcję trzy razy z lekko innymi sformułowaniami
```

**Dobry wzorzec:**

```
1. Zapisz awarię
2. Sklasyfikuj wzorzec
3. Uruchom jeden bezpośredni check
4. Zmień plan tylko jeśli check to wspiera
```

## Standard Outputu

Gdy ten skill jest aktywny, nie kończ wyłącznie "Naprawiłem to".

Zawsze podaj:

- wzorzec awarii
- hipotezę root-cause
- akcję odzyskiwania
- dowód że sytuacja jest teraz lepsza lub nadal zablokowana

## Integracja

- **verification-loop** — po recovery uruchom verification-loop jeśli kod był zmieniany
- **council** — gdy problem to nie technical failure ale decision ambiguity
