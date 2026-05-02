ALTER TABLE stores
    ADD COLUMN IF NOT EXISTS landmark VARCHAR(255) NULL AFTER address,
    ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20) NULL AFTER landmark,
    ADD COLUMN IF NOT EXISTS google_rating DECIMAL(3,1) NULL AFTER video_url,
    ADD COLUMN IF NOT EXISTS google_review_count INT NULL AFTER google_rating;

UPDATE stores
SET
    name = 'VR Technologies - Ameerpet',
    address = 'Plot No. 4, Bhavya Construction, Gayatri Co-op Housing Society, Ground Floor, Maitrivanam, Ameerpet',
    landmark = 'Kumar Basti, Srinivasa Nagar',
    postal_code = '500038',
    city = 'Hyderabad',
    state = 'Telangana',
    phone = '9848641070',
    whatsapp = '9000371070',
    timings = '10:00 AM - 9:00 PM',
    map_link = 'https://maps.google.com/?q=VR+Technologies+Ameerpet+Hyderabad',
    google_rating = 4.6,
    google_review_count = 238,
    active = b'1'
WHERE id = 1;

UPDATE stores
SET
    name = 'VR Technologies - KPHB',
    address = 'MIG-17, Phase 1, Dharma Reddy Colony, KPHB Colony, Kukatpally',
    landmark = 'Near Bhagya Shopping Back Side',
    postal_code = '500072',
    city = 'Hyderabad',
    state = 'Telangana',
    phone = '9000103396',
    whatsapp = '9000103396',
    timings = '10:00 AM - 8:30 PM',
    map_link = 'https://maps.google.com/?q=VR+Technologies+KPHB+Hyderabad',
    google_rating = 4.4,
    google_review_count = 45,
    active = b'1'
WHERE id = 2;

INSERT INTO stores (
    name,
    address,
    landmark,
    postal_code,
    city,
    state,
    phone,
    whatsapp,
    timings,
    map_link,
    image_url,
    video_url,
    google_rating,
    google_review_count,
    active
)
SELECT
    'VR Technologies - Dilsukhnagar',
    'Metro Pillar No. 1524, 1st Floor, Shop No. 18, Dilsukhnagar',
    'Vaibhav Jewellers, LB Nagar Road',
    '500060',
    'Hyderabad',
    'Telangana',
    '9963479888',
    '9963479888',
    '10:00 AM - 9:00 PM',
    'https://maps.google.com/?q=VR+Technologies+Dilsukhnagar+Hyderabad',
    NULL,
    NULL,
    4.7,
    25,
    b'1'
WHERE NOT EXISTS (
    SELECT 1
    FROM stores
    WHERE name = 'VR Technologies - Dilsukhnagar'
);

INSERT INTO stores (
    name,
    address,
    landmark,
    postal_code,
    city,
    state,
    phone,
    whatsapp,
    timings,
    map_link,
    image_url,
    video_url,
    google_rating,
    google_review_count,
    active
)
SELECT
    'VR Technologies - Guntur',
    'Shop No. 6 & 7, Ground Floor, Apple Plaza, 4/11, Brodipet',
    'Beside IOB Bank',
    '522002',
    'Guntur',
    'Andhra Pradesh',
    '9063666310',
    '9063666310',
    '10:00 AM - 9:00 PM',
    'https://maps.google.com/?q=VR+Technologies+Guntur+Brodipet',
    NULL,
    NULL,
    4.4,
    27,
    b'1'
WHERE NOT EXISTS (
    SELECT 1
    FROM stores
    WHERE name = 'VR Technologies - Guntur'
);

SELECT
    id,
    name,
    city,
    state,
    phone,
    google_rating,
    google_review_count,
    active
FROM stores
ORDER BY city ASC, name ASC;
