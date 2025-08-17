import React, { createContext, useContext, useReducer, useEffect, ReactNode, useCallback } from 'react';
import toast from 'react-hot-toast';

// Types
export interface Location {
  latitude: number;
  longitude: number;
  accuracy?: number;
  timestamp: number;
  address?: string;
  city?: string;
  state?: string;
  country?: string;
  postalCode?: string;
}

export interface LocationState {
  currentLocation: Location | null;
  pickupLocation: Location | null;
  dropoffLocation: Location | null;
  hasPermission: boolean;
  isTracking: boolean;
  isLoading: boolean;
  error: string | null;
  watchId: number | null;
}

// Action Types
type LocationAction =
  | { type: 'SET_CURRENT_LOCATION'; payload: Location }
  | { type: 'SET_PICKUP_LOCATION'; payload: Location | null }
  | { type: 'SET_DROPOFF_LOCATION'; payload: Location | null }
  | { type: 'SET_PERMISSION'; payload: boolean }
  | { type: 'SET_TRACKING'; payload: boolean }
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'SET_WATCH_ID'; payload: number | null }
  | { type: 'CLEAR_ERROR' }
  | { type: 'RESET_LOCATIONS' };

// Initial State
const initialState: LocationState = {
  currentLocation: null,
  pickupLocation: null,
  dropoffLocation: null,
  hasPermission: false,
  isTracking: false,
  isLoading: false,
  error: null,
  watchId: null,
};

// Reducer
const locationReducer = (state: LocationState, action: LocationAction): LocationState => {
  switch (action.type) {
    case 'SET_CURRENT_LOCATION':
      return {
        ...state,
        currentLocation: action.payload,
      };
    case 'SET_PICKUP_LOCATION':
      return {
        ...state,
        pickupLocation: action.payload,
      };
    case 'SET_DROPOFF_LOCATION':
      return {
        ...state,
        dropoffLocation: action.payload,
      };
    case 'SET_PERMISSION':
      return {
        ...state,
        hasPermission: action.payload,
      };
    case 'SET_TRACKING':
      return {
        ...state,
        isTracking: action.payload,
      };
    case 'SET_LOADING':
      return {
        ...state,
        isLoading: action.payload,
      };
    case 'SET_ERROR':
      return {
        ...state,
        error: action.payload,
      };
    case 'SET_WATCH_ID':
      return {
        ...state,
        watchId: action.payload,
      };
    case 'CLEAR_ERROR':
      return {
        ...state,
        error: null,
      };
    case 'RESET_LOCATIONS':
      return {
        ...state,
        pickupLocation: null,
        dropoffLocation: null,
      };
    default:
      return state;
  }
};

// Context
interface LocationContextType {
  state: LocationState;
  requestLocationPermission: () => Promise<boolean>;
  getCurrentLocation: () => Promise<Location | null>;
  startLocationTracking: () => Promise<void>;
  stopLocationTracking: () => void;
  setPickupLocation: (location: Location | null) => void;
  setDropoffLocation: (location: Location | null) => void;
  reverseGeocode: (latitude: number, longitude: number) => Promise<string>;
  calculateDistance: (lat1: number, lon1: number, lat2: number, lon2: number) => number;
  clearError: () => void;
  resetLocations: () => void;
}

const LocationContext = createContext<LocationContextType | undefined>(undefined);

// Provider Component
interface LocationProviderProps {
  children: ReactNode;
}

