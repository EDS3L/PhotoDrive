import { useMemo, useState } from 'react';
import { ChevronDown } from 'lucide-react';
import { PageHeader } from '@/shared/components/layout/PageHeader';
import { cn } from '@/lib/utils';
import { questions as allQuestions } from './data/questions';
import { filterQuestions } from './lib/filterQuestions';
import { AnswerBlocks } from './components/AnswerBlocks';
import type { StudyQuestion } from './types';

interface StudyPageProps {
	questions?: StudyQuestion[];
}

export default function StudyPage({ questions = allQuestions }: StudyPageProps) {
	const [query, setQuery] = useState('');
	const [expanded, setExpanded] = useState<Set<number>>(new Set());

	const visible = useMemo(
		() => filterQuestions(questions, query),
		[questions, query],
	);

	const toggle = (n: number) =>
		setExpanded((prev) => {
			const next = new Set(prev);
			if (next.has(n)) next.delete(n);
			else next.add(n);
			return next;
		});

	return (
		<div className='max-w-4xl mx-auto px-4 sm:px-6 pb-24'>
			<title>Nauka — PhotoDrive</title>
			<meta name='robots' content='noindex, nofollow' />

			<PageHeader
				eyebrow='Egzamin dyplomowy'
				title='Nauka'
				subtitle={`${questions.length} pytań kierunkowych — odpowiedź krótka i rozszerzona`}
			/>

			<div className='sticky top-20 z-10 bg-background/95 backdrop-blur py-4 border-b border-border'>
				<input
					type='search'
					value={query}
					onChange={(e) => setQuery(e.target.value)}
					placeholder='Szukaj po treści albo wpisz numer pytania…'
					aria-label='Szukaj pytania'
					className='w-full bg-transparent border-b border-border py-3 text-foreground placeholder:text-muted/60 focus:border-accent focus:outline-none transition-colors'
				/>
				<div className='mt-3 flex flex-wrap items-center gap-x-6 gap-y-2 text-xs uppercase tracking-widest text-muted'>
					<span>Wyniki: {visible.length}</span>
					<button
						type='button'
						onClick={() => setExpanded(new Set(visible.map((q) => q.n)))}
						className='hover:text-accent transition-colors'
					>
						Rozwiń wszystkie
					</button>
					<button
						type='button'
						onClick={() => setExpanded(new Set())}
						className='hover:text-accent transition-colors'
					>
						Zwiń wszystkie
					</button>
				</div>
			</div>

			{visible.length === 0 && (
				<p className='mt-12 text-center text-muted'>Brak pytań pasujących do wyszukiwania.</p>
			)}

			<ol className='mt-8 space-y-6'>
				{visible.map((q) => {
					const isOpen = expanded.has(q.n);
					return (
						<li
							key={q.n}
							id={`pytanie-${q.n}`}
							className='border border-border bg-surface p-5 sm:p-7'
						>
							<h2 className='font-serif text-2xl text-foreground leading-snug'>
								<span className='text-accent mr-2'>{q.n}.</span>
								{q.title}
							</h2>

							<p className='mt-5 mb-2 text-xs uppercase tracking-[0.2em] text-accent'>
								Odpowiedź krótka
							</p>
							<AnswerBlocks blocks={q.short} />

							<button
								type='button'
								onClick={() => toggle(q.n)}
								aria-expanded={isOpen}
								className='mt-6 inline-flex items-center gap-2 text-xs uppercase tracking-[0.2em] text-muted hover:text-accent transition-colors'
							>
								{isOpen ? 'Zwiń odpowiedź rozszerzoną' : 'Odpowiedź rozszerzona'}
								<ChevronDown
									className={cn('w-4 h-4 transition-transform', isOpen && 'rotate-180')}
								/>
							</button>

							{isOpen && (
								<div className='mt-4 pt-4 border-t border-border'>
									<AnswerBlocks blocks={q.long} />
								</div>
							)}
						</li>
					);
				})}
			</ol>
		</div>
	);
}
