import AsyncStorage from '@react-native-async-storage/async-storage';

export const storeToken = async (token) => {
  try {
    await AsyncStorage.setItem('jwt_token', token);
  } catch (e) {}
};

export const getToken = async () => {
  try {
    return await AsyncStorage.getItem('jwt_token');
  } catch (e) {
    return null;
  }
};

export const removeToken = async () => {
  try {
    await AsyncStorage.removeItem('jwt_token');
  } catch (e) {}
}; 