export const LocationProvider: React.FC<LocationProviderProps> = ({ children }) => {
  const [state, dispatch] = useReducer(locationReducer, initialState);

  // Check if geolocation is supported
  const isGeolocationSupported = () => {
    return 'geolocation' in navigator;
  };

  // Request location permission
  const requestLocationPermission = async (): Promise<boolean> => {
    if (!isGeolocationSupported()) {
      dispatch({ type: 'SET_ERROR', payload: 'Geolocation is not supported in this browser' });
      return false;
    }

    try {
      dispatch({ type: 'SET_LOADING', payload: true });
      dispatch({ type: 'CLEAR_ERROR' });

      // Check if we already have permission
      if (state.hasPermission) {
        return true;
      }

      // Request permission by getting current position
      const position = await getCurrentPosition();
      if (position) {
        dispatch({ type: 'SET_PERMISSION', payload: true });
        return true;
      }

      return false;
    } catch (error: any) {
      const errorMessage = error.message || 'Failed to get location permission';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      toast.error('Location permission denied. Please enable location access.');
      return false;
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false });
    }
  };

  // Get current position with timeout
  const getCurrentPosition = (): Promise<GeolocationPosition> => {
    return new Promise((resolve, reject) => {
      const options: PositionOptions = {
        enableHighAccuracy: true,
        timeout: 10000, // 10 seconds
        maximumAge: 60000, // 1 minute
      };

      navigator.geolocation.getCurrentPosition(resolve, reject, options);
    });
  };

  // Get current location
  const getCurrentLocation = async (): Promise<Location | null> => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true });
      dispatch({ type: 'CLEAR_ERROR' });

      if (!state.hasPermission) {
        const hasPermission = await requestLocationPermission();
        if (!hasPermission) {
          return null;
        }
      }

      const position = await getCurrentPosition();
      const location: Location = {
        latitude: position.coords.latitude,
        longitude: position.coords.longitude,
        accuracy: position.coords.accuracy,
        timestamp: position.timestamp,
      };

      // Try to get address
      try {
        location.address = await reverseGeocode(location.latitude, location.longitude);
      } catch (error) {
        console.warn('Failed to get address:', error);
      }

      dispatch({ type: 'SET_CURRENT_LOCATION', payload: location });
      return location;
    } catch (error: any) {
      const errorMessage = error.message || 'Failed to get current location';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      toast.error('Failed to get your location. Please check your location settings.');
      return null;
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false });
    }
  };

  // Start location tracking
  const startLocationTracking = async (): Promise<void> => {
    try {
      if (!state.hasPermission) {
        const hasPermission = await requestLocationPermission();
        if (!hasPermission) {
          return;
        }
      }

      if (state.isTracking) {
        return; // Already tracking
      }

      const options: PositionOptions = {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 30000, // 30 seconds
      };

      const watchId = navigator.geolocation.watchPosition(
        (position) => {
          const location: Location = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy,
            timestamp: position.timestamp,
          };

          dispatch({ type: 'SET_CURRENT_LOCATION', payload: location });
        },
        (error) => {
          console.error('Location tracking error:', error);
          dispatch({ type: 'SET_ERROR', payload: error.message });
        },
        options
      );

      dispatch({ type: 'SET_WATCH_ID', payload: watchId });
      dispatch({ type: 'SET_TRACKING', payload: true });
      toast.success('Location tracking started');
    } catch (error: any) {
      const errorMessage = error.message || 'Failed to start location tracking';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      toast.error('Failed to start location tracking');
    }
  };

  // Stop location tracking
  const stopLocationTracking = (): void => {
    if (state.watchId !== null) {
      navigator.geolocation.clearWatch(state.watchId);
      dispatch({ type: 'SET_WATCH_ID', payload: null });
      dispatch({ type: 'SET_TRACKING', payload: false });
      toast.success('Location tracking stopped');
    }
  };

  // Set pickup location
  const setPickupLocation = (location: Location | null): void => {
    dispatch({ type: 'SET_PICKUP_LOCATION', payload: location });
  };

  // Set dropoff location
  const setDropoffLocation = (location: Location | null): void => {
    dispatch({ type: 'SET_DROPOFF_LOCATION', payload: location });
  };

  // Reverse geocoding to get address from coordinates
  const reverseGeocode = async (latitude: number, longitude: number): Promise<string> => {
    try {
      // Using OpenStreetMap Nominatim API (free, no API key required)
      const response = await fetch(
        `https://nominatim.openstreetmap.org/reverse?format=json&lat=${latitude}&lon=${longitude}&zoom=18&addressdetails=1`
      );

      if (!response.ok) {
        throw new Error('Reverse geocoding failed');
      }

      const data = await response.json();
      
      if (data.display_name) {
        return data.display_name;
      }

      // Fallback to coordinates
      return `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`;
    } catch (error) {
      console.warn('Reverse geocoding failed:', error);
      // Return coordinates as fallback
      return `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`;
    }
  };

  // Calculate distance between two points using Haversine formula
  const calculateDistance = (lat1: number, lon1: number, lat2: number, lon2: number): number => {
    const R = 6371; // Earth's radius in kilometers
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  };

  // Clear error
  const clearError = (): void => {
    dispatch({ type: 'CLEAR_ERROR' });
  };

  // Reset locations
  const resetLocations = (): void => {
    dispatch({ type: 'RESET_LOCATIONS' });
  };

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (state.watchId !== null) {
        navigator.geolocation.clearWatch(state.watchId);
      }
    };
  }, [state.watchId]);

  // Auto-get location on mount if permission is granted
  useEffect(() => {
    if (state.hasPermission && !state.currentLocation) {
      getCurrentLocation();
    }
  }, [state.hasPermission]);

  const value: LocationContextType = {
    state,
    requestLocationPermission,
    getCurrentLocation,
    startLocationTracking,
    stopLocationTracking,
    setPickupLocation,
    setDropoffLocation,
    reverseGeocode,
    calculateDistance,
    clearError,
    resetLocations,
  };

  return (
    <LocationContext.Provider value={value}>
      {children}
    </LocationContext.Provider>
  );
};

// Hook to use location context
export const useLocation = (): LocationContextType => {
  const context = useContext(LocationContext);
  if (context === undefined) {
    throw new Error('useLocation must be used within a LocationProvider');
  }
  return context;
};

// Hook to get current location
export const useCurrentLocation = (): Location | null => {
  const { state } = useLocation();
  return state.currentLocation;
};

// Hook to check if location tracking is active
export const useIsLocationTracking = (): boolean => {
  const { state } = useLocation();
  return state.isTracking;
};

// Hook to check if location permission is granted
export const useHasLocationPermission = (): boolean => {
  const { state } = useLocation();
  return state.hasPermission;
};
