import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { 
  Clock, 
  MapPin, 
  Star, 
  Car, 
  Filter,
  Search,
  Calendar,
  DollarSign,
  Navigation,
  User,
  MessageCircle
} from 'lucide-react';
import { useRide } from '../../contexts/RideContext';
import { toast } from 'react-hot-toast';

interface RideHistoryItem {
  id: string;
  date: string;
  from: string;
  to: string;
  driver: string;
  vehicle: string;
  status: 'completed' | 'cancelled' | 'in_progress';
  rating: number | null;
  fare: number;
  distance: number;
  duration: number;
}

const RideHistoryPage: React.FC = () => {
  const { state } = useRide();
  const { rideHistory, isLoading } = state;
  const [filteredRides, setFilteredRides] = useState<RideHistoryItem[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState<string>('all');
  const [dateFilter, setDateFilter] = useState<string>('all');

  // Mock data for demonstration
  const mockRideHistory: RideHistoryItem[] = [
    {
      id: '1',
      date: '2024-01-15',
      from: 'Home',
      to: 'Office',
      driver: 'John Driver',
      vehicle: 'Toyota Camry',
      status: 'completed',
      rating: 5,
      fare: 25.50,
      distance: 12.5,
      duration: 25
    },
    {
      id: '2',
      date: '2024-01-14',
      from: 'Mall',
      to: 'Airport',
      driver: 'Sarah Driver',
      vehicle: 'Honda Accord',
      status: 'completed',
      rating: 4,
      fare: 45.00,
      distance: 28.0,
      duration: 45
    },
    {
      id: '3',
      date: '2024-01-13',
      from: 'Restaurant',
      to: 'Home',
      driver: 'Mike Driver',
      vehicle: 'Ford Fusion',
      status: 'cancelled',
      rating: null,
      fare: 18.75,
      distance: 8.2,
      duration: 0
    },
    {
      id: '4',
      date: '2024-01-12',
      from: 'Gym',
      to: 'Home',
      driver: 'Lisa Driver',
      vehicle: 'Nissan Altima',
      status: 'completed',
      rating: 5,
      fare: 22.00,
      distance: 10.8,
      duration: 22
    },
    {
      id: '5',
      date: '2024-01-11',
      from: 'Home',
      to: 'Shopping Center',
      driver: 'David Driver',
      vehicle: 'Chevrolet Malibu',
      status: 'completed',
      rating: 4,
      fare: 19.50,
      distance: 9.5,
      duration: 20
    }
  ];

  useEffect(() => {
    // In a real app, this would come from the context
    setFilteredRides(mockRideHistory);
  }, []);

  useEffect(() => {
    let filtered = mockRideHistory;

    // Apply search filter
    if (searchTerm) {
      filtered = filtered.filter(ride => 
        ride.from.toLowerCase().includes(searchTerm.toLowerCase()) ||
        ride.to.toLowerCase().includes(searchTerm.toLowerCase()) ||
        ride.driver.toLowerCase().includes(searchTerm.toLowerCase())
      );
    }

    // Apply status filter
    if (statusFilter !== 'all') {
      filtered = filtered.filter(ride => ride.status === statusFilter);
    }

    // Apply date filter
    if (dateFilter !== 'all') {
      const today = new Date();
      const yesterday = new Date(today);
      yesterday.setDate(yesterday.getDate() - 1);
      const lastWeek = new Date(today);
      lastWeek.setDate(lastWeek.getDate() - 7);
      const lastMonth = new Date(today);
      lastMonth.setMonth(lastMonth.getMonth() - 1);

      filtered = filtered.filter(ride => {
        const rideDate = new Date(ride.date);
        switch (dateFilter) {
          case 'today':
            return rideDate.toDateString() === today.toDateString();
          case 'yesterday':
            return rideDate.toDateString() === yesterday.toDateString();
          case 'lastWeek':
            return rideDate >= lastWeek;
          case 'lastMonth':
            return rideDate >= lastMonth;
          default:
            return true;
        }
      });
    }

    setFilteredRides(filtered);
  }, [searchTerm, statusFilter, dateFilter]);

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

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
      year: 'numeric'
    });
  };

  const handleRateRide = (rideId: string) => {
    toast.success('Rating feature coming soon!');
  };

  const handleContactDriver = (rideId: string) => {
    toast.success('Contact feature coming soon!');
  };

  const handleViewDetails = (rideId: string) => {
    toast.success('Details view coming soon!');
  };

  const totalRides = filteredRides.length;
  const totalSpent = filteredRides.reduce((sum, ride) => sum + ride.fare, 0);
  const averageRating = filteredRides
    .filter(ride => ride.rating !== null)
    .reduce((sum, ride) => sum + (ride.rating || 0), 0) / 
    filteredRides.filter(ride => ride.rating !== null).length || 0;

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="bg-white shadow-sm border-b"
      >
        <div className="max-w-7xl mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <h1 className="text-xl font-semibold text-gray-900">Ride History</h1>
            <div className="flex items-center space-x-4">
              <span className="text-sm text-gray-500">
                {totalRides} rides • ${totalSpent.toFixed(2)} spent
              </span>
            </div>
          </div>
        </div>
      </motion.div>

      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Stats Overview */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8"
        >
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Rides</p>
                <p className="text-2xl font-bold text-gray-900">{totalRides}</p>
              </div>
              <div className="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center">
                <Car className="w-6 h-6 text-blue-600" />
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Total Spent</p>
                <p className="text-2xl font-bold text-gray-900">${totalSpent.toFixed(2)}</p>
              </div>
              <div className="w-12 h-12 bg-green-100 rounded-xl flex items-center justify-center">
                <DollarSign className="w-6 h-6 text-green-600" />
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">Average Rating</p>
                <p className="text-2xl font-bold text-gray-900">{averageRating.toFixed(1)}</p>
              </div>
              <div className="w-12 h-12 bg-yellow-100 rounded-xl flex items-center justify-center">
                <Star className="w-6 h-6 text-yellow-600" />
              </div>
            </div>
          </div>

          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-200">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600">This Month</p>
                <p className="text-2xl font-bold text-gray-900">
                  ${filteredRides
                    .filter(ride => {
                      const rideDate = new Date(ride.date);
                      const thisMonth = new Date();
                      return rideDate.getMonth() === thisMonth.getMonth() && 
                             rideDate.getFullYear() === thisMonth.getFullYear();
                    })
                    .reduce((sum, ride) => sum + ride.fare, 0)
                    .toFixed(2)}
                </p>
              </div>
              <div className="w-12 h-12 bg-purple-100 rounded-xl flex items-center justify-center">
                <Calendar className="w-6 h-6 text-purple-600" />
              </div>
            </div>
          </div>
        </motion.div>

        {/* Filters */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 mb-8"
        >
          <div className="flex flex-col md:flex-row gap-4">
            {/* Search */}
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search rides by location or driver..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="w-full pl-10 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
            </div>

            {/* Status Filter */}
            <div className="w-full md:w-48">
              <select
                value={statusFilter}
                onChange={(e) => setStatusFilter(e.target.value)}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="all">All Status</option>
                <option value="completed">Completed</option>
                <option value="cancelled">Cancelled</option>
                <option value="in_progress">In Progress</option>
              </select>
            </div>

            {/* Date Filter */}
            <div className="w-full md:w-48">
              <select
                value={dateFilter}
                onChange={(e) => setDateFilter(e.target.value)}
                className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              >
                <option value="all">All Time</option>
                <option value="today">Today</option>
                <option value="yesterday">Yesterday</option>
                <option value="lastWeek">Last 7 Days</option>
                <option value="lastMonth">Last 30 Days</option>
              </select>
            </div>
          </div>
        </motion.div>

        {/* Ride List */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="bg-white rounded-2xl shadow-sm border border-gray-200 overflow-hidden"
        >
          {isLoading ? (
            <div className="p-8 text-center">
              <div className="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
              <p className="text-gray-500">Loading ride history...</p>
            </div>
          ) : filteredRides.length === 0 ? (
            <div className="p-8 text-center">
              <Car className="w-16 h-16 mx-auto mb-4 text-gray-300" />
              <p className="text-lg font-medium text-gray-900 mb-2">No rides found</p>
              <p className="text-gray-500">Try adjusting your filters or search terms</p>
            </div>
          ) : (
            <div className="divide-y divide-gray-200">
              {filteredRides.map((ride, index) => (
                <motion.div
                  key={ride.id}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.1 + index * 0.05 }}
                  className="p-6 hover:bg-gray-50 transition-colors"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex-1">
                      <div className="flex items-start space-x-4">
                        <div className="w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center">
                          <Car className="w-6 h-6 text-blue-600" />
                        </div>
                        
                        <div className="flex-1">
                          <div className="flex items-center space-x-3 mb-2">
                            <h3 className="font-medium text-gray-900">
                              {ride.from} → {ride.to}
                            </h3>
                            <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(ride.status)}`}>
                              {getStatusText(ride.status)}
                            </span>
                          </div>
                          
                          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm text-gray-600">
                            <div className="flex items-center space-x-2">
                              <Clock className="w-4 h-4" />
                              <span>{formatDate(ride.date)}</span>
                            </div>
                            <div className="flex items-center space-x-2">
                              <User className="w-4 h-4" />
                              <span>{ride.driver}</span>
                            </div>
                            <div className="flex items-center space-x-2">
                              <Navigation className="w-4 h-4" />
                              <span>{ride.distance} km</span>
                            </div>
                            <div className="flex items-center space-x-2">
                              <DollarSign className="w-4 h-4" />
                              <span>${ride.fare.toFixed(2)}</span>
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-3 ml-6">
                      {ride.rating && (
                        <div className="flex items-center space-x-1">
                          <Star className="w-4 h-4 text-yellow-500 fill-current" />
                          <span className="text-sm font-medium">{ride.rating}</span>
                        </div>
                      )}
                      
                      <div className="flex space-x-2">
                        <button
                          onClick={() => handleViewDetails(ride.id)}
                          className="px-3 py-2 text-blue-600 hover:text-blue-700 font-medium text-sm"
                        >
                          Details
                        </button>
                        
                        {ride.status === 'completed' && !ride.rating && (
                          <button
                            onClick={() => handleRateRide(ride.id)}
                            className="px-3 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm"
                          >
                            Rate
                          </button>
                        )}
                        
                        <button
                          onClick={() => handleContactDriver(ride.id)}
                          className="p-2 text-gray-600 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition-colors"
                        >
                          <MessageCircle className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  </div>
                </motion.div>
              ))}
            </div>
          )}
        </motion.div>
      </div>
    </div>
  );
};

export default RideHistoryPage;
