import { useState, useMemo } from 'react';
import {
	UserPlus,
	Camera,
	User,
	Check,
	XCircle,
	Loader2,
	Link2,
	Unlink,
} from 'lucide-react';
import { Button } from '@/shared/components/ui/Button';
import { Input } from '@/shared/components/ui/Input';
import { Modal } from '../../components/shared/Modal';
import { SearchInput } from '../../components/shared/SearchInput';
import { LoadingSpinner } from '../../components/shared/LoadingSpinner';
import { StatusBadge } from '../../components/shared/StatusBadge';
import { EmptyState } from '../../components/shared/EmptyState';
import {
	useAllUsers,
	useCreateUser,
	useActivateUser,
	useDeactivateUser,
	useAssignUsers,
	useRemoveAssignedUsers,
	usePhotographerAssignedUsers,
} from '../../hooks/useUsers';
import type { UserInfo } from '../../types/panel';

type RoleFilter = 'ALL' | 'ADMIN' | 'PHOTOGRAPHER' | 'CLIENT';
type StatusFilter = 'ALL' | 'ACTIVE' | 'INACTIVE';

const roleLabel: Record<string, string> = {
	ADMIN: 'Admin',
	PHOTOGRAPHER: 'Fotograf',
	CLIENT: 'Klient',
};

function getRoleBadgeVariant(role: string) {
	if (role === 'ADMIN') return 'error' as const;
	if (role === 'PHOTOGRAPHER') return 'accent' as const;
	return 'default' as const;
}

