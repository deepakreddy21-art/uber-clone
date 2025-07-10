import React, { useState, useEffect } from 'react';
import { View, Text, TextInput, Button, FlatList, StyleSheet, Alert, ActivityIndicator } from 'react-native';
import { getToken } from '../utils/auth';
import { StripeProvider, useStripe } from '@stripe/stripe-react-native';

export default function BookingScreen({ navigation, route }) {
  const role = route.params?.role || 'USER';
  const [pickup, setPickup] = useState('');
  const [dropoff, setDropoff] = useState('');
  const [availableRides, setAvailableRides] = useState([]);
  const [clientSecret, setClientSecret] = useState(null);
  const stripe = useStripe();
  const [loading, setLoading] = useState(false);
  const [paying, setPaying] = useState(false);

  const handleRequestRide = async () => {
    setLoading(true);
    try {
      const token = await getToken();
      const res = await fetch('http://172.29.61.80:8080/api/rides/request', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
        body: JSON.stringify({ pickupLocation: pickup, dropoffLocation: dropoff })
      });
      if (res.ok) {
        // For demo, fixed amount (e.g., $10)
        const paymentRes = await fetch('http://172.29.61.80:8080/api/payments/create-intent', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
          body: JSON.stringify({ amount: 1000 })
        });
        if (paymentRes.ok) {
          const paymentData = await paymentRes.json();
          setClientSecret(paymentData.clientSecret);
        }
        Alert.alert('Success', 'Ride requested! Please pay to confirm.');
      } else {
        const msg = await res.text();
        Alert.alert('Error', msg || 'Could not request ride');
      }
    } catch (e) {
      Alert.alert('Error', 'Could not connect to server');
    }
    setLoading(false);
  };

  const handlePay = async () => {
    setPaying(true);
    if (!clientSecret) return;
    const { error, paymentIntent } = await stripe.confirmPayment(clientSecret, {
      paymentMethodType: 'Card',
    });
    if (error) {
      Alert.alert('Payment failed', error.message);
    } else if (paymentIntent) {
      Alert.alert('Payment successful', 'Your ride is confirmed!');
      navigation.navigate('RideStatus', { role });
    }
    setPaying(false);
  };

  const fetchAvailableRides = async () => {
    try {
      const token = await getToken();
      const res = await fetch('http://172.29.61.80:8080/api/rides/available', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        const data = await res.json();
        setAvailableRides(data);
      }
    } catch {}
  };

  const handleAcceptRide = async (rideId) => {
    try {
      const token = await getToken();
      const res = await fetch(`http://172.29.61.80:8080/api/rides/accept/${rideId}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (res.ok) {
        Alert.alert('Success', 'Ride accepted!');
        navigation.navigate('RideStatus', { role });
      } else {
        Alert.alert('Error', 'Could not accept ride');
      }
    } catch (e) {
      Alert.alert('Error', 'Could not connect to server');
    }
  };

  useEffect(() => {
    if (role === 'DRIVER') fetchAvailableRides();
  }, [role]);

  return (
    <StripeProvider publishableKey="pk_test_51N...your_key_here...">
      <View style={styles.container}>
        {role === 'USER' ? (
          <>
            <Text style={styles.title}>Book a Ride</Text>
            <TextInput placeholder="Pickup Location" value={pickup} onChangeText={setPickup} style={styles.input} />
            <TextInput placeholder="Dropoff Location" value={dropoff} onChangeText={setDropoff} style={styles.input} />
            <Button title="Request Ride" onPress={handleRequestRide} />
            {loading && <ActivityIndicator size="large" color="#0000ff" style={{ margin: 10 }} />}
            {role === 'USER' && clientSecret && (
              <Button title={paying ? 'Processing...' : 'Pay for Ride'} onPress={handlePay} color="green" disabled={paying} />
            )}
          </>
        ) : (
          <>
            <Text style={styles.title}>Available Rides</Text>
            <FlatList
              data={availableRides}
              keyExtractor={item => item.id.toString()}
              renderItem={({ item }) => (
                <View style={styles.rideItem}>
                  <Text>Pickup: {item.pickupLocation}</Text>
                  <Text>Dropoff: {item.dropoffLocation}</Text>
                  <Button title="Accept Ride" onPress={() => handleAcceptRide(item.id)} />
                </View>
              )}
            />
          </>
        )}
      </View>
    </StripeProvider>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20 },
  title: { fontSize: 24, marginBottom: 20, textAlign: 'center' },
  input: { borderWidth: 1, borderColor: '#ccc', padding: 10, marginBottom: 10, borderRadius: 5 },
  rideItem: { borderWidth: 1, borderColor: '#ccc', padding: 10, marginBottom: 10, borderRadius: 5 }
}); 