-- Add new primary skills requested by user

-- Update existing Mason if it exists to avoid duplicates
UPDATE master_skills SET name = 'Mason (राजमिस्त्री)' WHERE name = 'Mason';

INSERT INTO master_skills (name) VALUES 
('Mason (राजमिस्त्री)'),
('Tile/Marble Mason'),
('Bar Bender'),
('Shuttering Carpenter'),
('Fabricator'),
('POP/False Ceiling Worker'),
('Aluminium & Glass Worker'),
('Heavy Equipment Operator'),
('Road Construction Worker'),
('General Labour')
ON DUPLICATE KEY UPDATE name=name;
