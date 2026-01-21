-- Add latitude and longitude columns to locations table
-- This allows each location to have its own coordinates for nearby events calculation

ALTER TABLE locations 
ADD COLUMN latitude DOUBLE,
ADD COLUMN longitude DOUBLE;

-- Add index for better performance on distance queries
CREATE INDEX idx_locations_coordinates ON locations(latitude, longitude);
