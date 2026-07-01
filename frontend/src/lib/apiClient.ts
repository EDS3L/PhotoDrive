import axios from 'axios';

declare module 'axios' {
	interface AxiosRequestConfig {
		/** Gdy true — interceptor nie robi redirectu przy 401 (ciche sprawdzenie sesji). */
		skipAuthRedirect?: boolean;
	}
}

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
		const config = error.config ?? {};
		const isAuthEndpoint = config.url?.startsWith('/auth/');
		const skipRedirect = config.skipAuthRedirect === true;
		if (error.response?.status === 401 && !isAuthEndpoint && !skipRedirect) {
			const isPanel =
				window.location.pathname.startsWith('/admin') ||
				window.location.pathname.startsWith('/photographer');
			window.location.href = isPanel ? '/panel-login' : '/strefa-klienta';
		}
		return Promise.reject(error);
	},
);
