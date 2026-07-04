import { useCallback, useRef, useState } from 'react';

/**
 * Wspólna logika zaznaczania zdjęć w gridzie (panel admina i fotografa).
 * Obsługuje: pojedyncze przełączanie, zakres z Shift (od kotwicy do klikniętego)
 * oraz „zaznacz / odznacz wszystkie" w obrębie aktualnie widocznej listy.
 *
 * Kolejność do wyznaczania zakresu podajemy przy kliknięciu (`orderedIds`),
 * bo zależy od aktywnego filtra widoczności — hook nie musi jej znać z góry.
 */
export function usePhotoSelection() {
	const [selected, setSelected] = useState<Set<string>>(new Set());
	// Kotwica zakresu (ostatnio kliknięte zdjęcie) — ref, bo nie wpływa na render.
	const anchorRef = useRef<string | null>(null);

	const clearSelection = useCallback(() => {
		setSelected(new Set());
		anchorRef.current = null;
	}, []);

	const selectOne = useCallback((id: string) => {
		setSelected(new Set([id]));
		anchorRef.current = id;
	}, []);

	const handleItemClick = useCallback(
		(id: string, orderedIds: string[], shiftKey: boolean) => {
			// Kotwicę odczytujemy PRZED setSelected i domykamy w closure — updater
			// Reacta może być wywołany asynchronicznie, a ref jest już nadpisany.
			const anchor = anchorRef.current;
			anchorRef.current = id;
			setSelected((prev) => {
				if (shiftKey && anchor && anchor !== id) {
					const from = orderedIds.indexOf(anchor);
					const to = orderedIds.indexOf(id);
					if (from !== -1 && to !== -1) {
						const [lo, hi] = from < to ? [from, to] : [to, from];
						const next = new Set(prev);
						for (let i = lo; i <= hi; i++) next.add(orderedIds[i]);
						return next;
					}
				}
				const next = new Set(prev);
				if (next.has(id)) next.delete(id);
				else next.add(id);
				return next;
			});
		},
		[],
	);

	const toggleAll = useCallback((orderedIds: string[]) => {
		setSelected((prev) => {
			const allSelected =
				orderedIds.length > 0 && orderedIds.every((id) => prev.has(id));
			const next = new Set(prev);
			if (allSelected) orderedIds.forEach((id) => next.delete(id));
			else orderedIds.forEach((id) => next.add(id));
			return next;
		});
	}, []);

	return {
		selected,
		setSelected,
		clearSelection,
		selectOne,
		handleItemClick,
		toggleAll,
	};
}
