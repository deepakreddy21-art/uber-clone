import React, { useState } from 'react';
import { View, Text, TextInput, Button, StyleSheet, Alert } from 'react-native';

export default function RegisterScreen({ navigation }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [role, setRole] = useState('USER');

  const handleRegister = async () => {
    try {
      const res = await fetch('http://172.29.61.80:8080/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, name, role })
      });
      if (res.ok) {
        Alert.alert('Success', 'Registration complete');
        navigation.replace('Login');
      } else {
        const msg = await res.text();
        Alert.alert('Registration failed', msg);
      }
    } catch (e) {
      Alert.alert('Error', 'Could not connect to server');
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Register</Text>
      <TextInput placeholder="Name" value={name} onChangeText={setName} style={styles.input} />
      <TextInput placeholder="Email" value={email} onChangeText={setEmail} style={styles.input} />
      <TextInput placeholder="Password" value={password} onChangeText={setPassword} secureTextEntry style={styles.input} />
      <View style={styles.roleRow}>
        <Button title="User" onPress={() => setRole('USER')} color={role === 'USER' ? 'blue' : 'gray'} />
        <Button title="Driver" onPress={() => setRole('DRIVER')} color={role === 'DRIVER' ? 'blue' : 'gray'} />
      </View>
      <Button title="Register" onPress={handleRegister} />
      <Button title="Back to Login" onPress={() => navigation.replace('Login')} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 20 },
  title: { fontSize: 32, marginBottom: 20, textAlign: 'center' },
  input: { borderWidth: 1, borderColor: '#ccc', padding: 10, marginBottom: 10, borderRadius: 5 },
  roleRow: { flexDirection: 'row', justifyContent: 'space-around', marginBottom: 10 }
}); 