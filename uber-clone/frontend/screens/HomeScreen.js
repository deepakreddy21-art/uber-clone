import React from 'react';
import { View, Text, Button, StyleSheet, Alert } from 'react-native';
import { removeToken } from '../utils/auth';

function parseJwt(token) {
  try {
    return JSON.parse(atob(token.split('.')[1]));
  } catch {
    return {};
  }
}

export default function HomeScreen({ navigation, route }) {
  const token = route.params?.token;
  let role = 'USER';
  if (token) {
    const payload = parseJwt(token);
    if (payload && payload.role) role = payload.role;
  } else {
    role = route.params?.role || 'USER';
  }

  const handleLogout = async () => {
    await removeToken();
    navigation.replace('Login');
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Welcome to Uber Clone</Text>
      {role === 'USER' ? (
        <>
          <Button title="Book a Ride" onPress={() => navigation.navigate('Booking', { role, token })} />
          <Button title="My Rides" onPress={() => navigation.navigate('RideStatus', { role, token })} />
        </>
      ) : (
        <>
          <Button title="Available Rides" onPress={() => navigation.navigate('Booking', { role, token })} />
          <Button title="My Rides" onPress={() => navigation.navigate('RideStatus', { role, token })} />
        </>
      )}
      <Button title="Logout" onPress={handleLogout} color="red" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 20 },
  title: { fontSize: 28, marginBottom: 30, textAlign: 'center' }
}); 