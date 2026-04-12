import { Sparkles, Sun, Users } from 'lucide-react';

const cards = [
	{
		Icon: Sparkles,
		title: 'Autentyczność',
		description:
			'Nie reżyseruję — obserwuję i uwieczniam prawdziwe emocje. Każde zdjęcie jest odzwierciedleniem tego, kim jesteście.',
	},
	{
		Icon: Sun,
		title: 'Światło i kadr',
		description:
			'Pracuję ze światłem naturalnym, szukając złotych godzin i ciekawych cieni. Kompozycja to mój język wizualny.',
	},
	{
		Icon: Users,
		title: 'Relacja z klientem',
		description:
			'Zależy mi, żebyście czuli się komfortowo przed obiektywem. Dobre zdjęcia powstają w atmosferze zaufania.',
	},
];

export function PhilosophyCards() {
	return (
		<section className='bg-surface py-24'>
			<div className='max-w-7xl mx-auto px-6'>
				<p className='text-xs uppercase tracking-[0.3em] text-accent mb-4 text-center'>
					Podejście
				</p>
				<h2 className='font-serif text-4xl md:text-5xl font-light text-center mb-16'>
					Filozofia pracy
				</h2>

				<div className='grid grid-cols-1 md:grid-cols-3 gap-8'>
					{cards.map(({ Icon, title, description }) => (
						<div
							key={title}
							className='border border-border p-8 hover:border-accent/30 transition-colors duration-300'
						>
							<Icon className='w-8 h-8 text-accent mb-6' />
							<h3 className='font-serif text-xl mb-4'>{title}</h3>
							<p className='text-sm text-muted leading-relaxed'>
								{description}
							</p>
						</div>
					))}
				</div>
			</div>
		</section>
	);
}
