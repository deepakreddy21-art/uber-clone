import React, { createContext, useContext, useReducer, useEffect, ReactNode } from 'react';
import { useAuth } from './AuthContext';
import axios from 'axios';
import toast from 'react-hot-toast';

// Types
export interface Location {
  latitude: number;
  longitude: number;
  address?: string;
}

export interface Driver {
  id: number;
  firstName: string;
  lastName: string;
  rating: number;
  vehicleType: string;
  vehicleModel: string;
  vehicleColor: string;
  licensePlate: string;
  profilePicture?: string;
  currentLocation: Location;
  eta: number; // in minutes
}

export interface RideRequest {
  id: number;
  pickupLocation: Location;
  dropoffLocation: Location;
  vehicleType: string;
  estimatedFare: number;
  estimatedDuration: number; // in minutes
  estimatedDistance: number; // in kilometers
  passengerCount: number;
  specialInstructions?: string;
  status: 'PENDING' | 'ACCEPTED' | 'STARTED' | 'COMPLETED' | 'CANCELLED';
  createdAt: string;
}

export interface Ride {
  id: number;
  user: {
    id: number;
    firstName: string;
    lastName: string;
    phoneNumber: string;
  };
  driver?: Driver;
  pickupLocation: Location;
  dropoffLocation: Location;
  pickupLatitude: number;
  pickupLongitude: number;
  dropoffLatitude: number;
  dropoffLongitude: number;
  vehicleType: string;
  status: 'REQUESTED' | 'ACCEPTED' | 'STARTED' | 'COMPLETED' | 'CANCELLED';
  baseFare: number;
  distanceFare: number;
  timeFare: number;
  totalFare: number;
  distance: number;
  estimatedDuration: number;
  actualDuration?: number;
  requestedAt: string;
  acceptedAt?: string;
  startedAt?: string;
  completedAt?: string;
  cancelledAt?: string;
  cancellationReason?: string;
  cancelledBy?: 'USER' | 'DRIVER' | 'SYSTEM';
  userRating?: number;
  userReview?: string;
  driverRating?: number;
  driverReview?: string;
  paymentMethod: 'CASH' | 'CARD' | 'WALLET';
  paymentStatus: 'PENDING' | 'COMPLETED' | 'FAILED';
  transactionId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface RideState {
  currentRide: Ride | null;
  rideHistory: Ride[];
  availableDrivers: Driver[];
  isLoading: boolean;
  error: string | null;
  isSearching: boolean;
  searchResults: Driver[];
}

// Action Types
type RideAction =
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'SET_CURRENT_RIDE'; payload: Ride | null }
  | { type: 'SET_RIDE_HISTORY'; payload: Ride[] }
  | { type: 'ADD_RIDE_TO_HISTORY'; payload: Ride }
  | { type: 'UPDATE_RIDE_STATUS'; payload: { rideId: number; status: Ride['status']; driver?: Driver } }
  | { type: 'SET_AVAILABLE_DRIVERS'; payload: Driver[] }
  | { type: 'SET_SEARCHING'; payload: boolean }
  | { type: 'SET_SEARCH_RESULTS'; payload: Driver[] }
  | { type: 'CLEAR_ERROR' };

// Initial State
const initialState: RideState = {
  currentRide: null,
  rideHistory: [],
  availableDrivers: [],
  isLoading: false,
  error: null,
  isSearching: false,
  searchResults: [],
};

// Reducer
const rideReducer = (state: RideState, action: RideAction): RideState => {
  switch (action.type) {
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
    case 'SET_CURRENT_RIDE':
      return {
        ...state,
        currentRide: action.payload,
      };
    case 'SET_RIDE_HISTORY':
      return {
        ...state,
        rideHistory: action.payload,
      };
    case 'ADD_RIDE_TO_HISTORY':
      return {
        ...state,
        rideHistory: [action.payload, ...state.rideHistory],
      };
    case 'UPDATE_RIDE_STATUS':
      return {
        ...state,
        currentRide: state.currentRide?.id === action.payload.rideId
          ? { ...state.currentRide, status: action.payload.status, driver: action.payload.driver }
          : state.currentRide,
        rideHistory: state.rideHistory.map(ride =>
          ride.id === action.payload.rideId
            ? { ...ride, status: action.payload.status, driver: action.payload.driver }
            : ride
        ),
      };
    case 'SET_AVAILABLE_DRIVERS':
      return {
        ...state,
        availableDrivers: action.payload,
      };
    case 'SET_SEARCHING':
      return {
        ...state,
        isSearching: action.payload,
      };
    case 'SET_SEARCH_RESULTS':
      return {
        ...state,
        searchResults: action.payload,
      };
    case 'CLEAR_ERROR':
      return {
        ...state,
        error: null,
      };
    default:
      return state;
  }
};

// Context
interface RideContextType {
  state: RideState;
  requestRide: (rideData: Omit<RideRequest, 'id' | 'status' | 'createdAt'>) => Promise<Ride>;
  cancelRide: (rideId: number, reason?: string) => Promise<void>;
  getRideHistory: () => Promise<void>;
  searchForDrivers: (location: Location, vehicleType?: string) => Promise<Driver[]>;
  updateRideStatus: (rideId: number, status: Ride['status']) => Promise<void>;
  rateRide: (rideId: number, rating: number, review?: string) => Promise<void>;
  clearError: () => void;
}

