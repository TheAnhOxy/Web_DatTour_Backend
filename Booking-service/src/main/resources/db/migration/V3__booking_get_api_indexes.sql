-- ==============================================
-- Database Migration: V3__booking_get_api_indexes.sql
-- Purpose: Create indexes to optimize GET API queries
-- ==============================================

-- Unique index on booking code (for fast lookup)
CREATE UNIQUE INDEX idx_bookings_code ON bookings(booking_code);

-- Index on user ID (for user's bookings queries)
CREATE INDEX idx_bookings_user_id ON bookings(user_id);

-- Index on status (for status filtering)
CREATE INDEX idx_bookings_status ON bookings(status);

-- Index on created_at DESC (for sorting)
CREATE INDEX idx_bookings_created_at ON bookings(created_at DESC);

-- Composite index for user + status + date (common filter combination)
CREATE INDEX idx_bookings_user_status_created ON bookings(user_id, status, created_at DESC);

-- Index on payment method (for filtering by payment method)
CREATE INDEX idx_bookings_payment_method ON bookings(payment_method);

-- Indexes for related tables
CREATE INDEX idx_passengers_booking_id ON passenger(booking_id);
CREATE INDEX idx_booking_notes_booking_id ON booking_notes(booking_id);
CREATE INDEX idx_cancellations_booking_id ON cancellations(booking_id);

-- Composite index for cleanup task queries
CREATE INDEX idx_bookings_status_payment_created ON bookings(status, payment_method, created_at DESC);
