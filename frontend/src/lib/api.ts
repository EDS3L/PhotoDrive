import axios from 'axios';

export const api = axios.create({
	baseURL: '/api',
	withCredentials: true,
	headers: {
		'Content-Type': 'application/json',
		Accept: 'application/json',
	},
});

api.interceptors.response.use(
	(response) => response,
	(error) => {
		if (error.response?.status === 401) {
			const path = window.location.pathname;
			const isProtected =
				path.startsWith('/admin') ||
				path.startsWith('/fotograf') ||
				path.startsWith('/klient');
			if (isProtected) {
				window.location.href = '/login';
			}
		}
		return Promise.reject(error);
	},
);
