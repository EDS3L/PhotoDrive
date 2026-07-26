/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import path from 'path';

export default defineConfig({
	plugins: [react(), tailwindcss()],
	resolve: {
		alias: {
			'@': path.resolve(__dirname, './src'),
		},
	},
	server: {
		proxy: {
			'/api': {
				target: 'http://localhost:8080',
				changeOrigin: true,
			},
		},
	},
	test: {
		// jsdom: hooki i komponenty dotykają DOM-u (window.setTimeout, render, zdarzenia).
		environment: 'jsdom',
		setupFiles: ['./src/test/setup.ts'],
		// Bez `globals: true` — `describe`/`it`/`expect`/`vi` importujemy jawnie.
		// Dzięki temu ESLint nie wymaga wyjątku na globalne symbole, a `tsc -b`
		// nie potrzebuje dopisywania typów testowych do konfiguracji aplikacji.
		globals: false,
		include: ['src/**/*.test.{ts,tsx}'],
		// Każdy test startuje z czystymi mockami — brak przecieków między przypadkami.
		clearMocks: true,
		restoreMocks: true,
		coverage: {
			provider: 'v8',
			reporter: ['text', 'html'],
			include: ['src/**/*.{ts,tsx}'],
			// Poza pomiarem: konfiguracja, typy, bootstrap i sam kod testowy.
			exclude: [
				'src/main.tsx',
				'src/test/**',
				'src/**/*.test.{ts,tsx}',
				'src/**/types/**',
				'src/vite-env.d.ts',
			],
			// Próg 70% — ta sama liczba co bramka backendu (JaCoCo), żeby w całym repo
			// obowiązywało jedno minimum. Realne pokrycie to 77/73/72/78 (stmts/branch/
			// funcs/lines), więc zwykły refaktor nie wywali builda, ale wyraźny regres tak.
			// Najciaśniej jest na funkcjach (72%), bo nietestowane zostają całe moduły
			// prezentacyjne (AppRoutes, Toast, Navbar, sekcje strony publicznej).
			// Egzekwuje to `npm run test:coverage` — i to ono, nie `npm run test`, jest
			// bramką w CI (odpowiednik `gradlew check` vs `gradlew test` po stronie backendu).
			thresholds: {
				statements: 70,
				branches: 70,
				functions: 70,
				lines: 70,
			},
		},
	},
});
