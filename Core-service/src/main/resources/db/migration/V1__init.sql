-- ==========================================================
-- V1__init.sql
-- CORE SERVICE DATABASE: core_db
-- ==========================================================

/* ==========================================================
   MASTER TABLES
========================================================== */

CREATE TABLE IF NOT EXISTS tour_categories (
                                               id BIGSERIAL PRIMARY KEY,
                                               name VARCHAR(120) UNIQUE NOT NULL
    );

CREATE TABLE IF NOT EXISTS transportations (
                                               id BIGSERIAL PRIMARY KEY,
                                               type VARCHAR(50) UNIQUE NOT NULL
    );


/* ==========================================================
   TOURS
========================================================== */

CREATE TABLE IF NOT EXISTS tours (
                                     id BIGSERIAL PRIMARY KEY,

                                     category_id BIGINT REFERENCES tour_categories(id),
    transportation_id BIGINT REFERENCES transportations(id),

    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE,

    description TEXT,

    duration_days INT,

    status VARCHAR(20) DEFAULT 'ACTIVE',

    is_hot BOOLEAN DEFAULT FALSE,

    base_price DECIMAL(15,2),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );


/* ==========================================================
   TOUR IMAGES
========================================================== */

CREATE TABLE IF NOT EXISTS tour_images (
                                           id BIGSERIAL PRIMARY KEY,

                                           tour_id BIGINT NOT NULL REFERENCES tours(id) ON DELETE CASCADE,

    image_url TEXT NOT NULL,

    alt_text VARCHAR(255),

    is_cover BOOLEAN DEFAULT FALSE,

    sort_order INT DEFAULT 0,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );


/* ==========================================================
   DESTINATIONS
========================================================== */

CREATE TABLE IF NOT EXISTS destinations (
                                            id BIGSERIAL PRIMARY KEY,

                                            city_name VARCHAR(100),
    region VARCHAR(100),
    country VARCHAR(100)
    );

CREATE TABLE IF NOT EXISTS tour_destinations (
                                                 tour_id BIGINT NOT NULL REFERENCES tours(id) ON DELETE CASCADE,
    destination_id BIGINT NOT NULL REFERENCES destinations(id) ON DELETE CASCADE,

    PRIMARY KEY (tour_id, destination_id)
    );


/* ==========================================================
   DEPARTURES
========================================================== */

CREATE TABLE departures (
                            id BIGSERIAL PRIMARY KEY,
                            tour_id BIGINT REFERENCES tours(id),
                            start_date TIMESTAMP NOT NULL,
                            end_date TIMESTAMP,
                            max_slots INT NOT NULL,
                            booked_slots INT DEFAULT 0,
                            status VARCHAR(20) DEFAULT 'OPEN'
);


/* ==========================================================
   PRICE CONFIGS
========================================================== */

CREATE TABLE IF NOT EXISTS price_configs (
                                             id BIGSERIAL PRIMARY KEY,

                                             departure_id BIGINT UNIQUE NOT NULL REFERENCES departures(id) ON DELETE CASCADE,

    adult_price DECIMAL(15,2),
    child_10_14_price DECIMAL(15,2),
    child_4_9_price DECIMAL(15,2),
    baby_price DECIMAL(15,2),

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );


/* ==========================================================
   DYNAMIC PRICING RULES
========================================================== */

CREATE TABLE IF NOT EXISTS pricing_rules (
                                             id BIGSERIAL PRIMARY KEY,

                                             departure_id BIGINT NOT NULL REFERENCES departures(id) ON DELETE CASCADE,

    rule_name VARCHAR(100),

    rule_type VARCHAR(50),

    adjustment_type VARCHAR(20),

    adjustment_value DECIMAL(15,2),

    min_days_before INT,
    max_days_before INT,

    min_slots_left INT,
    max_slots_left INT,

    valid_from TIMESTAMP,
    valid_to TIMESTAMP,

    priority INT DEFAULT 1,

    is_active BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );


/* ==========================================================
   PROMOTIONS
========================================================== */

CREATE TABLE IF NOT EXISTS promotions (
                                          id BIGSERIAL PRIMARY KEY,

                                          code VARCHAR(50) UNIQUE NOT NULL,

    discount_percent DECIMAL(5,2),
    max_discount DECIMAL(15,2),

    usage_limit INT,
    used_count INT DEFAULT 0,

    valid_from TIMESTAMP,
    valid_to TIMESTAMP,

    is_active BOOLEAN DEFAULT TRUE
    );

CREATE TABLE IF NOT EXISTS tour_promotions (
                                               tour_id BIGINT NOT NULL REFERENCES tours(id) ON DELETE CASCADE,
    promotion_id BIGINT NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,

    PRIMARY KEY (tour_id, promotion_id)
    );


/* ==========================================================
   WISHLISTS
========================================================== */

CREATE TABLE IF NOT EXISTS wishlists (
                                         id BIGSERIAL PRIMARY KEY,

                                         user_id INT NOT NULL,

                                         tour_id BIGINT NOT NULL REFERENCES tours(id) ON DELETE CASCADE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );


/* ==========================================================
   INDEXES
========================================================== */

CREATE INDEX IF NOT EXISTS idx_tours_slug ON tours(slug);
CREATE INDEX IF NOT EXISTS idx_tours_status ON tours(status);

CREATE INDEX IF NOT EXISTS idx_departures_tour_id ON departures(tour_id);
CREATE INDEX IF NOT EXISTS idx_departures_start_date ON departures(start_date);

CREATE INDEX IF NOT EXISTS idx_tour_images_tour_id ON tour_images(tour_id);

CREATE INDEX IF NOT EXISTS idx_pricing_rules_departure_id ON pricing_rules(departure_id);

CREATE INDEX IF NOT EXISTS idx_wishlists_user_id ON wishlists(user_id);
CREATE INDEX IF NOT EXISTS idx_wishlists_tour_id ON wishlists(tour_id);