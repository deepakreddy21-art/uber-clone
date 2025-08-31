-- Seed demo data for Uber Clone
-- V2__seed_demo_data.sql

-- Demo Rider (Chicago coordinates)
INSERT INTO users (
    email, 
    password, 
    first_name, 
    last_name, 
    phone_number, 
    role, 
    account_status,
    latitude,
    longitude,
    geohash
) VALUES (
    'rider@demo.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password
    'John',
    'Rider',
    '+1-555-0101',
    'PASSENGER',
    'ACTIVE',
    41.8781,
    -87.6298,
    'dp3wj0'
) ON CONFLICT (email) DO NOTHING;

-- Demo Driver (Chicago coordinates)
INSERT INTO users (
    email, 
    password, 
    first_name, 
    last_name, 
    phone_number, 
    role, 
    account_status,
    latitude,
    longitude,
    geohash,
    is_available,
    is_online,
    vehicle_model,
    vehicle_color,
    license_plate,
    vehicle_type
) VALUES (
    'driver@demo.com',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- password: password
    'Mike',
    'Driver',
    '+1-555-0202',
    'DRIVER',
    'ACTIVE',
    41.8781,
    -87.6298,
    'dp3wj0',
    TRUE,
    TRUE,
    'Toyota Camry',
    'Silver',
    'ABC123',
    'STANDARD'
) ON CONFLICT (email) DO NOTHING;

-- Demo Driver Location
INSERT INTO driver_locations (
    driver_id,
    latitude,
    longitude,
    geohash,
    is_online,
    is_available,
    vehicle_type
) 
SELECT 
    u.id,
    u.latitude,
    u.longitude,
    u.geohash,
    u.is_online,
    u.is_available,
    u.vehicle_type
FROM users u 
WHERE u.email = 'driver@demo.com' 
AND u.role = 'DRIVER'
ON CONFLICT DO NOTHING;
