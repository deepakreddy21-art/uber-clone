import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  MapPin, 
  Search, 
  Clock, 
  Car, 
  Star, 
  Phone, 
  MessageCircle,
  Navigation,
  X,
  Plus,
  Minus,
  CreditCard,
  Wallet,
  Shield,
  Info
} from 'lucide-react';
import { useLocation } from '../../contexts/LocationContext';
import { useRide } from '../../contexts/RideContext';
import { toast } from 'react-hot-toast';

interface VehicleOption {
  id: string;
  name: string;
  description: string;
  capacity: number;
  basePrice: number;
  pricePerKm: number;
  estimatedTime: number;
  icon: React.ComponentType<any>;
  features: string[];
}

interface FareBreakdown {
  baseFare: number;
  distanceFare: number;
  timeFare: number;
  surgeMultiplier: number;
  totalFare: number;
  estimatedDistance: number;
  estimatedTime: number;
}

const RideBookingPage: React.FC = () => {
  const navigate = useNavigate();
    const {
    state: locationState,
    setPickupLocation,
    setDropoffLocation,
    reverseGeocode
  } = useLocation();
  const { currentLocation, pickupLocation, dropoffLocation } = locationState;
  const { requestRide, state } = useRide();
  const { isLoading } = state;

  const [selectedVehicle, setSelectedVehicle] = useState<string>('');
  const [fareBreakdown, setFareBreakdown] = useState<FareBreakdown | null>(null);
  const [isCalculatingFare, setIsCalculatingFare] = useState(false);
  const [showFareBreakdown, setShowFareBreakdown] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState('card');
  const [specialInstructions, setSpecialInstructions] = useState('');
  const [passengerCount, setPassengerCount] = useState(1);
  const [isSearchingDrivers, setIsSearchingDrivers] = useState(false);

  const vehicleOptions: VehicleOption[] = [
    {
      id: 'economy',
      name: 'Economy',
      description: 'Affordable rides for everyday trips',
      capacity: 4,
      basePrice: 2.50,
      pricePerKm: 1.20,
      estimatedTime: 5,
      icon: Car,
      features: ['Air conditioning', 'Clean interior', 'Professional driver']
    },
    {
      id: 'comfort',
      name: 'Comfort',
      description: 'Spacious rides with extra comfort',
      capacity: 4,
      basePrice: 3.50,
      pricePerKm: 1.50,
      estimatedTime: 7,
      icon: Car,
      features: ['Larger vehicles', 'Premium interior', 'Priority pickup']
    },
    {
      id: 'luxury',
      name: 'Luxury',
      description: 'Premium vehicles for special occasions',
      capacity: 4,
      basePrice: 5.00,
      pricePerKm: 2.00,
      estimatedTime: 10,
      icon: Car,
      features: ['High-end vehicles', 'Professional chauffeur', 'Complimentary amenities']
    },
    {
      id: 'xl',
      name: 'XL',
      description: 'Extra large vehicles for groups',
      capacity: 6,
      basePrice: 4.00,
      pricePerKm: 1.80,
      estimatedTime: 8,
      icon: Car,
      features: ['6+ passengers', 'Luggage space', 'Group-friendly']
    }
  ];

  useEffect(() => {
    if (pickupLocation && dropoffLocation && selectedVehicle) {
      calculateFare();
    }
  }, [pickupLocation, dropoffLocation, selectedVehicle]);

  const calculateFare = async () => {
    if (!pickupLocation || !dropoffLocation || !selectedVehicle) return;

    setIsCalculatingFare(true);
    try {
      // Simulate API call for fare calculation
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      const vehicle = vehicleOptions.find(v => v.id === selectedVehicle);
      if (!vehicle) return;

      const distance = calculateDistance(
        pickupLocation.latitude,
        pickupLocation.longitude,
        dropoffLocation.latitude,
        dropoffLocation.longitude
      );

      const baseFare = vehicle.basePrice;
      const distanceFare = distance * vehicle.pricePerKm;
      const timeFare = (vehicle.estimatedTime / 60) * 0.50; // $0.50 per minute
      const surgeMultiplier = 1.0; // No surge for now
      const totalFare = (baseFare + distanceFare + timeFare) * surgeMultiplier;

      setFareBreakdown({
        baseFare,
        distanceFare,
        timeFare,
        surgeMultiplier,
        totalFare,
        estimatedDistance: distance,
        estimatedTime: vehicle.estimatedTime
      });
    } catch (error) {
      toast.error('Failed to calculate fare');
    } finally {
      setIsCalculatingFare(false);
    }
  };

  const calculateDistance = (lat1: number, lon1: number, lat2: number, lon2: number): number => {
    const R = 6371; // Earth's radius in kilometers
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = 
      Math.sin(dLat/2) * Math.sin(dLat/2) +
      Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) * 
      Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
  };

  const handleLocationSelect = async (type: 'pickup' | 'dropoff', lat: number, lng: number) => {
    try {
      const address = await reverseGeocode(lat, lng);
              const location = { latitude: lat, longitude: lng, address, timestamp: Date.now() };
      
      if (type === 'pickup') {
        setPickupLocation(location);
      } else {
        setDropoffLocation(location);
      }
    } catch (error) {
      toast.error('Failed to get address for location');
    }
  };

  const handleBookRide = async () => {
    if (!pickupLocation || !dropoffLocation || !selectedVehicle || !fareBreakdown) {
      toast.error('Please complete all required fields');
      return;
    }

    setIsSearchingDrivers(true);
    try {
      const rideRequest = {
        pickupLocation: pickupLocation,
        dropoffLocation: dropoffLocation,
        vehicleType: selectedVehicle,
        estimatedFare: fareBreakdown.totalFare,
        estimatedDistance: fareBreakdown.estimatedDistance,
        estimatedDuration: fareBreakdown.estimatedTime,
        specialInstructions,
        passengerCount,
        paymentMethod
      };

      await requestRide(rideRequest);
      toast.success('Ride requested! Searching for drivers...');
      navigate('/track-ride');
    } catch (error) {
      toast.error('Failed to request ride');
    } finally {
      setIsSearchingDrivers(false);
    }
  };

  const canBookRide = pickupLocation && dropoffLocation && selectedVehicle && fareBreakdown;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-white shadow-sm border-b"
      >
        <div className="max-w-4xl mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <button
              onClick={() => navigate(-1)}
              className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <X className="w-6 h-6" />
            </button>
            <h1 className="text-xl font-semibold text-gray-900">Book a Ride</h1>
            <div className="w-10" /> {/* Spacer for centering */}
          </div>
        </div>
      </motion.div>

      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column - Location Selection */}
          <div className="lg:col-span-2 space-y-6">
            {/* Location Inputs */}
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6"
            >
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Where to?</h2>
              
              {/* Pickup Location */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Pickup Location
                </label>
                <div className="relative">
                  <MapPin className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-blue-500" />
                  <input
                    type="text"
                    placeholder="Enter pickup location"
                    value={pickupLocation?.address || ''}
                    readOnly
                    className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                  {currentLocation && (
                    <button
                      onClick={() => handleLocationSelect('pickup', currentLocation.latitude, currentLocation.longitude)}
                      className="absolute right-3 top-1/2 transform -translate-y-1/2 text-blue-600 hover:text-blue-700 text-sm font-medium"
                    >
                      Use Current
                    </button>
                  )}
                </div>
              </div>

              {/* Dropoff Location */}
              <div className="mb-4">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Dropoff Location
                </label>
                <div className="relative">
                  <MapPin className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-red-500" />
                  <input
                    type="text"
                    placeholder="Where to?"
                    value={dropoffLocation?.address || ''}
                    readOnly
                    className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  />
                </div>
              </div>

              {/* Quick Location Buttons */}
              <div className="flex flex-wrap gap-2">
                {['Home', 'Work', 'Airport', 'Mall', 'Hospital'].map((location) => (
                  <button
                    key={location}
                    className="px-3 py-1 bg-gray-100 text-gray-700 rounded-full text-sm hover:bg-gray-200 transition-colors"
                  >
                    {location}
                  </button>
                ))}
              </div>
            </motion.div>

            {/* Vehicle Selection */}
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
              className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6"
            >
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Choose Your Ride</h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {vehicleOptions.map((vehicle) => (
                  <motion.button
                    key={vehicle.id}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => setSelectedVehicle(vehicle.id)}
                    className={`p-4 rounded-xl border-2 transition-all ${
                      selectedVehicle === vehicle.id
                        ? 'border-blue-500 bg-blue-50'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <div className="flex items-start justify-between mb-3">
                      <div className="flex items-center space-x-3">
                        <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${
                          selectedVehicle === vehicle.id ? 'bg-blue-500' : 'bg-gray-100'
                        }`}>
                          <vehicle.icon className={`w-5 h-5 ${
                            selectedVehicle === vehicle.id ? 'text-white' : 'text-gray-600'
                          }`} />
                        </div>
                        <div>
                          <h3 className="font-medium text-gray-900">{vehicle.name}</h3>
                          <p className="text-sm text-gray-500">{vehicle.description}</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <div className="text-lg font-semibold text-gray-900">
                          ${vehicle.basePrice.toFixed(2)}
                        </div>
                        <div className="text-sm text-gray-500">base fare</div>
                      </div>
                    </div>
                    
                    <div className="flex items-center justify-between text-sm text-gray-600 mb-3">
                      <span>Capacity: {vehicle.capacity} people</span>
                      <span>~{vehicle.estimatedTime} min</span>
                    </div>
                    
                    <div className="flex flex-wrap gap-1">
                      {vehicle.features.map((feature, index) => (
                        <span
                          key={index}
                          className="px-2 py-1 bg-gray-100 text-gray-600 rounded text-xs"
                        >
                          {feature}
                        </span>
                      ))}
                    </div>
                  </motion.button>
                ))}
              </div>
            </motion.div>

            {/* Additional Options */}
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
              className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6"
            >
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Additional Options</h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Passenger Count */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Number of Passengers
                  </label>
                  <div className="flex items-center space-x-3">
                    <button
                      onClick={() => setPassengerCount(Math.max(1, passengerCount - 1))}
                      className="w-8 h-8 rounded-full border border-gray-300 flex items-center justify-center hover:bg-gray-50"
                    >
                      <Minus className="w-4 h-4" />
                    </button>
                    <span className="text-lg font-medium w-8 text-center">{passengerCount}</span>
                    <button
                      onClick={() => setPassengerCount(passengerCount + 1)}
                      className="w-8 h-8 rounded-full border border-gray-300 flex items-center justify-center hover:bg-gray-50"
                    >
                      <Plus className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {/* Payment Method */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Payment Method
                  </label>
                  <select
                    value={paymentMethod}
                    onChange={(e) => setPaymentMethod(e.target.value)}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option value="card">Credit/Debit Card</option>
                    <option value="wallet">Digital Wallet</option>
                    <option value="cash">Cash</option>
                  </select>
                </div>
              </div>

              {/* Special Instructions */}
              <div className="mt-6">
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Special Instructions (Optional)
                </label>
                <textarea
                  value={specialInstructions}
                  onChange={(e) => setSpecialInstructions(e.target.value)}
                  placeholder="Any special requests for your driver?"
                  rows={3}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                />
              </div>
            </motion.div>
          </div>

          {/* Right Column - Fare & Booking */}
          <div className="space-y-6">
            {/* Fare Estimate */}
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 sticky top-8"
            >
              <h2 className="text-lg font-semibold text-gray-900 mb-4">Fare Estimate</h2>
              
              {!canBookRide ? (
                <div className="text-center py-8 text-gray-500">
                  <MapPin className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                  <p>Select pickup and dropoff locations to see fare estimate</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {isCalculatingFare ? (
                    <div className="text-center py-4">
                      <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-2"></div>
                      <p className="text-sm text-gray-500">Calculating fare...</p>
                    </div>
                  ) : (
                    <>
                      <div className="text-center">
                        <div className="text-3xl font-bold text-gray-900">
                          ${fareBreakdown?.totalFare.toFixed(2)}
                        </div>
                        <p className="text-sm text-gray-500">Estimated total</p>
                      </div>

                      <div className="border-t pt-4">
                        <div className="flex justify-between text-sm mb-2">
                          <span>Base fare</span>
                          <span>${fareBreakdown?.baseFare.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between text-sm mb-2">
                          <span>Distance ({fareBreakdown?.estimatedDistance.toFixed(1)} km)</span>
                          <span>${fareBreakdown?.distanceFare.toFixed(2)}</span>
                        </div>
                        <div className="flex justify-between text-sm mb-2">
                          <span>Time ({fareBreakdown?.estimatedTime} min)</span>
                          <span>${fareBreakdown?.timeFare.toFixed(2)}</span>
                        </div>
                        {fareBreakdown?.surgeMultiplier !== 1 && (
                          <div className="flex justify-between text-sm mb-2">
                            <span>Surge pricing</span>
                            <span>×{fareBreakdown.surgeMultiplier.toFixed(1)}</span>
                          </div>
                        )}
                      </div>

                      <button
                        onClick={() => setShowFareBreakdown(!showFareBreakdown)}
                        className="w-full text-blue-600 hover:text-blue-700 text-sm font-medium"
                      >
                        {showFareBreakdown ? 'Hide' : 'Show'} detailed breakdown
                      </button>
                    </>
                  )}
                </div>
              )}
            </motion.div>

            {/* Book Ride Button */}
            <motion.button
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              onClick={handleBookRide}
              disabled={!canBookRide || isLoading || isSearchingDrivers}
              className="w-full bg-gradient-to-r from-blue-500 to-purple-600 text-white py-4 px-6 rounded-2xl font-semibold text-lg hover:from-blue-600 hover:to-purple-700 focus:ring-4 focus:ring-blue-200 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isSearchingDrivers ? (
                <div className="flex items-center justify-center">
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin mr-2"></div>
                  Searching for drivers...
                </div>
              ) : (
                'Book Ride'
              )}
            </motion.button>

            {/* Safety Features */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.4 }}
              className="bg-blue-50 rounded-2xl p-4 border border-blue-200"
            >
              <div className="flex items-center space-x-3 mb-3">
                <Shield className="w-5 h-5 text-blue-600" />
                <h3 className="font-medium text-blue-900">Safety Features</h3>
              </div>
              <ul className="text-sm text-blue-800 space-y-1">
                <li>• Real-time ride tracking</li>
                <li>• Driver verification</li>
                <li>• 24/7 support</li>
                <li>• Emergency assistance</li>
              </ul>
            </motion.div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RideBookingPage;
