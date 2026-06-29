import { useState } from 'react';
import {
	useUsers,
	useActivateUser,
	useDeactivateUser,
} from '@/hooks/use-users';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { useUiStore } from '@/lib/stores/ui-store';
import type { User } from '@/types/user';
import type { Role } from '@/types/user';

export function UserManagementPage() {
	const { data: users, isLoading } = useUsers();
	const activate = useActivateUser();
	const deactivate = useDeactivateUser();
	const openModal = useUiStore((s) => s.openModal);

	const [search, setSearch] = useState('');
	const [roleFilter, setRoleFilter] = useState<string>('all');

	const filtered = (users ?? []).filter((u) => {
		const matchesSearch =
			u.email.toLowerCase().includes(search.toLowerCase()) ||
			u.name.toLowerCase().includes(search.toLowerCase());
		const matchesRole =
			roleFilter === 'all' || u.roles.includes(roleFilter as Role);
		return matchesSearch && matchesRole;
	});

	return (
		<div className='p-6 lg:p-10 max-w-7xl mx-auto'>
			<div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-10'>
				<div>
					<p className='label text-[11px] text-primary mb-1'>ZARZĄDZANIE</p>
					<h1 className='font-display text-4xl text-on-surface'>Użytkownicy</h1>
				</div>
				<Button onClick={() => openModal('add-user')}>
					<span className='material-symbols-outlined text-[18px] mr-2'>
						person_add
					</span>
					Dodaj użytkownika
				</Button>
			</div>

			<div className='flex flex-col sm:flex-row gap-4 mb-8'>
				<Input
					placeholder='Szukaj po imieniu lub email...'
					value={search}
					onChange={(e) => setSearch(e.target.value)}
					className='max-w-md'
				/>
				<div className='flex gap-2'>
					{['all', 'ADMIN', 'PHOTOGRAPHER', 'CLIENT'].map((role) => (
						<button
							key={role}
							onClick={() => setRoleFilter(role)}
							className={`px-3 py-1.5 label text-[10px] transition-colors ${
								roleFilter === role
									? 'bg-primary text-on-primary'
									: 'bg-surface-container-low text-on-surface-variant hover:text-on-surface'
							}`}
						>
							{role === 'all' ? 'WSZYSCY' : role}
						</button>
					))}
				</div>
			</div>

			{isLoading ? (
				<div className='flex items-center justify-center py-20'>
					<span className='material-symbols-outlined text-primary animate-spin text-3xl'>
						progress_activity
					</span>
				</div>
			) : filtered.length > 0 ? (
				<div className='overflow-x-auto'>
					<table className='w-full'>
						<thead>
							<tr className='border-b border-outline-variant/20'>
								<th className='label text-[10px] text-on-surface-variant text-left py-3 px-4'>
									UŻYTKOWNIK
								</th>
								<th className='label text-[10px] text-on-surface-variant text-left py-3 px-4'>
									EMAIL
								</th>
								<th className='label text-[10px] text-on-surface-variant text-left py-3 px-4'>
									ROLE
								</th>
								<th className='label text-[10px] text-on-surface-variant text-left py-3 px-4'>
									STATUS
								</th>
								<th className='label text-[10px] text-on-surface-variant text-right py-3 px-4'>
									AKCJE
								</th>
							</tr>
						</thead>
						<tbody>
							{filtered.map((user) => (
								<UserRow
									key={user.id}
									user={user}
									onActivate={() => activate.mutate(user.id)}
									onDeactivate={() => deactivate.mutate(user.id)}
									onDetails={() => openModal('user-details', user)}
								/>
							))}
						</tbody>
					</table>
				</div>
			) : (
				<div className='text-center py-20'>
					<span className='material-symbols-outlined text-5xl text-on-surface-variant/30 mb-3 block'>
						group_off
					</span>
					<p className='text-on-surface-variant text-sm'>
						Brak użytkowników spełniających kryteria
					</p>
				</div>
			)}
		</div>
	);
}

function UserRow({
	user,
	onActivate,
	onDeactivate,
	onDetails,
}: {
	user: User;
	onActivate: () => void;
	onDeactivate: () => void;
	onDetails: () => void;
}) {
	return (
		<tr
			className='border-b border-outline-variant/10 hover:bg-surface-container-low/50 transition-colors cursor-pointer'
			onClick={onDetails}
		>
			<td className='py-3 px-4'>
				<div className='flex items-center gap-3'>
					<div className='w-8 h-8 bg-surface-container flex items-center justify-center text-on-surface-variant text-xs'>
						{user.name.slice(0, 2).toUpperCase()}
					</div>
					<span className='text-on-surface text-sm'>{user.name}</span>
				</div>
			</td>
			<td className='py-3 px-4 text-on-surface-variant text-sm'>
				{user.email}
			</td>
			<td className='py-3 px-4'>
				<div className='flex gap-1 flex-wrap'>
					{user.roles.map((role) => (
						<Badge
							key={role}
							variant={
								role === 'ADMIN'
									? 'error'
									: role === 'PHOTOGRAPHER'
										? 'primary'
										: 'default'
							}
						>
							{role}
						</Badge>
					))}
				</div>
			</td>
			<td className='py-3 px-4'>
				<Badge variant={user.isActive ? 'success' : 'muted'}>
					{user.isActive ? 'Aktywny' : 'Nieaktywny'}
				</Badge>
			</td>
			<td className='py-3 px-4 text-right'>
				<button
					onClick={(e) => {
						e.stopPropagation();
						if (user.isActive) {
							onDeactivate();
						} else {
							onActivate();
						}
					}}
					className='text-on-surface-variant hover:text-on-surface transition-colors p-1'
					title={user.isActive ? 'Dezaktywuj' : 'Aktywuj'}
				>
					<span className='material-symbols-outlined text-[18px]'>
						{user.isActive ? 'person_off' : 'person'}
					</span>
				</button>
			</td>
		</tr>
	);
}
