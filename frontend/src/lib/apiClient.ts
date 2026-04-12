import axios from 'axios';

export const apiClient = axios.create({
	baseURL: '/api',
	withCredentials: true,
	headers: {
		'Content-Type': 'application/json',
	},
});

apiClient.interceptors.response.use(
	(response) => response,
	(error) => {
		const isAuthEndpoint = error.config?.url?.startsWith('/auth/');
		if (error.response?.status === 401 && !isAuthEndpoint) {
			const isPanel =
				window.location.pathname.startsWith('/admin') ||
				window.location.pathname.startsWith('/photographer');
			window.location.href = isPanel ? '/panel-login' : '/strefa-klienta';
		}
		return Promise.reject(error);
	},
);
