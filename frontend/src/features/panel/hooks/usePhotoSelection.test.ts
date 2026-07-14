import { describe, it, expect } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import { usePhotoSelection } from './usePhotoSelection';

/**
 * Zaznaczanie zdjęć steruje operacjami wsadowymi (usuwanie, widoczność, watermark),
 * więc pomyłka w zakresie = akcja na cudzych zdjęciach. Stąd nacisk na zakres z Shift.
 */
describe('usePhotoSelection', () => {
	const visible = ['a', 'b', 'c', 'd', 'e'];

	it('adds a photo on click and removes it on the next click', () => {
		// Given
		const { result } = renderHook(() => usePhotoSelection());

		// When - the same photo is clicked twice
		act(() => result.current.handleItemClick('b', visible, false));
		const afterFirst = new Set(result.current.selected);
		act(() => result.current.handleItemClick('b', visible, false));

		// Then - the first click selects, the second deselects
		expect(afterFirst).toEqual(new Set(['b']));
		expect(result.current.selected.size).toBe(0);
	});

	it('accumulates photos clicked one by one, so a batch can be built up', () => {
		// Given
		const { result } = renderHook(() => usePhotoSelection());

		// When
		act(() => result.current.handleItemClick('a', visible, false));
		act(() => result.current.handleItemClick('d', visible, false));

		// Then
		expect(result.current.selected).toEqual(new Set(['a', 'd']));
	});

	it('Shift-click selects the whole inclusive range from the anchor', () => {
		// Given - the anchor is the last photo clicked without Shift
		const { result } = renderHook(() => usePhotoSelection());
		act(() => result.current.handleItemClick('b', visible, false));

		// When
		act(() => result.current.handleItemClick('d', visible, true));

		// Then - both ends belong to the range
		expect(result.current.selected).toEqual(new Set(['b', 'c', 'd']));
	});

	it('Shift-click selects the range backwards as well, so click order does not matter', () => {
		// Given
		const { result } = renderHook(() => usePhotoSelection());
		act(() => result.current.handleItemClick('d', visible, false));

		// When - the range is drawn towards the beginning of the list
		act(() => result.current.handleItemClick('b', visible, true));

		// Then
		expect(result.current.selected).toEqual(new Set(['b', 'c', 'd']));
	});

	it('Shift-click range follows the currently visible order, so a filter never selects hidden photos', () => {
		// Given - the visibility filter hides 'b' and 'd'; only these are on screen
		const filtered = ['a', 'c', 'e'];
		const { result } = renderHook(() => usePhotoSelection());
		act(() => result.current.handleItemClick('a', filtered, false));

		// When
		act(() => result.current.handleItemClick('e', filtered, true));

		// Then - photos filtered out are NOT swept into the range
		expect(result.current.selected).toEqual(new Set(['a', 'c', 'e']));
	});

	it('Shift-click range extends the selection instead of replacing it', () => {
		// Given - a photo selected far from the upcoming range
		const { result } = renderHook(() => usePhotoSelection());
		act(() => result.current.handleItemClick('e', visible, false));
		act(() => result.current.handleItemClick('a', visible, false));

		// When - a range is drawn from the new anchor
		act(() => result.current.handleItemClick('c', visible, true));

		// Then - the earlier selection survives
		expect(result.current.selected).toEqual(new Set(['a', 'b', 'c', 'e']));
	});

	it('Shift-click without an anchor behaves like a plain click', () => {
		// Given - nothing was clicked before, so there is no anchor
		const { result } = renderHook(() => usePhotoSelection());

		// When
		act(() => result.current.handleItemClick('c', visible, true));

		// Then
		expect(result.current.selected).toEqual(new Set(['c']));
	});

	it('selects all visible photos, then clears them on the second call', () => {
		// Given
		const { result } = renderHook(() => usePhotoSelection());

		// When
		act(() => result.current.toggleAll(visible));
		const afterSelectAll = new Set(result.current.selected);
		act(() => result.current.toggleAll(visible));

		// Then - "select all" is a toggle, not a one-way action
		expect(afterSelectAll).toEqual(new Set(visible));
		expect(result.current.selected.size).toBe(0);
	});

	it('"select all" touches only the visible photos and leaves the rest selected', () => {
		// Given - 'e' is selected but filtered out of the visible list
		const filtered = ['a', 'b'];
		const { result } = renderHook(() => usePhotoSelection());
		act(() => result.current.handleItemClick('e', visible, false));

		// When - all visible are selected, then deselected
		act(() => result.current.toggleAll(filtered));
		act(() => result.current.toggleAll(filtered));

		// Then - clearing the visible list does not silently drop the hidden selection
		expect(result.current.selected).toEqual(new Set(['e']));
	});

	it('"select all" on an empty list changes nothing', () => {
		// Given - e.g. a filter that matches no photo
		const { result } = renderHook(() => usePhotoSelection());

		// When
		act(() => result.current.toggleAll([]));

		// Then
		expect(result.current.selected.size).toBe(0);
	});

	it('selectOne replaces the selection and becomes the anchor for the next range', () => {
		// Given - a stale, wider selection
		const { result } = renderHook(() => usePhotoSelection());
		act(() => result.current.toggleAll(visible));

		// When - one photo is picked, then a range is drawn from it
		act(() => result.current.selectOne('b'));
		const afterSelectOne = new Set(result.current.selected);
		act(() => result.current.handleItemClick('c', visible, true));

		// Then
		expect(afterSelectOne).toEqual(new Set(['b']));
		expect(result.current.selected).toEqual(new Set(['b', 'c']));
	});

	it('clearSelection drops both the selection and the anchor', () => {
		// Given
		const { result } = renderHook(() => usePhotoSelection());
		act(() => result.current.handleItemClick('b', visible, false));

		// When - the selection is cleared and a Shift-click follows
		act(() => result.current.clearSelection());
		act(() => result.current.handleItemClick('d', visible, true));

		// Then - the dropped anchor cannot resurrect the old range
		expect(result.current.selected).toEqual(new Set(['d']));
	});
});
