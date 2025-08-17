import React, { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  MapPin, 
  Car, 
  User, 
  LogOut, 
  Menu, 
  X, 
  Home, 
  Clock, 
  Settings,
  Bell,
  Search,
  Plus
} from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import { useLocation as useLocationContext } from '../../contexts/LocationContext';

interface LayoutProps {
  children: React.ReactNode;
}

const Layout: React.FC<LayoutProps> = ({ children }) => {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [notifications, setNotifications] = useState(3); // Mock notification count
  const { state: authState, logout } = useAuth();
  const { state: locationState, getCurrentLocation } = useLocationContext();
  const location = useLocation();
  const navigate = useNavigate();

  // Close sidebar on route change
  useEffect(() => {
    setSidebarOpen(false);
  }, [location.pathname]);

  // Get current location on mount
  useEffect(() => {
    if (!locationState.currentLocation) {
      getCurrentLocation();
    }
  }, []);

  const navigation = [
    { name: 'Dashboard', href: '/dashboard', icon: Home },
    { name: 'Book Ride', href: '/book-ride', icon: Car },
    { name: 'Ride History', href: '/ride-history', icon: Clock },
    { name: 'Profile', href: '/profile', icon: User },
  ];

  const driverNavigation = [
    { name: 'Driver Dashboard', href: '/driver-dashboard', icon: Car },
    { name: 'Ride History', href: '/ride-history', icon: Clock },
    { name: 'Profile', href: '/profile', icon: User },
  ];

  const currentNavigation = authState.user?.isDriver ? driverNavigation : navigation;

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  const Sidebar = () => (
    <motion.div
      initial={{ x: -300 }}
      animate={{ x: 0 }}
      exit={{ x: -300 }}
      transition={{ type: 'spring', damping: 25, stiffness: 200 }}
      className="fixed inset-y-0 left-0 z-50 w-64 bg-white shadow-xl border-r border-gray-200"
    >
      <div className="flex flex-col h-full">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <Link to="/" className="flex items-center space-x-2">
            <div className="w-8 h-8 bg-primary-600 rounded-lg flex items-center justify-center">
              <Car className="w-5 h-5 text-white" />
            </div>
            <span className="text-xl font-bold text-gray-900">UberClone</span>
          </Link>
          <button
            onClick={() => setSidebarOpen(false)}
            className="lg:hidden p-2 rounded-lg hover:bg-gray-100"
          >
            <X className="w-5 h-5 text-gray-600" />
          </button>
        </div>

        {/* User Info */}
        <div className="p-6 border-b border-gray-200">
          <div className="flex items-center space-x-3">
            <div className="w-12 h-12 bg-primary-100 rounded-full flex items-center justify-center">
              <User className="w-6 h-6 text-primary-600" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-gray-900 truncate">
                {authState.user?.firstName} {authState.user?.lastName}
              </p>
              <p className="text-xs text-gray-500 truncate">
                {authState.user?.isDriver ? 'Driver' : 'Passenger'}
              </p>
            </div>
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-4 py-6 space-y-2">
          {currentNavigation.map((item) => {
            const isActive = location.pathname === item.href;
            return (
              <Link
                key={item.name}
                to={item.href}
                className={`flex items-center space-x-3 px-3 py-2 rounded-lg text-sm font-medium transition-colors duration-200 ${
                  isActive
                    ? 'bg-primary-50 text-primary-700 border-r-2 border-primary-600'
                    : 'text-gray-700 hover:bg-gray-50 hover:text-gray-900'
                }`}
              >
                <item.icon className={`w-5 h-5 ${
                  isActive ? 'text-primary-600' : 'text-gray-400'
                }`} />
                <span>{item.name}</span>
              </Link>
            );
          })}
        </nav>

        {/* Footer */}
        <div className="p-4 border-t border-gray-200">
          <button
            onClick={handleLogout}
            className="flex items-center space-x-3 w-full px-3 py-2 rounded-lg text-sm font-medium text-gray-700 hover:bg-gray-50 hover:text-gray-900 transition-colors duration-200"
          >
            <LogOut className="w-5 h-5 text-gray-400" />
            <span>Logout</span>
          </button>
        </div>
      </div>
    </motion.div>
  );

  const TopBar = () => (
    <div className="bg-white border-b border-gray-200 px-4 py-3">
      <div className="flex items-center justify-between">
        {/* Left side */}
        <div className="flex items-center space-x-4">
          <button
            onClick={() => setSidebarOpen(true)}
            className="lg:hidden p-2 rounded-lg hover:bg-gray-100"
          >
            <Menu className="w-5 h-5 text-gray-600" />
          </button>
          
          {/* Location indicator */}
          <div className="hidden sm:flex items-center space-x-2 text-sm text-gray-600">
            <MapPin className="w-4 h-4" />
            <span className="truncate max-w-48">
              {locationState.currentLocation?.address || 'Getting location...'}
            </span>
          </div>

          {/* Profile Menu */}
          <div className="relative">
            <button
              onClick={() => navigate('/profile')}
              className="flex items-center space-x-2 p-2 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <div className="w-8 h-8 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full flex items-center justify-center">
                <span className="text-white text-sm font-medium">
                  {authState.user?.firstName?.charAt(0)}{authState.user?.lastName?.charAt(0)}
                </span>
              </div>
              <span className="text-sm font-medium text-gray-700 hidden md:block">
                {authState.user?.firstName}
              </span>
            </button>
          </div>
        </div>

        {/* Right side */}
        <div className="flex items-center space-x-3">
          {/* Search */}
          <button className="p-2 rounded-lg hover:bg-gray-100">
            <Search className="w-5 h-5 text-gray-600" />
          </button>

          {/* Notifications */}
          <button className="relative p-2 rounded-lg hover:bg-gray-100">
            <Bell className="w-5 h-5 text-gray-600" />
            {notifications > 0 && (
              <span className="absolute -top-1 -right-1 w-5 h-5 bg-error-500 text-white text-xs rounded-full flex items-center justify-center">
                {notifications > 9 ? '9+' : notifications}
              </span>
            )}
          </button>

          {/* Quick Actions */}
          <div className="hidden md:flex items-center space-x-2">
            <button className="btn btn-primary btn-sm">
              <Plus className="w-4 h-4 mr-1" />
              Book Ride
            </button>
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Mobile sidebar overlay */}
      <AnimatePresence>
        {sidebarOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-40 bg-black bg-opacity-50 lg:hidden"
            onClick={() => setSidebarOpen(false)}
          />
        )}
      </AnimatePresence>

      {/* Sidebar */}
      <AnimatePresence>
        {sidebarOpen && <Sidebar />}
      </AnimatePresence>

      {/* Desktop sidebar */}
      <div className="hidden lg:block lg:fixed lg:inset-y-0 lg:left-0 lg:z-40">
        <Sidebar />
      </div>

      {/* Main content */}
      <div className="lg:pl-64">
        {/* Top bar */}
        <TopBar />

        {/* Page content */}
        <main className="p-6">
          {children}
        </main>
      </div>
    </div>
  );
};

export default Layout;
