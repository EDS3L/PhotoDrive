import { BioSection } from './components/BioSection';
import { PhilosophyCards } from './components/PhilosophyCards';
import { EquipmentSection } from './components/EquipmentSection';
import { CTASection } from '@/features/home/components/CTASection';

export default function AboutPage() {
	return (
		<>
			<BioSection />
			<PhilosophyCards />
			<EquipmentSection />
			<CTASection />
		</>
	);
}
