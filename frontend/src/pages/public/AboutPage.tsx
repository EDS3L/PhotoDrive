import { Link } from 'react-router';
import { Button } from '@/components/ui/Button';
import { PhotoPlaceholder } from '@/components/ui/PhotoPlaceholder';

const PHILOSOPHY = [
	{
		icon: 'favorite',
		title: 'Autentyczność',
		desc: 'Nie inscenizuję — rejestruję prawdziwe emocje i momenty, które naprawdę się wydarzyły.',
	},
	{
		icon: 'wb_sunny',
		title: 'Światło',
		desc: 'Naturalne światło to mój główny narzędzie. Każdy kadr to dialog między cieniem a blaskiem.',
	},
	{
		icon: 'handshake',
		title: 'Relacja',
		desc: 'Zanim sięgnę po aparat, poznaję Twoją historię. Zaufanie jest fundamentem dobrego zdjęcia.',
	},
];

const STATS = [
	{ value: '8+', label: 'Lat doświadczenia' },
	{ value: '500+', label: 'Zrealizowanych sesji' },
	{ value: '1500+', label: 'Dostarczonych zdjęć' },
];

export function AboutPage() {
	return (
		<>
			{/* Hero */}
			<section className='pt-32 pb-24 lg:py-40'>
				<div className='max-w-7xl mx-auto px-6 lg:px-12 grid grid-cols-1 lg:grid-cols-12 gap-12 items-center'>
					<div className='lg:col-span-5 aspect-[2/3] overflow-hidden'>
						<PhotoPlaceholder
							label='Portret fotografki'
							className='w-full h-full'
						/>
					</div>
					<div className='lg:col-span-7'>
						<p className='label text-[11px] text-primary mb-4'>O MNIE</p>
						<h1 className='font-display italic text-5xl lg:text-6xl text-on-surface mb-6 leading-[1]'>
							Każdy kadr to historia
						</h1>
						<p className='text-on-surface-variant text-sm leading-relaxed mb-4'>
							Nazywam się Anna i od ponad 8 lat zajmuję się profesjonalną
							fotografią. Moja przygoda z aparatem zaczęła się od dokumentowania
							podróży, a z czasem przekształciła w pasję do fotografii ślubnej i
							portretowej.
						</p>
						<p className='text-on-surface-variant text-sm leading-relaxed mb-8'>
							Wierzę, że najpiękniejsze zdjęcia rodzą się z autentycznych
							emocji. Dlatego zawsze staram się stworzyć atmosferę, w której moi
							klienci czują się swobodnie i naturalnie.
						</p>

						{/* Stats */}
						<div className='grid grid-cols-3 gap-6'>
							{STATS.map((s) => (
								<div key={s.label}>
									<p className='font-display text-4xl text-primary'>
										{s.value}
									</p>
									<p className='label text-[10px] text-on-surface-variant mt-1'>
										{s.label}
									</p>
								</div>
							))}
						</div>
					</div>
				</div>
			</section>

			{/* Philosophy */}
			<section className='py-24 bg-surface-container-low'>
				<div className='max-w-7xl mx-auto px-6 lg:px-12'>
					<h2 className='font-display text-4xl text-on-surface text-center mb-16'>
						Moja filozofia
					</h2>
					<div className='grid grid-cols-1 md:grid-cols-3 gap-8'>
						{PHILOSOPHY.map((item) => (
							<div
								key={item.title}
								className='p-8 hover:bg-surface-container transition-colors duration-300 group'
							>
								<span className='material-symbols-outlined text-3xl text-primary mb-4 block group-hover:scale-110 transition-transform'>
									{item.icon}
								</span>
								<h3 className='font-display text-2xl text-on-surface mb-3'>
									{item.title}
								</h3>
								<p className='text-on-surface-variant text-sm leading-relaxed'>
									{item.desc}
								</p>
							</div>
						))}
					</div>
				</div>
			</section>

			{/* Equipment */}
			<section className='py-24 relative overflow-hidden'>
				<div className='absolute inset-0 bg-surface-dim' />
				<div className='absolute inset-0 bg-background/60' />
				<div className='relative max-w-3xl mx-auto px-6 text-center'>
					<p className='label text-[11px] text-primary mb-4'>SPRZĘT</p>
					<h2 className='font-display text-4xl text-on-surface mb-4'>
						Narzędzia w służbie historii
					</h2>
					<p className='text-on-surface-variant text-sm leading-relaxed'>
						Pracuję na profesjonalnym sprzęcie Canon i Sony, korzystając z
						obiektywów stałoogniskowych, które pozwalają oddać głębię i
						naturalne piękno każdej sceny.
					</p>
				</div>
			</section>

			{/* CTA */}
			<section className='py-24'>
				<div className='max-w-3xl mx-auto px-6 text-center'>
					<h2 className='font-display text-4xl text-on-surface mb-6'>
						Chcesz współpracować?
					</h2>
					<div className='flex flex-col sm:flex-row items-center justify-center gap-4'>
						<Link to='/kontakt'>
							<Button size='lg'>SKONTAKTUJ SIĘ</Button>
						</Link>
						<Link to='/portfolio'>
							<Button variant='outline' size='lg'>
								ZOBACZ PORTFOLIO
							</Button>
						</Link>
					</div>
				</div>
			</section>
		</>
	);
}
