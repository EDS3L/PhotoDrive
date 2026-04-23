import { useState } from 'react';
import { useAssignedUsers } from '@/hooks/use-users';
import { useUiStore } from '@/lib/stores/ui-store';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import type { User } from '@/types/user';

export function ClientListPage() {
	const { data: clients, isLoading } = useAssignedUsers();
	const openModal = useUiStore((s) => s.openModal);
	const [search, setSearch] = useState('');

	const filtered = (clients ?? []).filter(
		(c) =>
			c.name.toLowerCase().includes(search.toLowerCase()) ||
			c.email.toLowerCase().includes(search.toLowerCase()),
	);

	return (
		<div className='p-6 lg:p-10 max-w-7xl mx-auto'>
			<div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8'>
				<div>
					<p className='label text-[11px] text-primary mb-1'>KLIENCI</p>
					<h1 className='font-display text-4xl text-on-surface'>
						Lista klientów
					</h1>
				</div>
				<Button onClick={() => openModal('add-client')}>
					<span className='material-symbols-outlined text-[18px] mr-2'>
						person_add
					</span>
					Nowy klient
				</Button>
			</div>

			<Input
				placeholder='Szukaj klienta...'
				value={search}
				onChange={(e) => setSearch(e.target.value)}
				className='max-w-sm mb-8'
			/>

			{isLoading ? (
				<div className='flex items-center justify-center py-20'>
					<span className='material-symbols-outlined text-primary animate-spin text-3xl'>
						progress_activity
					</span>
				</div>
			) : filtered.length > 0 ? (
				<div className='grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4'>
					{filtered.map((client) => (
						<ClientCard key={client.id} client={client} />
					))}
				</div>
			) : (
				<div className='text-center py-20'>
					<span className='material-symbols-outlined text-5xl text-on-surface-variant/30 mb-3 block'>
						person_off
					</span>
					<p className='text-on-surface-variant text-sm'>Brak klientów</p>
				</div>
			)}
		</div>
	);
}

function ClientCard({ client }: { client: User }) {
	return (
		<div className='bg-surface-container-low p-5'>
			<div className='flex items-center gap-3 mb-2'>
				<div className='w-10 h-10 bg-surface-container flex items-center justify-center'>
					<span className='material-symbols-outlined text-on-surface-variant'>
						person
					</span>
				</div>
				<div className='min-w-0'>
					<p className='text-on-surface text-sm font-medium truncate'>
						{client.name}
					</p>
					<p className='text-on-surface-variant text-xs truncate'>
						{client.email}
					</p>
				</div>
			</div>
			<div className='flex gap-2 mt-3'>
				<Badge variant={client.isActive ? 'success' : 'muted'}>
					{client.isActive ? 'Aktywny' : 'Nieaktywny'}
				</Badge>
			</div>
		</div>
	);
}
