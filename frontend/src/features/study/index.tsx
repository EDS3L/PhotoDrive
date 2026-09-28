import { useMemo, useState } from 'react';
import { Check, ChevronDown } from 'lucide-react';
import { PageHeader } from '@/shared/components/layout/PageHeader';
import { cn } from '@/lib/utils';
import { questions as allQuestions } from './data/questions';
import { filterQuestions } from './lib/filterQuestions';
import { groupByCommittee } from './lib/committee';
import { AnswerBlocks } from './components/AnswerBlocks';
import { useLearned } from './lib/useLearned';
import type { StudyQuestion } from './types';

type Order = 'numeric' | 'committee';

interface StudyPageProps {
	questions?: StudyQuestion[];
}

interface QuestionCardProps {
	question: StudyQuestion;
	isOpen: boolean;
	onToggle: () => void;
	isLearned: boolean;
	onToggleLearned: () => void;
	heading: 'h2' | 'h3';
}

function QuestionCard({
	question: q,
	isOpen,
	onToggle,
	isLearned,
	onToggleLearned,
	heading: Heading,
}: QuestionCardProps) {
	return (
		<li
			id={`pytanie-${q.n}`}
			className={cn(
				'border bg-surface p-5 sm:p-7',
				isLearned ? 'border-accent/40' : 'border-border',
			)}
		>
			<div className='flex items-start justify-between gap-4'>
				<Heading
					className={cn(
						'font-serif text-2xl leading-snug',
						isLearned ? 'text-muted' : 'text-foreground',
					)}
				>
					<span className='text-accent mr-2'>{q.n}.</span>
					{q.title}
				</Heading>
				<button
					type='button'
					onClick={onToggleLearned}
					aria-pressed={isLearned}
					aria-label={`Pytanie ${q.n} nauczone`}
					className={cn(
						'shrink-0 inline-flex items-center gap-2 border px-3 py-1.5 text-xs uppercase tracking-[0.15em] transition-colors',
						isLearned
							? 'border-accent bg-accent/10 text-accent'
							: 'border-border text-muted hover:border-accent hover:text-accent',
					)}
				>
					<Check className={cn('w-3.5 h-3.5', !isLearned && 'opacity-30')} />
					{isLearned ? 'Nauczone' : 'Nienauczone'}
				</button>
			</div>

			{isLearned ? (
				<>
					<p className='mt-5 mb-2 text-xs uppercase tracking-[0.2em] text-accent'>
						Skróty myślowe
					</p>
					<ul className='space-y-1.5' aria-label={`Skróty myślowe do pytania ${q.n}`}>
						{q.hints.map((hint) => (
							<li key={hint} className='flex gap-3 text-muted leading-relaxed'>
								<span aria-hidden className='text-accent'>
									·
								</span>
								{hint}
							</li>
						))}
					</ul>
				</>
			) : (
				<>
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
				</>
			)}
		</li>
	);
}

export default function StudyPage({ questions = allQuestions }: StudyPageProps) {
	const [query, setQuery] = useState('');
	const [order, setOrder] = useState<Order>('numeric');
	const [expanded, setExpanded] = useState<Set<number>>(new Set());
	const { learned, toggleLearned } = useLearned();

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
			isLearned={learned.has(q.n)}
			onToggleLearned={() => toggleLearned(q.n)}
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
					<span>
						Nauczone: {questions.filter((q) => learned.has(q.n)).length}/{questions.length}
					</span>
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
