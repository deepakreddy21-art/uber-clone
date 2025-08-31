-- Baseline schema for Uber Clone
-- V1__baseline_schema.sql

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'PASSENGER',
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Driver-specific fields (extending users)
ALTER TABLE users ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 8);
ALTER TABLE users ADD COLUMN IF NOT EXISTS longitude DECIMAL(11, 8);
ALTER TABLE users ADD COLUMN IF NOT EXISTS geohash VARCHAR(12);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_location_update TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_available BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_online BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS vehicle_model VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS vehicle_color VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS license_plate VARCHAR(20);
ALTER TABLE users ADD COLUMN IF NOT EXISTS vehicle_type VARCHAR(50);

-- Driver locations table for real-time tracking
CREATE TABLE driver_locations (
    id BIGSERIAL PRIMARY KEY,
    driver_id BIGINT NOT NULL REFERENCES users(id),
    latitude DECIMAL(10, 8) NOT NULL,
    longitude DECIMAL(11, 8) NOT NULL,
    geohash VARCHAR(12) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    heading DECIMAL(5, 2),
    speed DECIMAL(5, 2),
    is_online BOOLEAN DEFAULT TRUE,
    is_available BOOLEAN DEFAULT TRUE,
    vehicle_type VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ride requests table
CREATE TABLE ride_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    pickup_location TEXT NOT NULL,
    dropoff_location TEXT NOT NULL,
    pickup_latitude DECIMAL(10, 8) NOT NULL,
    pickup_longitude DECIMAL(11, 8) NOT NULL,
    dropoff_latitude DECIMAL(10, 8) NOT NULL,
    dropoff_longitude DECIMAL(11, 8) NOT NULL,
    pickup_geohash VARCHAR(12) NOT NULL,
    dropoff_geohash VARCHAR(12) NOT NULL,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    vehicle_type VARCHAR(50),
    estimated_duration INTEGER,
    estimated_distance DECIMAL(10, 2),
    estimated_fare DECIMAL(10, 2),
    special_instructions TEXT,
    passenger_count INTEGER DEFAULT 1,
    payment_method VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Rides table
CREATE TABLE rides (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    driver_id BIGINT REFERENCES users(id),
    pickup_location TEXT NOT NULL,
    dropoff_location TEXT NOT NULL,
    pickup_latitude DECIMAL(10, 8) NOT NULL,
    pickup_longitude DECIMAL(11, 8) NOT NULL,
    dropoff_latitude DECIMAL(10, 8) NOT NULL,
    dropoff_longitude DECIMAL(11, 8) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    base_fare DECIMAL(10, 2),
    distance_fare DECIMAL(10, 2),
    time_fare DECIMAL(10, 2),
    total_fare DECIMAL(10, 2),
    distance DECIMAL(10, 2),
    estimated_duration INTEGER,
    actual_duration INTEGER,
    driver_accept_latitude DECIMAL(10, 8),
    driver_accept_longitude DECIMAL(11, 8),
    cancellation_reason TEXT,
    cancelled_by VARCHAR(20),
    user_rating INTEGER CHECK (user_rating >= 1 AND user_rating <= 5),
    user_review TEXT,
    driver_rating INTEGER CHECK (driver_rating >= 1 AND driver_rating <= 5),
    driver_review TEXT,
    payment_method VARCHAR(50),
    payment_status VARCHAR(20) DEFAULT 'PENDING',
    transaction_id VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payment transactions table
CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    ride_id BIGINT REFERENCES rides(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    stripe_payment_intent_id VARCHAR(255) UNIQUE,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50),
    description TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_geohash ON users(geohash);
CREATE INDEX idx_users_online_available ON users(is_online, is_available) WHERE is_online = TRUE AND is_available = TRUE;

CREATE INDEX idx_driver_locations_driver_id ON driver_locations(driver_id);
CREATE INDEX idx_driver_locations_geohash ON driver_locations(geohash);
CREATE INDEX idx_driver_locations_timestamp ON driver_locations(timestamp);
CREATE INDEX idx_driver_locations_online_available ON driver_locations(is_online, is_available) WHERE is_online = TRUE AND is_available = TRUE;

CREATE INDEX idx_ride_requests_user_id ON ride_requests(user_id);
CREATE INDEX idx_ride_requests_status ON ride_requests(status);
CREATE INDEX idx_ride_requests_pickup_geohash ON ride_requests(pickup_geohash);
CREATE INDEX idx_ride_requests_expires_at ON ride_requests(expires_at);

CREATE INDEX idx_rides_user_id ON rides(user_id);
CREATE INDEX idx_rides_driver_id ON rides(driver_id);
CREATE INDEX idx_rides_status ON rides(status);
CREATE INDEX idx_rides_requested_at ON rides(requested_at);

CREATE INDEX idx_payment_transactions_ride_id ON payment_transactions(ride_id);
CREATE INDEX idx_payment_transactions_user_id ON payment_transactions(user_id);
CREATE INDEX idx_payment_transactions_stripe_id ON payment_transactions(stripe_payment_intent_id);
CREATE INDEX idx_payment_transactions_status ON payment_transactions(status);
