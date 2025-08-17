import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  MapPin, 
  Clock, 
  Star, 
  Car, 
  Navigation, 
  Phone, 
  Settings, 
  LogOut,
  Plus,
  Search,
  Filter,
  Calendar,
  TrendingUp,
  DollarSign,
  Users,
  Shield
} from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useLocation } from '../../contexts/LocationContext';
import { useRide } from '../../contexts/RideContext';
import { toast } from 'react-hot-toast';
import { useNavigate } from 'react-router-dom';

const DashboardPage: React.FC = () => {
  const { state, logout } = useAuth();
  const { user } = state;
  const { state: locationState } = useLocation();
  const { currentLocation, pickupLocation, dropoffLocation } = locationState;
  const { state: rideState } = useRide();
  const { currentRide, rideHistory, isLoading } = rideState;
  const navigate = useNavigate();
  
  const [activeTab, setActiveTab] = useState('overview');
  const [quickActions, setQuickActions] = useState([
    { id: 1, title: 'Book a Ride', icon: Car, color: 'blue', action: 'book' },
    { id: 2, title: 'Track Ride', icon: Navigation, color: 'green', action: 'track' },
    { id: 3, title: 'Call Support', icon: Phone, color: 'purple', action: 'support' },
    { id: 4, title: 'Settings', icon: Settings, color: 'gray', action: 'settings' }
  ]);

  const [recentRides] = useState([
    { id: 1, from: 'Home', to: 'Office', date: '2024-01-15', status: 'completed', rating: 5 },
    { id: 2, from: 'Mall', to: 'Airport', date: '2024-01-14', status: 'completed', rating: 4 },
    { id: 3, from: 'Restaurant', to: 'Home', date: '2024-01-13', status: 'cancelled', rating: null }
  ]);

  const [stats] = useState({
    totalRides: 47,
    completedRides: 45,
    cancelledRides: 2,
    averageRating: 4.8,
    totalSpent: 234.50,
    thisMonth: 67.30
  });

  const handleQuickAction = (action: string) => {
    switch (action) {
      case 'book':
        // Navigate to booking page
        navigate('/book-ride');
        break;
      case 'track':
        // Navigate to tracking page
        if (currentRide) {
          navigate(`/track-ride/${currentRide.id}`);
        } else {
          toast.error('No active ride to track');
        }
        break;
      case 'support':
        // Open support chat/call
        toast.success('Opening support...');
        break;
      case 'settings':
        // Navigate to settings
        toast.success('Redirecting to settings...');
        break;
      default:
        break;
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
      toast.success('Logged out successfully');
    } catch (error) {
      toast.error('Logout failed');
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'completed':
        return 'text-green-600 bg-green-100';
      case 'cancelled':
        return 'text-red-600 bg-red-100';
      case 'in_progress':
        return 'text-blue-600 bg-blue-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  };

  const getStatusText = (status: string) => {
    return status.charAt(0).toUpperCase() + status.slice(1).replace('_', ' ');
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-white shadow-sm border-b"
      >
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-4">
            <div className="flex items-center space-x-4">
              <div className="w-10 h-10 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full flex items-center justify-center">
                <Car className="w-6 h-6 text-white" />
              </div>
              <div>
                <h1 className="text-xl font-semibold text-gray-900">Uber Clone</h1>
                <p className="text-sm text-gray-500">Welcome back, {user?.firstName}!</p>
              </div>
            </div>
            
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2 text-sm text-gray-600">
                <MapPin className="w-4 h-4" />
                <span>{currentLocation ? 'Location Active' : 'Location Off'}</span>
              </div>
              <button
                onClick={handleLogout}
                className="flex items-center space-x-2 px-3 py-2 text-gray-600 hover:text-gray-900 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <LogOut className="w-4 h-4" />
                <span>Logout</span>
              </button>
            </div>
          </div>
        </div>
      </motion.div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Current Ride Status */}
        {currentRide && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-gradient-to-r from-blue-500 to-purple-600 rounded-2xl p-6 text-white mb-8"
          >
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-xl font-semibold mb-2">Current Ride</h2>
                <p className="text-blue-100 mb-1">
                  From: {pickupLocation?.address || 'Pickup location'}
                </p>
                <p className="text-blue-100">
                  To: {dropoffLocation?.address || 'Dropoff location'}
                </p>
              </div>
              <div className="text-right">
                <div className="text-3xl font-bold">${currentRide.totalFare}</div>
                <p className="text-blue-100 text-sm">Total Fare</p>
              </div>
            </div>
            <div className="mt-4 flex items-center space-x-4">
              <button className="bg-white bg-opacity-20 px-4 py-2 rounded-lg hover:bg-opacity-30 transition-all">
                Track Ride
              </button>
              <button className="bg-white bg-opacity-20 px-4 py-2 rounded-lg hover:bg-opacity-30 transition-all">
                Contact Driver
              </button>
            </div>
          </motion.div>
        )}

        {/* Quick Actions */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8"
        >
          {quickActions.map((action, index) => (
            <motion.button
              key={action.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 + index * 0.1 }}
              onClick={() => handleQuickAction(action.action)}
              className={`bg-white p-6 rounded-2xl shadow-sm border border-gray-200 hover:shadow-md transition-all hover:scale-105 group`}
            >
              <div className={`w-12 h-12 bg-${action.color}-100 rounded-xl flex items-center justify-center mb-4 group-hover:scale-110 transition-transform`}>
                <action.icon className={`w-6 h-6 text-${action.color}-600`} />
              </div>
              <h3 className="font-medium text-gray-900">{action.title}</h3>
            </motion.button>
          ))}
        </motion.div>

        {/* Stats Overview */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8"
        >
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Rides</p>
                <p className="text-2xl font-bold text-gray-900">{stats.totalRides}</p>
              </div>
              <div className="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center">
                <Car className="w-6 h-6 text-blue-600" />
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Average Rating</p>
                <p className="text-2xl font-bold text-gray-900">{stats.averageRating}</p>
              </div>
              <div className="w-12 h-12 bg-yellow-100 rounded-xl flex items-center justify-center">
                <Star className="w-6 h-6 text-yellow-600" />
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Spent</p>
                <p className="text-2xl font-bold text-gray-900">${stats.totalSpent}</p>
              </div>
              <div className="w-12 h-12 bg-green-100 rounded-xl flex items-center justify-center">
                <DollarSign className="w-6 h-6 text-green-600" />
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">This Month</p>
                <p className="text-2xl font-bold text-gray-900">${stats.thisMonth}</p>
              </div>
              <div className="w-12 h-12 bg-purple-100 rounded-xl flex items-center justify-center">
                <TrendingUp className="w-6 h-6 text-purple-600" />
              </div>
            </div>
          </div>
        </motion.div>

        {/* Recent Rides */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 mb-8"
        >
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-semibold text-gray-900">Recent Rides</h2>
            <button 
              onClick={() => navigate('/ride-history')}
              className="text-blue-600 hover:text-blue-700 font-medium"
            >
              View All
            </button>
          </div>
          
          <div className="space-y-4">
            {recentRides.map((ride, index) => (
              <motion.div
                key={ride.id}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.4 + index * 0.1 }}
                className="flex items-center justify-between p-4 bg-gray-50 rounded-xl hover:bg-gray-100 transition-colors"
              >
                <div className="flex items-center space-x-4">
                  <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center">
                    <Car className="w-5 h-5 text-blue-600" />
                  </div>
                  <div>
                    <p className="font-medium text-gray-900">{ride.from} → {ride.to}</p>
                    <p className="text-sm text-gray-500">{ride.date}</p>
                  </div>
                </div>
                
                <div className="flex items-center space-x-4">
                  <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(ride.status)}`}>
                    {getStatusText(ride.status)}
                  </span>
                  {ride.rating && (
                    <div className="flex items-center space-x-1">
                      <Star className="w-4 h-4 text-yellow-500 fill-current" />
                      <span className="text-sm text-gray-600">{ride.rating}</span>
                    </div>
                  )}
                </div>
              </motion.div>
            ))}
          </div>
        </motion.div>

        {/* Location Status */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6"
        >
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Location Services</h2>
          
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="text-center">
              <div className={`w-16 h-16 mx-auto mb-3 rounded-full flex items-center justify-center ${
                currentLocation ? 'bg-green-100' : 'bg-red-100'
              }`}>
                <MapPin className={`w-8 h-8 ${currentLocation ? 'text-green-600' : 'text-red-600'}`} />
              </div>
              <h3 className="font-medium text-gray-900">GPS Location</h3>
              <p className={`text-sm ${currentLocation ? 'text-green-600' : 'text-red-600'}`}>
                {currentLocation ? 'Active' : 'Inactive'}
              </p>
            </div>

            <div className="text-center">
              <div className={`w-16 h-16 mx-auto mb-3 rounded-full flex items-center justify-center ${
                pickupLocation ? 'bg-blue-100' : 'bg-gray-100'
              }`}>
                <MapPin className={`w-8 h-8 ${pickupLocation ? 'text-blue-600' : 'text-gray-400'}`} />
              </div>
              <h3 className="font-medium text-gray-900">Pickup Location</h3>
              <p className={`text-sm ${pickupLocation ? 'text-blue-600' : 'text-gray-500'}`}>
                {pickupLocation ? 'Set' : 'Not Set'}
              </p>
            </div>

            <div className="text-center">
              <div className={`w-16 h-16 mx-auto mb-3 rounded-full flex items-center justify-center ${
                dropoffLocation ? 'bg-purple-100' : 'bg-gray-100'
              }`}>
                <MapPin className={`w-8 h-8 ${dropoffLocation ? 'text-purple-600' : 'text-gray-400'}`} />
              </div>
              <h3 className="font-medium text-gray-900">Dropoff Location</h3>
              <p className={`text-sm ${dropoffLocation ? 'text-purple-600' : 'text-gray-500'}`}>
                {dropoffLocation ? 'Set' : 'Not Set'}
              </p>
            </div>
          </div>

          <div className="mt-6 flex justify-center">
            <button 
              onClick={() => navigate('/book-ride')}
              className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition-colors flex items-center space-x-2"
            >
              <Plus className="w-5 h-5" />
              <span>Book New Ride</span>
            </button>
          </div>
        </motion.div>
      </div>
    </div>
  );
};

export default DashboardPage;
