import axios from 'axios';

const TOKEN_KEY = 'govgrant_jwt_token';
const RENDER_BACKEND_URL = 'https://digital-subsidy-platform.onrender.com';

// Determine base URL dynamically
let baseURL = window.API_BASE_URL || localStorage.getItem('API_BASE_URL') || '';
if (!baseURL && window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
  if (window.location.hostname.includes('web.app') || window.location.hostname.includes('firebaseapp.com')) {
    baseURL = RENDER_BACKEND_URL;
  } else {
    baseURL = ''; // Use relative path if hosted directly inside Spring Boot
  }
}

export const apiClient = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor to handle response formatting and unwrap page arrays
apiClient.interceptors.response.use(
  (response) => {
    const body = response.data;
    if (body && Array.isArray(body.content)) {
      const arr = body.content;
      arr.page = body.number;
      arr.size = body.size;
      arr.totalElements = body.totalElements;
      arr.totalPages = body.totalPages;
      return { success: true, data: arr, raw: body, status: response.status };
    }
    return { success: true, data: body, status: response.status };
  },
  (error) => {
    let message = 'Could not reach the server. Please check your connection.';
    const status = error.response ? error.response.status : 0;
    
    if (error.response && error.response.data) {
      const d = error.response.data;
      message = d.message || d.error || (typeof d === 'string' ? d : `Request failed with status ${status}`);
      message = message.replace(/^An unexpected error occurred:\s*/i, '');
    }

    if (status === 401) {
      // Clear token and user session on 401 Unauthorized
      localStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem('govgrant_auth_user');
      sessionStorage.removeItem('govgrant_auth_user');
    }

    return { success: false, error: message, status };
  }
);

export const getToken = () => localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
export const setToken = (t, remember = true) => {
  if (remember) {
    localStorage.setItem(TOKEN_KEY, t);
  } else {
    sessionStorage.setItem(TOKEN_KEY, t);
  }
};
export const clearToken = () => {
  localStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem('govgrant_auth_user');
  sessionStorage.removeItem('govgrant_auth_user');
};

/**
 * Helper for multipart file uploads
 */
export async function uploadDocument(url, formData) {
  const token = getToken();
  return axios.post(baseURL + url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  }).then(res => ({ success: true, data: res.data }))
    .catch(err => ({
      success: false,
      error: (err.response && err.response.data && err.response.data.message) || err.message,
    }));
}

/**
 * Helper for downloading binary files (Excel, PDF, documents) with auth
 */
export async function downloadFile(url, filename) {
  const token = getToken();
  try {
    const res = await axios.get(baseURL + url, {
      responseType: 'blob',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });
    const blob = new Blob([res.data], {
      type: res.headers['content-type'] || 'application/octet-stream',
    });
    const downloadUrl = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = downloadUrl;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(downloadUrl);
    return { success: true };
  } catch (err) {
    return {
      success: false,
      error: err.response?.data?.message || err.message || 'Download failed',
    };
  }
}

