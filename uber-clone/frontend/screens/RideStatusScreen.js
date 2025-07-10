import React, { useEffect, useState } from 'react';
import { View, Text, FlatList, StyleSheet } from 'react-native';
import { getToken } from '../utils/auth';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

export default function RideStatusScreen({ route }) {
  const role = route.params?.role || 'USER';
  const [rides, setRides] = useState([]);

  const fetchMyRides = async () => {
    try {
      const token = await getToken();
      const res = await fetch('http://172.29.61.80:8080/api/rides/my', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        setRides(data);
      }
    } catch {}
  };

  useEffect(() => {
    fetchMyRides();
    // WebSocket for real-time updates
    const token = route.params?.token;
    let stompClient;
    if (rides.length > 0 && token) {
      const rideId = rides[0].id; // For demo, subscribe to first ride
      stompClient = new Client({
        webSocketFactory: () => new SockJS('http://172.29.61.80:8080/ws'),
        connectHeaders: { Authorization: `Bearer ${token}` },
        onConnect: () => {
          stompClient.subscribe(`/topic/ride-status/${rideId}`, (msg) => {
            fetchMyRides(); // Refresh rides on update
          });
        },
      });
      stompClient.activate();
    }
    return () => {
      if (stompClient) stompClient.deactivate();
    };
  }, [rides.length, route.params?.token]);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>My Rides ({role})</Text>
      <FlatList
        data={rides}
        keyExtractor={item => item.id.toString()}
        renderItem={({ item }) => (
          <View style={styles.rideItem}>
            <Text>Pickup: {item.pickupLocation}</Text>
            <Text>Dropoff: {item.dropoffLocation}</Text>
            <Text>Status: {item.status}</Text>
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20 },
  title: { fontSize: 24, marginBottom: 20, textAlign: 'center' },
  rideItem: { borderWidth: 1, borderColor: '#ccc', padding: 10, marginBottom: 10, borderRadius: 5 }
}); 