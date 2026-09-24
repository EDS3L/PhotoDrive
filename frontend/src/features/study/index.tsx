import { useMemo, useState } from 'react';
import { ChevronDown } from 'lucide-react';
import { PageHeader } from '@/shared/components/layout/PageHeader';
import { cn } from '@/lib/utils';
import { questions as allQuestions } from './data/questions';
import { filterQuestions } from './lib/filterQuestions';
import { groupByCommittee } from './lib/committee';
import { AnswerBlocks } from './components/AnswerBlocks';
import type { StudyQuestion } from './types';

type Order = 'numeric' | 'committee';

interface StudyPageProps {
	questions?: StudyQuestion[];
}

interface QuestionCardProps {
	question: StudyQuestion;
	isOpen: boolean;
	onToggle: () => void;
	heading: 'h2' | 'h3';
}

function QuestionCard({ question: q, isOpen, onToggle, heading: Heading }: QuestionCardProps) {
	return (
		<li id={`pytanie-${q.n}`} className='border border-border bg-surface p-5 sm:p-7'>
			<Heading className='font-serif text-2xl text-foreground leading-snug'>
				<span className='text-accent mr-2'>{q.n}.</span>
				{q.title}
			</Heading>

			<p className='mt-5 mb-2 text-xs uppercase tracking-[0.2em] text-accent'>
				Odpowiedź krótka
			</p>
			<AnswerBlocks blocks={q.short} />

			<button
				type='button'
				onClick={onToggle}
				aria-expanded={isOpen}
				className='mt-6 inline-flex items-center gap-2 text-xs uppercase tracking-[0.2em] text-muted hover:text-accent transition-colors'
			>
				{isOpen ? 'Zwiń odpowiedź rozszerzoną' : 'Odpowiedź rozszerzona'}
				<ChevronDown className={cn('w-4 h-4 transition-transform', isOpen && 'rotate-180')} />
			</button>

			{isOpen && (
				<div className='mt-4 pt-4 border-t border-border'>
					<AnswerBlocks blocks={q.long} />
				</div>
			)}
		</li>
	);
}

export default function StudyPage({ questions = allQuestions }: StudyPageProps) {
	const [query, setQuery] = useState('');
	const [order, setOrder] = useState<Order>('numeric');
	const [expanded, setExpanded] = useState<Set<number>>(new Set());

	const visible = useMemo(() => filterQuestions(questions, query), [questions, query]);
	const groups = useMemo(() => groupByCommittee(visible), [visible]);

	const toggle = (n: number) =>
		setExpanded((prev) => {
			const next = new Set(prev);
			if (next.has(n)) next.delete(n);
			else next.add(n);
			return next;
		});

	const card = (q: StudyQuestion, heading: 'h2' | 'h3') => (
		<QuestionCard
			key={q.n}
			question={q}
			isOpen={expanded.has(q.n)}
			onToggle={() => toggle(q.n)}
			heading={heading}
		/>
	);

	const orderButton = (value: Order, label: string) => (
		<button
			type='button'
			onClick={() => setOrder(value)}
			aria-pressed={order === value}
			className={cn(
				'transition-colors',
				order === value ? 'text-accent' : 'hover:text-foreground',
			)}
		>
			{label}
		</button>
	);

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
					<span className='flex items-center gap-3' role='group' aria-label='Kolejność pytań'>
						{orderButton('numeric', 'Numerycznie')}
						<span aria-hidden>/</span>
						{orderButton('committee', 'Komisyjnie')}
					</span>
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

			{order === 'numeric' ? (
				<ol className='mt-8 space-y-6'>{visible.map((q) => card(q, 'h2'))}</ol>
			) : (
				groups.map((g) => (
					<section key={g.key} aria-labelledby={`komisja-${g.key}`} className='mt-12'>
						<div className='mb-6 border-b border-accent/40 pb-3'>
							<h2 id={`komisja-${g.key}`} className='font-serif text-3xl text-foreground'>
								{g.name}
							</h2>
							<p className='mt-1 text-xs uppercase tracking-[0.2em] text-muted'>
								{g.topic} · {g.questions.length} pyt.
							</p>
						</div>
						<ol className='space-y-6'>{g.questions.map((q) => card(q, 'h3'))}</ol>
					</section>
				))
			)}
		</div>
	);
}