const RideContext = createContext<RideContextType | undefined>(undefined);

// Provider Component
interface RideProviderProps {
  children: ReactNode;
}

export const RideProvider: React.FC<RideProviderProps> = ({ children }) => {
  const [state, dispatch] = useReducer(rideReducer, initialState);
  const { state: authState } = useAuth();

  // Load ride history on mount
  useEffect(() => {
    if (authState.isAuthenticated) {
      getRideHistory();
    }
  }, [authState.isAuthenticated]);

  // Request a new ride
  const requestRide = async (rideData: Omit<RideRequest, 'id' | 'status' | 'createdAt'>): Promise<Ride> => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true });
      dispatch({ type: 'CLEAR_ERROR' });

      const response = await axios.post('/api/rides/request', rideData);
      const ride: Ride = response.data;

      dispatch({ type: 'SET_CURRENT_RIDE', payload: ride });
      toast.success('Ride request sent successfully!');

      return ride;
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to request ride';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      toast.error(errorMessage);
      throw error;
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false });
    }
  };

  // Cancel a ride
  const cancelRide = async (rideId: number, reason?: string): Promise<void> => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true });

      await axios.post(`/api/rides/${rideId}/cancel`, { reason });
      
      dispatch({ type: 'UPDATE_RIDE_STATUS', payload: { rideId, status: 'CANCELLED' } });
      toast.success('Ride cancelled successfully');
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to cancel ride';
      toast.error(errorMessage);
      throw error;
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false });
    }
  };

  // Get ride history
  const getRideHistory = async (): Promise<void> => {
    try {
      dispatch({ type: 'SET_LOADING', payload: true });

      const response = await axios.get('/api/rides/history');
      const rides: Ride[] = response.data;

      dispatch({ type: 'SET_RIDE_HISTORY', payload: rides });
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to load ride history';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      toast.error(errorMessage);
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false });
    }
  };

  // Search for available drivers
  const searchForDrivers = async (location: Location, vehicleType?: string): Promise<Driver[]> => {
    try {
      dispatch({ type: 'SET_SEARCHING', payload: true });

      const params = new URLSearchParams({
        lat: location.latitude.toString(),
        lon: location.longitude.toString(),
        radius: '5', // 5km radius
      });

      if (vehicleType) {
        params.append('vehicleType', vehicleType);
      }

      const response = await axios.get(`/api/drivers/nearby?${params}`);
      const drivers: Driver[] = response.data.drivers || [];

      dispatch({ type: 'SET_SEARCH_RESULTS', payload: drivers });
      return drivers;
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to search for drivers';
      toast.error(errorMessage);
      return [];
    } finally {
      dispatch({ type: 'SET_SEARCHING', payload: false });
    }
  };

  // Update ride status
  const updateRideStatus = async (rideId: number, status: Ride['status']): Promise<void> => {
    try {
      await axios.put(`/api/rides/${rideId}/status`, { status });
      
      dispatch({ type: 'UPDATE_RIDE_STATUS', payload: { rideId, status } });
      
      // Update toast messages based on status
      switch (status) {
        case 'ACCEPTED':
          toast.success('Driver accepted your ride!');
          break;
        case 'STARTED':
          toast.success('Your ride has started!');
          break;
        case 'COMPLETED':
          toast.success('Ride completed! Please rate your experience.');
          break;
        case 'CANCELLED':
          toast.success('Ride was cancelled');
          break;
      }
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to update ride status';
      toast.error(errorMessage);
      throw error;
    }
  };

  // Rate a completed ride
  const rateRide = async (rideId: number, rating: number, review?: string): Promise<void> => {
    try {
      await axios.post(`/api/rides/${rideId}/rate`, { rating, review });
      toast.success('Thank you for your rating!');
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || 'Failed to submit rating';
      toast.error(errorMessage);
      throw error;
    }
  };

  // Clear error
  const clearError = () => {
    dispatch({ type: 'CLEAR_ERROR' });
  };

  const value: RideContextType = {
    state,
    requestRide,
    cancelRide,
    getRideHistory,
    searchForDrivers,
    updateRideStatus,
    rateRide,
    clearError,
  };

  return (
    <RideContext.Provider value={value}>
      {children}
    </RideContext.Provider>
  );
};

// Hook to use ride context
export const useRide = (): RideContextType => {
  const context = useContext(RideContext);
  if (context === undefined) {
    throw new Error('useRide must be used within a RideProvider');
  }
  return context;
};

// Hook to get current ride
export const useCurrentRide = (): Ride | null => {
  const { state } = useRide();
  return state.currentRide;
};

// Hook to check if ride is active
export const useIsRideActive = (): boolean => {
  const { state } = useRide();
  return state.currentRide !== null && 
         ['REQUESTED', 'ACCEPTED', 'STARTED'].includes(state.currentRide.status);
};
