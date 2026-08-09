-- Create Location Master Tables
CREATE TABLE master_states (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE master_districts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    state_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT fk_districts_state FOREIGN KEY (state_id) REFERENCES master_states(id)
);

CREATE TABLE master_cities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    district_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT fk_cities_district FOREIGN KEY (district_id) REFERENCES master_districts(id)
);


-- Update worker_addresses table to add area_village and FKs
ALTER TABLE worker_addresses
    ADD COLUMN area_village VARCHAR(255) NOT NULL,
    ADD CONSTRAINT fk_addresses_state FOREIGN KEY (state_id) REFERENCES master_states(id),
    ADD CONSTRAINT fk_addresses_district FOREIGN KEY (district_id) REFERENCES master_districts(id),
    ADD CONSTRAINT fk_addresses_city FOREIGN KEY (city_id) REFERENCES master_cities(id);
