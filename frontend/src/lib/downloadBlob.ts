/**
 * Uruchamia pobranie bloba przez przeglądarkę za pomocą tymczasowego `<a download>`.
 *
 * Jedno miejsce dla logiki, która wcześniej istniała w DWÓCH rozjechanych kopiach (F.3):
 * strefa klienta montowała `<a>` w warstwie API (i nazywała plik UUID-em albumu), a panel
 * robił to w komponencie — z innym sprzątaniem i inną nazwą pliku. Teraz obie warstwy API
 * zwracają `Blob`, a pobranie odpala ten helper z czytelną nazwą.
 */
export function triggerBlobDownload(blob: Blob, filename: string): void {
	const url = URL.createObjectURL(blob);
	const anchor = document.createElement('a');
	anchor.href = url;
	anchor.download = filename;
	document.body.appendChild(anchor);
	anchor.click();
	anchor.remove();
	// Revoke dopiero w kolejnym ticku — natychmiastowy revoke potrafi przerwać pobranie,
	// zanim przeglądarka zdąży sięgnąć po zawartość bloba.
	setTimeout(() => URL.revokeObjectURL(url), 0);
}
