import { Link } from 'react-router-dom';

export function NotFoundPage() {
	return (
		<section className='min-h-[70vh] flex flex-col items-center justify-center text-center px-6 py-24'>
			<p className='text-xs uppercase tracking-[0.3em] text-accent mb-4'>
				Błąd 404
			</p>
			<h1 className='font-serif text-7xl md:text-9xl font-light text-foreground leading-none'>
				404
			</h1>
			<p className='mt-6 text-muted max-w-md'>
				Strona, której szukasz, nie istnieje lub została przeniesiona.
			</p>
			<Link
				to='/'
				className='mt-10 inline-flex items-center justify-center bg-accent text-background hover:bg-accent-hover font-medium tracking-widest uppercase transition-colors duration-300 px-8 py-4 text-sm'
			>
				Wróć na stronę główną
			</Link>
		</section>
	);
}