export default function AdminUsers() {
	const { data: users, isLoading } = useAllUsers();
	const createUserMutation = useCreateUser();
	const activateMutation = useActivateUser();
	const deactivateMutation = useDeactivateUser();
	const assignMutation = useAssignUsers();
	const removeMutation = useRemoveAssignedUsers();

	const [search, setSearch] = useState('');
	const [roleFilter, setRoleFilter] = useState<RoleFilter>('ALL');
	const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
	const [addOpen, setAddOpen] = useState(false);
	const [detailUser, setDetailUser] = useState<UserInfo | null>(null);
	const [assignOpen, setAssignOpen] = useState(false);
	const [selectedClientIds, setSelectedClientIds] = useState<string[]>([]);

	const isPhotographer = detailUser?.roles.includes('PHOTOGRAPHER') ?? false;

	const { data: assignedClients } = usePhotographerAssignedUsers(
		isPhotographer ? (detailUser?.id ?? null) : null,
	);

	const availableClients = useMemo(() => {
		if (!users || !assignedClients) return [];
		const assignedIds = new Set(assignedClients.map((c) => c.id));
		return users.filter(
			(u) =>
				u.roles.includes('CLIENT') &&
				u.isActive &&
				!assignedIds.has(u.id),
		);
	}, [users, assignedClients]);

	// New user form
	const [newName, setNewName] = useState('');
	const [newEmail, setNewEmail] = useState('');
	const [newRole, setNewRole] = useState<'PHOTOGRAPHER' | 'CLIENT'>('CLIENT');

	const filtered = useMemo(() => {
		if (!users) return [];
		return users.filter((u) => {
			const q = search.toLowerCase();
			const matchSearch =
				!q ||
				u.name.toLowerCase().includes(q) ||
				u.email.toLowerCase().includes(q);
			const matchRole = roleFilter === 'ALL' || u.roles.includes(roleFilter);
			const matchStatus =
				statusFilter === 'ALL' ||
				(statusFilter === 'ACTIVE' ? u.isActive : !u.isActive);
			return matchSearch && matchRole && matchStatus;
		});
	}, [users, search, roleFilter, statusFilter]);

	const handleCreate = () => {
		createUserMutation.mutate(
			{ name: newName, email: newEmail, role: newRole },
			{
				onSuccess: () => {
					setAddOpen(false);
					setNewName('');
					setNewEmail('');
					setNewRole('CLIENT');
				},
			},
		);
	};

	const handleToggleActive = (user: UserInfo) => {
		const id = user.id;
		if (user.isActive) {
			deactivateMutation.mutate({ id, active: false });
		} else {
			activateMutation.mutate({ id, active: true });
		}
	};

	if (isLoading) return <LoadingSpinner />;

	return (
		<div>
			{/* Header */}
			<div className='flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6'>
				<div>
					<h2 className='font-serif text-4xl font-light'>Użytkownicy</h2>
					<p className='text-sm text-muted mt-1'>
						Zarządzaj użytkownikami systemu
					</p>
				</div>
				<Button onClick={() => setAddOpen(true)}>
					<UserPlus className='w-4 h-4 mr-2' />
					Dodaj użytkownika
				</Button>
			</div>

			{/* Filters */}
			<div className='bg-surface border border-border p-4 mb-6 flex flex-col sm:flex-row gap-4 items-start sm:items-center'>
				<SearchInput
					value={search}
					onChange={setSearch}
					placeholder='Szukaj użytkownika...'
					className='w-full sm:w-64'
				/>
				<select
					value={roleFilter}
					onChange={(e) => setRoleFilter(e.target.value as RoleFilter)}
					className='bg-transparent border-b border-border py-2 text-sm text-foreground focus:border-accent focus:outline-none'
				>
					<option value='ALL' className='bg-surface'>
						Wszystkie role
					</option>
					<option value='ADMIN' className='bg-surface'>
						Admin
					</option>
					<option value='PHOTOGRAPHER' className='bg-surface'>
						Fotograf
					</option>
					<option value='CLIENT' className='bg-surface'>
						Klient
					</option>
				</select>
				<select
					value={statusFilter}
					onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
					className='bg-transparent border-b border-border py-2 text-sm text-foreground focus:border-accent focus:outline-none'
				>
					<option value='ALL' className='bg-surface'>
						Wszyscy
					</option>
					<option value='ACTIVE' className='bg-surface'>
						Aktywni
					</option>
					<option value='INACTIVE' className='bg-surface'>
						Nieaktywni
					</option>
				</select>
				<span className='text-xs text-muted ml-auto'>
					{filtered.length} użytkowników
				</span>
			</div>

			{/* Table */}
			{filtered.length === 0 ? (
				<EmptyState
					icon={<User className='w-12 h-12' />}
					title='Brak użytkowników'
					description='Nie znaleziono użytkowników spełniających kryteria'
				/>
			) : (
				<div className='overflow-x-auto'>
					<table className='w-full text-sm'>
						<thead>
							<tr className='border-b border-border text-left'>
								<th className='py-3 px-4 text-xs uppercase tracking-widest text-muted font-medium'>
									Użytkownik
								</th>
								<th className='py-3 px-4 text-xs uppercase tracking-widest text-muted font-medium'>
									Email
								</th>
								<th className='py-3 px-4 text-xs uppercase tracking-widest text-muted font-medium'>
									Rola
								</th>
								<th className='py-3 px-4 text-xs uppercase tracking-widest text-muted font-medium'>
									Status
								</th>
								<th className='py-3 px-4 text-xs uppercase tracking-widest text-muted font-medium'>
									Akcje
								</th>
							</tr>
						</thead>
						<tbody>
							{filtered.map((user) => (
								<tr
									key={user.id}
									className='border-b border-border hover:bg-surface-light/50 transition-colors cursor-pointer'
									onClick={() => setDetailUser(user)}
								>
									<td className='py-3 px-4'>
										<div className='flex items-center gap-3'>
											<div className='w-8 h-8 bg-border flex items-center justify-center'>
												<span className='text-xs text-muted'>
													{user.name
														.split(' ')
														.map((w) => w[0])
														.join('')
														.slice(0, 2)
														.toUpperCase()}
												</span>
											</div>
											<span className='text-foreground'>{user.name}</span>
										</div>
									</td>
									<td className='py-3 px-4 text-muted'>{user.email}</td>
									<td className='py-3 px-4'>
										<div className='flex gap-1'>
											{user.roles.map((r) => (
												<StatusBadge key={r} variant={getRoleBadgeVariant(r)}>
													{roleLabel[r] ?? r}
												</StatusBadge>
											))}
										</div>
									</td>
									<td className='py-3 px-4'>
										<StatusBadge variant={user.isActive ? 'success' : 'error'}>
											{user.isActive ? 'Aktywny' : 'Nieaktywny'}
										</StatusBadge>
									</td>
									<td className='py-3 px-4'>
										<button
											onClick={(e) => {
												e.stopPropagation();
												handleToggleActive(user);
											}}
											className='text-xs text-muted hover:text-foreground transition-colors'
										>
											{user.isActive ? 'Dezaktywuj' : 'Aktywuj'}
										</button>
									</td>
								</tr>
							))}
						</tbody>
					</table>
				</div>
			)}

			{/* Add User Modal */}
			<Modal
				open={addOpen}
				onClose={() => setAddOpen(false)}
				title='Nowy użytkownik'
			>
				<div className='p-8 space-y-6'>
					<Input
						id='add-name'
						label='Imię i Nazwisko'
						placeholder='Jan Kowalski'
						value={newName}
						onChange={(e) => setNewName(e.target.value)}
					/>
					<Input
						id='add-email'
						label='Email'
						type='email'
						placeholder='jan@kowalski.pl'
						value={newEmail}
						onChange={(e) => setNewEmail(e.target.value)}
					/>
					{/* Role selector */}
					<div>
						<p className='text-xs uppercase tracking-widest text-muted mb-3'>
							Rola
						</p>
						<div className='grid grid-cols-2 gap-3'>
							<button
								type='button'
								onClick={() => setNewRole('PHOTOGRAPHER')}
								className={`p-4 border text-left transition-colors ${
									newRole === 'PHOTOGRAPHER'
										? 'border-accent bg-accent/5'
										: 'border-border hover:border-muted'
								}`}
							>
								<Camera className='w-5 h-5 text-accent mb-2' />
								<p className='text-sm font-medium'>Fotograf</p>
								<p className='text-xs text-muted'>
									Zarządza klientami i albumami
								</p>
							</button>
							<button
								type='button'
								onClick={() => setNewRole('CLIENT')}
								className={`p-4 border text-left transition-colors ${
									newRole === 'CLIENT'
										? 'border-accent bg-accent/5'
										: 'border-border hover:border-muted'
								}`}
							>
								<User className='w-5 h-5 text-accent mb-2' />
								<p className='text-sm font-medium'>Klient</p>
								<p className='text-xs text-muted'>
									Przegląda i pobiera zdjęcia
								</p>
							</button>
						</div>
					</div>

					<p className='text-xs text-muted'>
						Hasło startowe zostanie wygenerowane i wysłane na podany email.
						Użytkownik ustawi własne hasło przy pierwszym logowaniu.
					</p>
				</div>
				<div className='px-8 py-6 border-t border-border flex justify-end gap-3'>
					<Button variant='ghost' onClick={() => setAddOpen(false)}>
						Anuluj
					</Button>
					<Button
						onClick={handleCreate}
						disabled={
							createUserMutation.isPending || !newName || !newEmail
						}
					>
						{createUserMutation.isPending ? (
							<Loader2 className='w-4 h-4 mr-2 animate-spin' />
						) : (
							<Check className='w-4 h-4 mr-2' />
						)}
						Utwórz konto
					</Button>
				</div>
			</Modal>

			{/* User Detail Modal */}
			<Modal
				open={!!detailUser}
				onClose={() => setDetailUser(null)}
				title={detailUser?.name ?? ''}
				maxWidth='max-w-xl'
			>
				{detailUser && (
					<>
						<div className='p-8'>
							<div className='grid grid-cols-2 gap-6 mb-6'>
								<div>
									<p className='text-xs uppercase tracking-widest text-muted mb-1'>
										Email
									</p>
									<p className='text-sm'>{detailUser.email}</p>
								</div>
								<div>
									<p className='text-xs uppercase tracking-widest text-muted mb-1'>
										Status
									</p>
									<StatusBadge
										variant={detailUser.isActive ? 'success' : 'error'}
									>
										{detailUser.isActive ? 'Aktywny' : 'Nieaktywny'}
									</StatusBadge>
								</div>
								<div>
									<p className='text-xs uppercase tracking-widest text-muted mb-1'>
										Role
									</p>
									<div className='flex gap-1'>
										{detailUser.roles.map((r) => (
											<StatusBadge key={r} variant={getRoleBadgeVariant(r)}>
												{roleLabel[r] ?? r}
											</StatusBadge>
										))}
									</div>
								</div>
								<div>
									<p className='text-xs uppercase tracking-widest text-muted mb-1'>
										Zmiana hasła
									</p>
									<p className='text-sm'>
										{detailUser.changePasswordOnNextLogin ? (
											<StatusBadge variant='warning'>Wymagana</StatusBadge>
										) : (
											<span className='text-muted'>Nie</span>
										)}
									</p>
								</div>
							</div>

							{isPhotographer && (
								<div>
									<div className='flex items-center justify-between mb-2'>
										<p className='text-xs uppercase tracking-widest text-muted'>
											Przypisani klienci ({assignedClients?.length ?? 0})
										</p>
										<Button
											variant='ghost'
											size='sm'
											onClick={() => {
												setSelectedClientIds([]);
												setAssignOpen(true);
											}}
										>
											<Link2 className='w-3 h-3 mr-1' />
											Przypisz
										</Button>
									</div>
									{assignedClients && assignedClients.length > 0 ? (
										<div className='space-y-2'>
											{assignedClients.map((client) => (
												<div
													key={client.id}
													className='flex items-center justify-between border border-border p-2'
												>
													<div>
														<p className='text-sm'>{client.name}</p>
														<p className='text-xs text-muted'>
															{client.email}
														</p>
													</div>
													<button
														onClick={() =>
															removeMutation.mutate({
																photographerId: detailUser.id,
																userIds: [client.id],
															})
														}
														className='text-xs text-muted hover:text-red-500 transition-colors'
														disabled={removeMutation.isPending}
													>
														<Unlink className='w-3.5 h-3.5' />
													</button>
												</div>
											))}
										</div>
									) : (
										<p className='text-xs text-muted'>
											Brak przypisanych klientów
										</p>
									)}
								</div>
							)}
						</div>
						<div className='px-8 py-6 border-t border-border flex gap-3'>
							<Button
								variant='outline'
								size='sm'
								onClick={() => handleToggleActive(detailUser)}
							>
								{detailUser.isActive ? (
									<>
										<XCircle className='w-4 h-4 mr-2' />
										Dezaktywuj
									</>
								) : (
									<>
										<Check className='w-4 h-4 mr-2' />
										Aktywuj
									</>
								)}
							</Button>
						</div>
					</>
				)}
			</Modal>

			{/* Assign Clients Modal */}
			<Modal
				open={assignOpen}
				onClose={() => setAssignOpen(false)}
				title='Przypisz klientów'
				maxWidth='max-w-md'
			>
				<div className='p-6'>
					{availableClients.length === 0 ? (
						<p className='text-sm text-muted'>
							Brak dostępnych klientów do przypisania
						</p>
					) : (
						<div className='space-y-2 max-h-64 overflow-y-auto'>
							{availableClients.map((client) => {
								const selected = selectedClientIds.includes(client.id);
								return (
									<label
										key={client.id}
										className={`flex items-center gap-3 p-2 border cursor-pointer transition-colors ${
											selected
												? 'border-accent bg-accent/5'
												: 'border-border hover:border-muted'
										}`}
									>
										<input
											type='checkbox'
											checked={selected}
											onChange={() =>
												setSelectedClientIds((prev) =>
													selected
														? prev.filter((id) => id !== client.id)
														: [...prev, client.id],
												)
											}
											className='accent-accent'
										/>
										<div>
											<p className='text-sm'>{client.name}</p>
											<p className='text-xs text-muted'>{client.email}</p>
										</div>
									</label>
								);
							})}
						</div>
					)}
				</div>
				<div className='px-6 py-4 border-t border-border flex justify-end gap-3'>
					<Button variant='ghost' onClick={() => setAssignOpen(false)}>
						Anuluj
					</Button>
					<Button
						disabled={
							selectedClientIds.length === 0 || assignMutation.isPending
						}
						onClick={() => {
							if (!detailUser) return;
							assignMutation.mutate(
								{
									photographerId: detailUser.id,
									userIds: selectedClientIds,
								},
								{
									onSuccess: () => {
										setAssignOpen(false);
										setSelectedClientIds([]);
									},
								},
							);
						}}
					>
						{assignMutation.isPending ? (
							<Loader2 className='w-4 h-4 mr-2 animate-spin' />
						) : (
							<Link2 className='w-4 h-4 mr-2' />
						)}
						Przypisz ({selectedClientIds.length})
					</Button>
				</div>
			</Modal>
		</div>
	);
}
