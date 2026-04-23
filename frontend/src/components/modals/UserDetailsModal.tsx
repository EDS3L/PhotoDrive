import { useState } from 'react';
import { Modal } from '@/components/ui/Modal';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { useUiStore } from '@/lib/stores/ui-store';
import {
	useAddRole,
	useRemoveRole,
	useChangeEmail,
	useChangePassword,
	useActivateUser,
	useDeactivateUser,
	useUsers,
	useAssignUsers,
	useRemoveUsers,
} from '@/hooks/use-users';
import type { User, Role } from '@/types/user';

export function UserDetailsModal() {
	const closeModal = useUiStore((s) => s.closeModal);
	const modalData = useUiStore((s) => s.modalData) as User | null;

	const addRole = useAddRole();
	const removeRole = useRemoveRole();
	const changeEmail = useChangeEmail();
	const changePassword = useChangePassword();
	const activateUser = useActivateUser();
	const deactivateUser = useDeactivateUser();
	const assignUsers = useAssignUsers();
	const removeUsers = useRemoveUsers();
	const { data: allUsers } = useUsers();

	const [newEmail, setNewEmail] = useState('');
	const [newPassword, setNewPassword] = useState('');

	if (!modalData) return null;
	const user = modalData;

	const allRoles: Role[] = ['ADMIN', 'PHOTOGRAPHER', 'CLIENT'];
	const missingRoles = allRoles.filter((r) => !user.roles.includes(r));

	const handleAddRole = (role: Role) => {
		addRole.mutate({ userId: user.id, role });
	};

	const handleRemoveRole = (role: Role) => {
		removeRole.mutate({ userId: user.id, role });
	};

	const handleChangeEmail = (e: React.FormEvent) => {
		e.preventDefault();
		if (!newEmail) return;
		changeEmail.mutate({ userId: user.id, newEmail });
		setNewEmail('');
	};

	const handleChangePassword = (e: React.FormEvent) => {
		e.preventDefault();
		if (!newPassword) return;
		changePassword.mutate({ userId: user.id, newPassword });
		setNewPassword('');
	};

	const handleToggleActive = () => {
		if (user.isActive) {
			deactivateUser.mutate(user.id);
		} else {
			activateUser.mutate(user.id);
		}
	};

	return (
		<Modal
			name='user-details'
			title={user.name}
			subtitle={user.email}
			maxWidth='max-w-xl'
		>
			<div className='space-y-6 mt-4'>
				{/* Status */}
				<div className='flex items-center justify-between'>
					<span className='text-on-surface-variant text-sm'>Status</span>
					<div className='flex items-center gap-3'>
						<Badge variant={user.isActive ? 'success' : 'muted'}>
							{user.isActive ? 'Aktywny' : 'Nieaktywny'}
						</Badge>
						<Button size='sm' variant='outline' onClick={handleToggleActive}>
							{user.isActive ? 'Dezaktywuj' : 'Aktywuj'}
						</Button>
					</div>
				</div>

				<div className='h-px bg-outline-variant/15' />

				{/* Roles */}
				<div>
					<p className='text-on-surface-variant text-sm mb-2'>Role</p>
					<div className='flex flex-wrap gap-2'>
						{user.roles.map((role) => (
							<div key={role} className='flex items-center gap-1'>
								<Badge variant='primary'>{role}</Badge>
								<button
									onClick={() => handleRemoveRole(role)}
									className='text-on-surface-variant hover:text-error transition-colors'
								>
									<span className='material-symbols-outlined text-[14px]'>
										close
									</span>
								</button>
							</div>
						))}
						{missingRoles.map((role) => (
							<button
								key={role}
								onClick={() => handleAddRole(role)}
								className='px-2 py-0.5 border border-dashed border-outline-variant/30 text-on-surface-variant text-xs hover:border-primary hover:text-primary transition-colors'
							>
								+ {role}
							</button>
						))}
					</div>
				</div>

				<div className='h-px bg-outline-variant/15' />

				{/* Change email */}
				<form onSubmit={handleChangeEmail} className='flex gap-3 items-end'>
					<Input
						label='Nowy email'
						type='email'
						value={newEmail}
						onChange={(e) => setNewEmail(e.target.value)}
						className='flex-1'
					/>
					<Button size='sm' type='submit' loading={changeEmail.isPending}>
						Zmień
					</Button>
				</form>

				{/* Change password */}
				<form onSubmit={handleChangePassword} className='flex gap-3 items-end'>
					<Input
						label='Nowe hasło'
						type='password'
						value={newPassword}
						onChange={(e) => setNewPassword(e.target.value)}
						className='flex-1'
					/>
					<Button size='sm' type='submit' loading={changePassword.isPending}>
						Zmień
					</Button>
				</form>

				{/* Assigned users */}
				{(user.roles.includes('PHOTOGRAPHER') ||
					user.roles.includes('CLIENT')) && (
					<>
						<div className='h-px bg-outline-variant/15' />
						<AssignedUsersSection
							user={user}
							allUsers={allUsers ?? []}
							onAssign={(userIdList) =>
								assignUsers.mutate({ userId: user.id, userIdList })
							}
							onRemove={(userIdList) =>
								removeUsers.mutate({ userId: user.id, userIdList })
							}
						/>
					</>
				)}

				<div className='flex justify-end pt-2'>
					<Button variant='ghost' onClick={closeModal}>
						Zamknij
					</Button>
				</div>
			</div>
		</Modal>
	);
}

function AssignedUsersSection({
	user,
	allUsers,
	onAssign,
	onRemove,
}: {
	user: User;
	allUsers: User[];
	onAssign: (userIdList: string[]) => void;
	onRemove: (userIdList: string[]) => void;
}) {
	const isPhotographer = user.roles.includes('PHOTOGRAPHER');
	const targetRole: Role = isPhotographer ? 'CLIENT' : 'PHOTOGRAPHER';
	const label = isPhotographer
		? 'Przypisani klienci'
		: 'Przypisani fotografowie';

	const assignedUsers = allUsers.filter((u) =>
		user.assignedUsers.includes(u.id),
	);
	const availableUsers = allUsers.filter(
		(u) =>
			u.id !== user.id &&
			u.roles.includes(targetRole) &&
			!user.assignedUsers.includes(u.id),
	);

	return (
		<div>
			<p className='text-on-surface-variant text-sm mb-2'>{label}</p>
			{assignedUsers.length > 0 ? (
				<div className='flex flex-col gap-1 mb-3'>
					{assignedUsers.map((u) => (
						<div
							key={u.id}
							className='flex items-center justify-between bg-surface-container-low px-3 py-2'
						>
							<span className='text-on-surface text-sm'>{u.name}</span>
							<button
								onClick={() => onRemove([u.id])}
								className='text-on-surface-variant hover:text-error transition-colors'
							>
								<span className='material-symbols-outlined text-[16px]'>
									close
								</span>
							</button>
						</div>
					))}
				</div>
			) : (
				<p className='text-on-surface-variant/60 text-xs mb-3'>
					Brak przypisań
				</p>
			)}
			{availableUsers.length > 0 && (
				<div>
					<p className='text-on-surface-variant text-xs mb-1'>Dodaj:</p>
					<div className='flex flex-wrap gap-1'>
						{availableUsers.map((u) => (
							<button
								key={u.id}
								onClick={() => onAssign([u.id])}
								className='px-2 py-0.5 border border-dashed border-outline-variant/30 text-on-surface-variant text-xs hover:border-primary hover:text-primary transition-colors'
							>
								+ {u.name}
							</button>
						))}
					</div>
				</div>
			)}
		</div>
	);
}
