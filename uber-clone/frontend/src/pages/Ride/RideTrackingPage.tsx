import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { 
  MapPin, 
  Clock, 
  Car, 
  Star, 
  Phone, 
  MessageCircle,
  Navigation,
  X,
  Shield,
  AlertCircle,
  CheckCircle,
  User
} from 'lucide-react';
import { useRide } from '../../contexts/RideContext';
import { toast } from 'react-hot-toast';

const RideTrackingPage: React.FC = () => {
  const { rideId } = useParams<{ rideId: string }>();
  const navigate = useNavigate();
  const { state, updateRideStatus } = useRide();
  const { currentRide } = state;

  const [rideStatus, setRideStatus] = useState('searching'); // searching, assigned, arriving, arrived, started, completed
  const [driverInfo, setDriverInfo] = useState({
    name: 'John Driver',
    rating: 4.8,
    vehicle: 'Toyota Camry',
    color: 'Silver',
    licensePlate: 'ABC-123',
    phone: '+1 (555) 123-4567'
  });
  const [eta, setEta] = useState(5); // minutes
  const [currentLocation, setCurrentLocation] = useState({
    latitude: 37.7749,
    longitude: -122.4194
  });

  useEffect(() => {
    // Simulate ride status updates
    const statusUpdates = [
      { status: 'searching', delay: 0 },
      { status: 'assigned', delay: 3000 },
      { status: 'arriving', delay: 8000 },
      { status: 'arrived', delay: 15000 },
      { status: 'started', delay: 20000 },
      { status: 'completed', delay: 35000 }
    ];

    statusUpdates.forEach(({ status, delay }) => {
      setTimeout(() => {
        setRideStatus(status);
        if (status === 'assigned') {
          toast.success('Driver assigned!');
        } else if (status === 'arriving') {
          toast.success('Driver is on the way!');
        } else if (status === 'arrived') {
          toast.success('Driver has arrived!');
        } else if (status === 'started') {
          toast.success('Ride started!');
        } else if (status === 'completed') {
          toast.success('Ride completed!');
        }
      }, delay);
    });
  }, []);

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'searching':
        return <Clock className="w-6 h-6 text-yellow-500" />;
      case 'assigned':
        return <User className="w-6 h-6 text-blue-500" />;
      case 'arriving':
        return <Navigation className="w-6 h-6 text-green-500" />;
      case 'arrived':
        return <MapPin className="w-6 h-6 text-green-500" />;
      case 'started':
        return <Car className="w-6 h-6 text-blue-500" />;
      case 'completed':
        return <CheckCircle className="w-6 h-6 text-green-500" />;
      default:
        return <Clock className="w-6 h-6 text-gray-500" />;
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'searching':
        return 'Searching for drivers...';
      case 'assigned':
        return 'Driver assigned';
      case 'arriving':
        return 'Driver is on the way';
      case 'arrived':
        return 'Driver has arrived';
      case 'started':
        return 'Ride in progress';
      case 'completed':
        return 'Ride completed';
      default:
        return 'Unknown status';
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'searching':
        return 'bg-yellow-100 text-yellow-800';
      case 'assigned':
        return 'bg-blue-100 text-blue-800';
      case 'arriving':
        return 'bg-green-100 text-green-800';
      case 'arrived':
        return 'bg-green-100 text-green-800';
      case 'started':
        return 'bg-blue-100 text-blue-800';
      case 'completed':
        return 'bg-green-100 text-green-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  const handleContactDriver = () => {
    toast.success('Calling driver...');
  };

  const handleMessageDriver = () => {
    toast.success('Opening chat...');
  };

  const handleCancelRide = () => {
    if (window.confirm('Are you sure you want to cancel this ride?')) {
      toast.success('Ride cancelled');
      navigate('/dashboard');
    }
  };

  const handleEmergency = () => {
    toast.error('Emergency contact activated');
  };

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
              onClick={() => navigate('/dashboard')}
              className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <X className="w-6 h-6" />
            </button>
            <h1 className="text-xl font-semibold text-gray-900">Track Ride</h1>
            <div className="w-10" />
          </div>
        </div>
      </motion.div>

      <div className="max-w-4xl mx-auto px-4 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column - Map Placeholder */}
          <div className="lg:col-span-2">
            <motion.div
              initial={{ opacity: 0, scale: 0.95 }}
              animate={{ opacity: 1, scale: 1 }}
              className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 h-96"
            >
              <div className="h-full bg-gray-100 rounded-lg flex items-center justify-center">
                <div className="text-center text-gray-500">
                  <MapPin className="w-16 h-16 mx-auto mb-4 text-gray-300" />
                  <p className="text-lg font-medium">Map View</p>
                  <p className="text-sm">Interactive map will be displayed here</p>
                  <p className="text-xs mt-2">Google Maps integration</p>
                </div>
              </div>
            </motion.div>
          </div>

          {/* Right Column - Ride Status */}
          <div className="space-y-6">
            {/* Current Status */}
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6"
            >
              <div className="flex items-center space-x-3 mb-4">
                {getStatusIcon(rideStatus)}
                <div>
                  <h2 className="text-lg font-semibold text-gray-900">Ride Status</h2>
                  <p className="text-sm text-gray-500">Ride #{rideId}</p>
                </div>
              </div>

              <div className={`inline-flex items-center px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(rideStatus)}`}>
                {getStatusText(rideStatus)}
              </div>

              {rideStatus === 'searching' && (
                <div className="mt-4 p-4 bg-blue-50 rounded-lg">
                  <div className="flex items-center space-x-2 text-blue-800">
                    <div className="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                    <span className="text-sm">Finding the best driver for you...</span>
                  </div>
                </div>
              )}

              {rideStatus === 'assigned' && (
                <div className="mt-4 p-4 bg-green-50 rounded-lg">
                  <div className="flex items-center space-x-2 text-green-800">
                    <CheckCircle className="w-4 h-4" />
                    <span className="text-sm">Driver found! They're heading to you.</span>
                  </div>
                </div>
              )}

              {rideStatus === 'arriving' && (
                <div className="mt-4 p-4 bg-green-50 rounded-lg">
                  <div className="flex items-center space-x-2 text-green-800">
                    <Navigation className="w-4 h-4" />
                    <span className="text-sm">Driver is {eta} minutes away</span>
                  </div>
                </div>
              )}
            </motion.div>

            {/* Driver Information */}
            {rideStatus !== 'searching' && (
              <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.1 }}
                className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6"
              >
                <h3 className="text-lg font-semibold text-gray-900 mb-4">Driver Information</h3>
                
                <div className="flex items-center space-x-4 mb-4">
                  <div className="w-16 h-16 bg-blue-100 rounded-full flex items-center justify-center">
                    <User className="w-8 h-8 text-blue-600" />
                  </div>
                  <div>
                    <h4 className="font-medium text-gray-900">{driverInfo.name}</h4>
                    <div className="flex items-center space-x-1">
                      <Star className="w-4 h-4 text-yellow-500 fill-current" />
                      <span className="text-sm text-gray-600">{driverInfo.rating}</span>
                    </div>
                  </div>
                </div>

                <div className="space-y-3 mb-4">
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-500">Vehicle:</span>
                    <span className="text-gray-900">{driverInfo.vehicle}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-500">Color:</span>
                    <span className="text-gray-900">{driverInfo.color}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-500">License:</span>
                    <span className="text-gray-900">{driverInfo.licensePlate}</span>
                  </div>
                </div>

                <div className="flex space-x-2">
                  <button
                    onClick={handleContactDriver}
                    className="flex-1 bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700 transition-colors flex items-center justify-center space-x-2"
                  >
                    <Phone className="w-4 h-4" />
                    <span>Call</span>
                  </button>
                  <button
                    onClick={handleMessageDriver}
                    className="flex-1 bg-gray-600 text-white py-2 px-4 rounded-lg hover:bg-gray-700 transition-colors flex items-center justify-center space-x-2"
                  >
                    <MessageCircle className="w-4 h-4" />
                    <span>Message</span>
                  </button>
                </div>
              </motion.div>
            )}

            {/* Ride Actions */}
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
              className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6"
            >
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Ride Actions</h3>
              
              <div className="space-y-3">
                <button
                  onClick={handleCancelRide}
                  className="w-full bg-red-600 text-white py-3 px-4 rounded-lg hover:bg-red-700 transition-colors flex items-center justify-center space-x-2"
                >
                  <X className="w-4 h-4" />
                  <span>Cancel Ride</span>
                </button>

                <button
                  onClick={handleEmergency}
                  className="w-full bg-red-800 text-white py-3 px-4 rounded-lg hover:bg-red-900 transition-colors flex items-center justify-center space-x-2"
                >
                  <AlertCircle className="w-4 h-4" />
                  <span>Emergency</span>
                </button>
              </div>
            </motion.div>

            {/* Safety Features */}
            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.3 }}
              className="bg-blue-50 rounded-2xl p-4 border border-blue-200"
            >
              <div className="flex items-center space-x-3 mb-3">
                <Shield className="w-5 h-5 text-blue-600" />
                <h3 className="font-medium text-blue-900">Safety Features</h3>
              </div>
              <ul className="text-sm text-blue-800 space-y-1">
                <li>• Real-time location sharing</li>
                <li>• Driver verification</li>
                <li>• 24/7 support available</li>
                <li>• Emergency assistance</li>
              </ul>
            </motion.div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RideTrackingPage;
