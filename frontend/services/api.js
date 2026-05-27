import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';

// Dynamically route the request based on where the app is running
const BASE_URL = Platform.OS === 'web' 
    ? 'http://localhost:8084/api'         // Web browsers MUST use localhost
    : 'http://192.168.1.45:8084/api';     // ⚠️ REPLACE '192.168.1.45' WITH YOUR PC's ACTUAL IPv4 ADDRESS!

export const api = axios.create({
    baseURL: BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

api.interceptors.request.use(async (config) => {
    const token = await AsyncStorage.getItem('userToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});