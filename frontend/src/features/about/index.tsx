import { BioSection } from './components/BioSection';
import { PhilosophyCards } from './components/PhilosophyCards';
import { EquipmentSection } from './components/EquipmentSection';

export default function AboutPage() {
	return (
		<>
			<BioSection />
			<PhilosophyCards />
			<EquipmentSection />
		</>
	);
}
