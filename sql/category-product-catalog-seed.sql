-- Category-wise starter catalog for VR Technologies.
-- Run after the schema exists. Images are placeholders; replace them from the admin image uploader later.

INSERT INTO categories (name, slug, icon_url, compare_fields)
SELECT x.name, x.slug, x.icon_url, x.compare_fields
FROM (
    SELECT 'Laptops' name, 'laptops' slug, NULL icon_url, 'brandName,categoryName,price,originalPrice,discountPercent,processor,processorGeneration,ramGb,storageGb,storageType,displaySize,displayType,os,graphicsCard,battery,weight,productCondition,warrantyMonths,stockQuantity,available' compare_fields
    UNION ALL SELECT 'MacBooks', 'macbooks', NULL, 'brandName,categoryName,price,originalPrice,discountPercent,processor,processorGeneration,ramGb,storageGb,storageType,displaySize,displayType,os,graphicsCard,battery,weight,productCondition,warrantyMonths,stockQuantity,available'
    UNION ALL SELECT 'Desktops', 'desktops', NULL, 'brandName,categoryName,price,originalPrice,discountPercent,processor,processorGeneration,ramGb,storageGb,storageType,os,graphicsCard,weight,productCondition,warrantyMonths,stockQuantity,available'
    UNION ALL SELECT 'Workstations', 'workstations', NULL, 'brandName,categoryName,price,originalPrice,discountPercent,processor,processorGeneration,ramGb,storageGb,storageType,os,graphicsCard,weight,productCondition,warrantyMonths,stockQuantity,available'
    UNION ALL SELECT 'Monitors', 'monitors', NULL, 'brandName,categoryName,price,originalPrice,discountPercent,displaySize,displayType,weight,productCondition,warrantyMonths,stockQuantity,available'
    UNION ALL SELECT 'Accessories', 'accessories', NULL, 'brandName,categoryName,price,originalPrice,discountPercent,productCondition,warrantyMonths,stockQuantity,available'
) x
WHERE NOT EXISTS (SELECT 1 FROM categories c WHERE c.name = x.name);

UPDATE categories
SET compare_fields = CASE name
    WHEN 'Laptops' THEN 'brandName,categoryName,price,originalPrice,discountPercent,processor,processorGeneration,ramGb,storageGb,storageType,displaySize,displayType,os,graphicsCard,battery,weight,productCondition,warrantyMonths,stockQuantity,available'
    WHEN 'MacBooks' THEN 'brandName,categoryName,price,originalPrice,discountPercent,processor,processorGeneration,ramGb,storageGb,storageType,displaySize,displayType,os,graphicsCard,battery,weight,productCondition,warrantyMonths,stockQuantity,available'
    WHEN 'Desktops' THEN 'brandName,categoryName,price,originalPrice,discountPercent,processor,processorGeneration,ramGb,storageGb,storageType,os,graphicsCard,weight,productCondition,warrantyMonths,stockQuantity,available'
    WHEN 'Workstations' THEN 'brandName,categoryName,price,originalPrice,discountPercent,processor,processorGeneration,ramGb,storageGb,storageType,os,graphicsCard,weight,productCondition,warrantyMonths,stockQuantity,available'
    WHEN 'Monitors' THEN 'brandName,categoryName,price,originalPrice,discountPercent,displaySize,displayType,weight,productCondition,warrantyMonths,stockQuantity,available'
    WHEN 'Accessories' THEN 'brandName,categoryName,price,originalPrice,discountPercent,productCondition,warrantyMonths,stockQuantity,available'
    ELSE compare_fields
END
WHERE name IN ('Laptops', 'MacBooks', 'Desktops', 'Workstations', 'Monitors', 'Accessories');

INSERT INTO brands (name, logo_url)
SELECT x.name, NULL
FROM (
    SELECT 'Dell' name UNION ALL SELECT 'HP' UNION ALL SELECT 'Lenovo' UNION ALL SELECT 'Apple'
    UNION ALL SELECT 'Acer' UNION ALL SELECT 'Asus' UNION ALL SELECT 'Samsung' UNION ALL SELECT 'LG'
    UNION ALL SELECT 'BenQ' UNION ALL SELECT 'Logitech' UNION ALL SELECT 'WD' UNION ALL SELECT 'Crucial'
    UNION ALL SELECT 'TP-Link' UNION ALL SELECT 'Zebronics'
) x
WHERE NOT EXISTS (SELECT 1 FROM brands b WHERE b.name = x.name);

INSERT INTO products (
    title, brand_id, category_id, model_number, processor, processor_generation, ram_gb, storage_gb, storage_type,
    display_size, display_type, os, graphics_card, battery, weight, warranty_months, warranty_summary, return_days,
    sku, serial_number, product_condition, product_status, price, original_price, discount_percent, stock_quantity,
    available, featured, best_seller, today_deal, display_order, seo_title, seo_description, seo_keywords,
    low_stock_threshold, description, custom_attributes, created_at, updated_at
)
SELECT
    x.title, b.id, c.id, x.model_number, x.processor, x.processor_generation, x.ram_gb, x.storage_gb, x.storage_type,
    x.display_size, x.display_type, x.os, x.graphics_card, x.battery, x.weight, x.warranty_months,
    CONCAT(x.warranty_months, ' months VR Technologies carry-in warranty'), 7,
    x.sku, CONCAT(x.sku, '-SEED'), x.product_condition, 'ACTIVE', x.price, x.original_price, x.discount_percent,
    x.stock_quantity, b'1', x.featured, x.best_seller, x.today_deal, x.display_order, x.title,
    CONCAT(x.title, ' available at VR Technologies with verified quality check and warranty.'),
    CONCAT(x.brand_name, ', ', x.category_name, ', refurbished, VR Technologies'), 3,
    x.description, '{}', NOW(), NOW()
FROM (
    SELECT 'Laptops' category_name, 'Dell' brand_name, 'Dell Latitude 5420 | Refurbished' title, 'Latitude 5420' model_number, 'Intel Core i5' processor, '11th Gen' processor_generation, 16 ram_gb, 512 storage_gb, 'NVMe' storage_type, '14 inch' display_size, 'FHD Anti-glare' display_type, 'Windows 11 Pro' os, 'Intel Iris Xe' graphics_card, '4-6 hrs backup' battery, '1.5 kg' weight, 6 warranty_months, 'VR-LAP-DELL-5420' sku, 'EXCELLENT' product_condition, 32999.00 price, 45999.00 original_price, 28 discount_percent, 8 stock_quantity, b'1' featured, b'1' best_seller, b'0' today_deal, 10 display_order, 'Business laptop with fast NVMe storage, clean display, charger, and inspection report.' description
    UNION ALL SELECT 'Laptops', 'HP', 'HP EliteBook 840 G7 | Refurbished', 'EliteBook 840 G7', 'Intel Core i5', '10th Gen', 16, 512, 'SSD', '14 inch', 'FHD IPS', 'Windows 11 Pro', 'Intel UHD', '4-5 hrs backup', '1.4 kg', 6, 'VR-LAP-HP-840G7', 'EXCELLENT', 31999.00, 44999.00, 29, 6, b'1', b'0', b'0', 20, 'Premium metal business laptop for office, students, and multitasking.'
    UNION ALL SELECT 'Laptops', 'Lenovo', 'Lenovo ThinkPad T480 | Refurbished', 'ThinkPad T480', 'Intel Core i5', '8th Gen', 16, 512, 'SSD', '14 inch', 'FHD IPS', 'Windows 11 Pro', 'Intel UHD 620', '5-7 hrs backup', '1.6 kg', 6, 'VR-LAP-LEN-T480', 'GOOD', 25999.00, 36999.00, 30, 10, b'1', b'1', b'1', 30, 'Durable ThinkPad with strong keyboard, business ports, and reliable battery.'
    UNION ALL SELECT 'Laptops', 'Dell', 'Dell Latitude 7490 i7 | Refurbished', 'Latitude 7490', 'Intel Core i7', '8th Gen', 16, 512, 'SSD', '14 inch', 'FHD IPS', 'Windows 11 Pro', 'Intel UHD 620', '4-6 hrs backup', '1.4 kg', 6, 'VR-LAP-DELL-7490', 'GOOD', 28999.00, 41999.00, 31, 7, b'0', b'0', b'1', 40, 'Compact i7 office laptop with SSD performance and clean business design.'
    UNION ALL SELECT 'Laptops', 'HP', 'HP ProBook 440 G8 | Refurbished', 'ProBook 440 G8', 'Intel Core i5', '11th Gen', 8, 512, 'NVMe', '14 inch', 'FHD Anti-glare', 'Windows 11 Pro', 'Intel Iris Xe', '4-6 hrs backup', '1.4 kg', 6, 'VR-LAP-HP-440G8', 'EXCELLENT', 30999.00, 42999.00, 28, 5, b'1', b'0', b'0', 50, 'Slim ProBook for everyday office work, browsing, billing, and online classes.'
    UNION ALL SELECT 'Laptops', 'Lenovo', 'Lenovo ThinkPad X1 Carbon Gen 7 | Refurbished', 'ThinkPad X1 Carbon Gen 7', 'Intel Core i7', '8th Gen', 16, 512, 'SSD', '14 inch', 'FHD IPS', 'Windows 11 Pro', 'Intel UHD 620', '5-7 hrs backup', '1.1 kg', 6, 'VR-LAP-LEN-X1G7', 'EXCELLENT', 38999.00, 59999.00, 35, 4, b'1', b'0', b'0', 60, 'Ultra-light premium ThinkPad for travel, coding, and executive use.'
    UNION ALL SELECT 'Laptops', 'Acer', 'Acer TravelMate P214 | Refurbished', 'TravelMate P214', 'Intel Core i5', '10th Gen', 8, 256, 'SSD', '14 inch', 'FHD LED', 'Windows 11 Pro', 'Intel UHD', '4-5 hrs backup', '1.6 kg', 6, 'VR-LAP-ACER-P214', 'GOOD', 22999.00, 32999.00, 30, 9, b'0', b'0', b'0', 70, 'Budget-friendly business laptop with SSD speed and essential ports.'
    UNION ALL SELECT 'Laptops', 'Asus', 'Asus ExpertBook B1400 | Refurbished', 'ExpertBook B1400', 'Intel Core i5', '11th Gen', 8, 512, 'NVMe', '14 inch', 'FHD Anti-glare', 'Windows 11 Pro', 'Intel Iris Xe', '4-6 hrs backup', '1.5 kg', 6, 'VR-LAP-ASUS-B1400', 'EXCELLENT', 29999.00, 40999.00, 27, 5, b'0', b'0', b'1', 80, 'Professional Asus laptop with fast boot, webcam, and compact build.'
    UNION ALL SELECT 'Laptops', 'Dell', 'Dell Precision 3541 | Refurbished', 'Precision 3541', 'Intel Core i7', '9th Gen', 16, 512, 'SSD', '15.6 inch', 'FHD IPS', 'Windows 11 Pro', 'NVIDIA Quadro P620', '3-5 hrs backup', '2.0 kg', 6, 'VR-LAP-DELL-3541', 'GOOD', 44999.00, 64999.00, 31, 3, b'1', b'0', b'0', 90, 'Mobile workstation laptop for CAD, editing, and engineering workloads.'
    UNION ALL SELECT 'Laptops', 'HP', 'HP ZBook 15 G5 | Refurbished', 'ZBook 15 G5', 'Intel Core i7', '8th Gen', 32, 1024, 'SSD', '15.6 inch', 'FHD IPS', 'Windows 11 Pro', 'NVIDIA Quadro P1000', '3-5 hrs backup', '2.4 kg', 6, 'VR-LAP-HP-Z15G5', 'GOOD', 52999.00, 78999.00, 33, 2, b'1', b'0', b'0', 100, 'Powerful ZBook with 32GB RAM for design, rendering, and heavy multitasking.'

    UNION ALL SELECT 'MacBooks', 'Apple', 'Apple MacBook Air M1 2020 | Refurbished', 'MacBook Air M1 2020', 'Apple M1', 'M1', 8, 256, 'SSD', '13.3 inch', 'Retina', 'macOS', 'Apple 7-core GPU', '8-12 hrs backup', '1.29 kg', 6, 'VR-MAC-AIR-M1-256', 'EXCELLENT', 54999.00, 74999.00, 27, 4, b'1', b'1', b'0', 110, 'Fanless M1 MacBook Air with Retina display, fast SSD, and long battery life.'
    UNION ALL SELECT 'MacBooks', 'Apple', 'Apple MacBook Air M1 2020 512GB | Refurbished', 'MacBook Air M1 2020', 'Apple M1', 'M1', 8, 512, 'SSD', '13.3 inch', 'Retina', 'macOS', 'Apple 7-core GPU', '8-12 hrs backup', '1.29 kg', 6, 'VR-MAC-AIR-M1-512', 'EXCELLENT', 64999.00, 84999.00, 24, 3, b'1', b'0', b'0', 120, 'M1 MacBook Air with upgraded 512GB storage for students and creators.'
    UNION ALL SELECT 'MacBooks', 'Apple', 'Apple MacBook Pro 13 M1 2020 | Refurbished', 'MacBook Pro 13 M1', 'Apple M1', 'M1', 8, 256, 'SSD', '13.3 inch', 'Retina', 'macOS', 'Apple 8-core GPU', '8-12 hrs backup', '1.4 kg', 6, 'VR-MAC-PRO13-M1', 'EXCELLENT', 69999.00, 94999.00, 26, 3, b'1', b'1', b'0', 130, 'MacBook Pro M1 with Touch Bar, Retina display, and strong sustained performance.'
    UNION ALL SELECT 'MacBooks', 'Apple', 'Apple MacBook Pro 13 2019 | Refurbished', 'MacBook Pro 13 2019', 'Intel Core i5', '8th Gen', 8, 256, 'SSD', '13.3 inch', 'Retina', 'macOS', 'Intel Iris Plus', '5-7 hrs backup', '1.37 kg', 6, 'VR-MAC-PRO13-2019', 'GOOD', 45999.00, 64999.00, 29, 5, b'0', b'0', b'1', 140, 'Intel MacBook Pro with Retina display for office, coding, and everyday creative work.'
    UNION ALL SELECT 'MacBooks', 'Apple', 'Apple MacBook Air 2019 | Refurbished', 'MacBook Air 2019', 'Intel Core i5', '8th Gen', 8, 128, 'SSD', '13.3 inch', 'Retina', 'macOS', 'Intel UHD 617', '5-7 hrs backup', '1.25 kg', 6, 'VR-MAC-AIR-2019', 'GOOD', 37999.00, 52999.00, 28, 6, b'0', b'0', b'0', 150, 'Affordable Retina MacBook Air with slim body and charger included.'
    UNION ALL SELECT 'MacBooks', 'Apple', 'Apple MacBook Pro 15 2018 | Refurbished', 'MacBook Pro 15 2018', 'Intel Core i7', '8th Gen', 16, 512, 'SSD', '15.4 inch', 'Retina', 'macOS', 'Radeon Pro 555X', '4-6 hrs backup', '1.83 kg', 6, 'VR-MAC-PRO15-2018', 'GOOD', 72999.00, 99999.00, 27, 2, b'1', b'0', b'0', 160, 'Large-screen MacBook Pro for editing, design, and multitasking.'
    UNION ALL SELECT 'MacBooks', 'Apple', 'Apple MacBook Pro 16 2019 | Refurbished', 'MacBook Pro 16 2019', 'Intel Core i9', '9th Gen', 16, 1024, 'SSD', '16 inch', 'Retina', 'macOS', 'Radeon Pro 5500M', '4-6 hrs backup', '2.0 kg', 6, 'VR-MAC-PRO16-2019', 'GOOD', 94999.00, 139999.00, 32, 2, b'1', b'0', b'0', 170, 'High-performance 16-inch MacBook Pro with 1TB SSD for pro workflows.'
    UNION ALL SELECT 'MacBooks', 'Apple', 'Apple MacBook Air M2 2022 | Refurbished', 'MacBook Air M2 2022', 'Apple M2', 'M2', 8, 256, 'SSD', '13.6 inch', 'Liquid Retina', 'macOS', 'Apple 8-core GPU', '9-13 hrs backup', '1.24 kg', 6, 'VR-MAC-AIR-M2-256', 'EXCELLENT', 78999.00, 99999.00, 21, 3, b'1', b'0', b'1', 180, 'Modern M2 MacBook Air with MagSafe, Liquid Retina display, and premium finish.'
    UNION ALL SELECT 'MacBooks', 'Apple', 'Apple MacBook Pro 14 M1 Pro | Refurbished', 'MacBook Pro 14 M1 Pro', 'Apple M1 Pro', 'M1 Pro', 16, 512, 'SSD', '14.2 inch', 'Liquid Retina XDR', 'macOS', 'Apple 14-core GPU', '8-12 hrs backup', '1.6 kg', 6, 'VR-MAC-PRO14-M1P', 'EXCELLENT', 124999.00, 159999.00, 22, 1, b'1', b'0', b'0', 190, 'Professional 14-inch MacBook Pro for editing, design, and development.'
    UNION ALL SELECT 'MacBooks', 'Apple', 'Apple MacBook Pro 13 M2 2022 | Refurbished', 'MacBook Pro 13 M2', 'Apple M2', 'M2', 8, 256, 'SSD', '13.3 inch', 'Retina', 'macOS', 'Apple 10-core GPU', '8-12 hrs backup', '1.4 kg', 6, 'VR-MAC-PRO13-M2', 'EXCELLENT', 87999.00, 114999.00, 23, 2, b'1', b'0', b'0', 200, 'M2 MacBook Pro with Touch Bar and excellent battery backup.'

    UNION ALL SELECT 'Desktops', 'Dell', 'Dell OptiPlex 3070 SFF | Refurbished', 'OptiPlex 3070', 'Intel Core i5', '9th Gen', 8, 256, 'SSD', NULL, NULL, 'Windows 11 Pro', 'Intel UHD 630', 'Power cable included', '5.3 kg', 6, 'VR-DESK-DELL-3070', 'GOOD', 18999.00, 27999.00, 32, 12, b'1', b'1', b'0', 210, 'Small form factor desktop for billing, office, school labs, and browsing.'
    UNION ALL SELECT 'Desktops', 'Dell', 'Dell OptiPlex 5070 SFF | Refurbished', 'OptiPlex 5070', 'Intel Core i5', '9th Gen', 16, 512, 'SSD', NULL, NULL, 'Windows 11 Pro', 'Intel UHD 630', 'Power cable included', '5.3 kg', 6, 'VR-DESK-DELL-5070', 'EXCELLENT', 24999.00, 34999.00, 29, 8, b'1', b'0', b'1', 220, 'Reliable office desktop with 16GB RAM and 512GB SSD.'
    UNION ALL SELECT 'Desktops', 'HP', 'HP ProDesk 400 G5 SFF | Refurbished', 'ProDesk 400 G5', 'Intel Core i5', '8th Gen', 8, 256, 'SSD', NULL, NULL, 'Windows 11 Pro', 'Intel UHD 630', 'Power cable included', '4.6 kg', 6, 'VR-DESK-HP-400G5', 'GOOD', 17999.00, 25999.00, 31, 10, b'0', b'0', b'0', 230, 'Compact HP desktop for daily office work and counters.'
    UNION ALL SELECT 'Desktops', 'HP', 'HP ProDesk 600 G4 MT | Refurbished', 'ProDesk 600 G4', 'Intel Core i5', '8th Gen', 16, 512, 'SSD', NULL, NULL, 'Windows 11 Pro', 'Intel UHD 630', 'Power cable included', '7.1 kg', 6, 'VR-DESK-HP-600G4', 'EXCELLENT', 23999.00, 33999.00, 29, 7, b'0', b'1', b'0', 240, 'Expandable HP tower desktop with SSD and Windows 11 Pro.'
    UNION ALL SELECT 'Desktops', 'Lenovo', 'Lenovo ThinkCentre M720 SFF | Refurbished', 'ThinkCentre M720', 'Intel Core i5', '8th Gen', 8, 256, 'SSD', NULL, NULL, 'Windows 11 Pro', 'Intel UHD 630', 'Power cable included', '5.5 kg', 6, 'VR-DESK-LEN-M720', 'GOOD', 18499.00, 26999.00, 31, 9, b'0', b'0', b'0', 250, 'ThinkCentre SFF machine for front desk, accounts, and study setups.'
    UNION ALL SELECT 'Desktops', 'Lenovo', 'Lenovo ThinkCentre M920 Tiny | Refurbished', 'ThinkCentre M920 Tiny', 'Intel Core i5', '8th Gen', 16, 512, 'SSD', NULL, NULL, 'Windows 11 Pro', 'Intel UHD 630', 'Adapter included', '1.3 kg', 6, 'VR-DESK-LEN-M920T', 'EXCELLENT', 26999.00, 38999.00, 31, 4, b'1', b'0', b'0', 260, 'Tiny desktop for space-saving counters and monitor-back setups.'
    UNION ALL SELECT 'Desktops', 'Acer', 'Acer Veriton X2660G | Refurbished', 'Veriton X2660G', 'Intel Core i5', '9th Gen', 8, 256, 'SSD', NULL, NULL, 'Windows 11 Pro', 'Intel UHD 630', 'Power cable included', '5.0 kg', 6, 'VR-DESK-ACER-X2660G', 'GOOD', 18999.00, 27999.00, 32, 6, b'0', b'0', b'1', 270, 'Acer business desktop with SSD, LAN, USB ports, and clean cabinet.'
    UNION ALL SELECT 'Desktops', 'Asus', 'Asus ExpertCenter D500SA | Refurbished', 'ExpertCenter D500SA', 'Intel Core i5', '10th Gen', 8, 512, 'SSD', NULL, NULL, 'Windows 11 Pro', 'Intel UHD 630', 'Power cable included', '5.5 kg', 6, 'VR-DESK-ASUS-D500SA', 'EXCELLENT', 26999.00, 36999.00, 27, 5, b'0', b'0', b'0', 280, 'Modern Asus desktop for office software and multitasking.'
    UNION ALL SELECT 'Desktops', 'Dell', 'Dell OptiPlex 7070 MT | Refurbished', 'OptiPlex 7070', 'Intel Core i7', '9th Gen', 16, 512, 'SSD', NULL, NULL, 'Windows 11 Pro', 'Intel UHD 630', 'Power cable included', '7.9 kg', 6, 'VR-DESK-DELL-7070', 'EXCELLENT', 34999.00, 49999.00, 30, 4, b'1', b'0', b'0', 290, 'i7 tower desktop for heavy Excel, business apps, and multi-display work.'
    UNION ALL SELECT 'Desktops', 'Dell', 'Dell Vostro 3681 SFF | Refurbished', 'Vostro 3681', 'Intel Core i3', '10th Gen', 8, 256, 'SSD', NULL, NULL, 'Windows 11 Pro', 'Intel UHD', 'Power cable included', '4.8 kg', 6, 'VR-DESK-DELL-3681', 'GOOD', 15999.00, 23999.00, 33, 11, b'0', b'0', b'0', 300, 'Budget Dell desktop for billing counters, online classes, and web work.'

    UNION ALL SELECT 'Workstations', 'Dell', 'Dell Precision T5810 Xeon | Refurbished', 'Precision T5810', 'Intel Xeon E5', 'v4', 32, 1024, 'SSD', NULL, NULL, 'Windows 11 Pro', 'NVIDIA Quadro K2200', 'Power cable included', '12.4 kg', 6, 'VR-WS-DELL-T5810', 'GOOD', 54999.00, 84999.00, 35, 3, b'1', b'1', b'0', 310, 'Xeon workstation for CAD, 3D, rendering, and engineering software.'
    UNION ALL SELECT 'Workstations', 'Dell', 'Dell Precision T5820 Tower | Refurbished', 'Precision T5820', 'Intel Xeon W', 'W-series', 32, 1024, 'NVMe', NULL, NULL, 'Windows 11 Pro', 'NVIDIA Quadro P2000', 'Power cable included', '15.0 kg', 6, 'VR-WS-DELL-T5820', 'EXCELLENT', 78999.00, 119999.00, 34, 2, b'1', b'0', b'0', 320, 'Professional workstation tower with Quadro graphics and NVMe storage.'
    UNION ALL SELECT 'Workstations', 'HP', 'HP Z240 Tower Workstation | Refurbished', 'HP Z240', 'Intel Core i7', '7th Gen', 16, 512, 'SSD', NULL, NULL, 'Windows 11 Pro', 'NVIDIA Quadro K620', 'Power cable included', '8.8 kg', 6, 'VR-WS-HP-Z240', 'GOOD', 38999.00, 59999.00, 35, 4, b'0', b'1', b'1', 330, 'Affordable workstation for AutoCAD, Photoshop, and office power users.'
    UNION ALL SELECT 'Workstations', 'HP', 'HP Z440 Xeon Workstation | Refurbished', 'HP Z440', 'Intel Xeon E5', 'v3', 32, 1024, 'SSD', NULL, NULL, 'Windows 11 Pro', 'NVIDIA Quadro M2000', 'Power cable included', '11.0 kg', 6, 'VR-WS-HP-Z440', 'GOOD', 58999.00, 89999.00, 34, 3, b'1', b'0', b'0', 340, 'HP Xeon workstation for professional drafting and rendering workflows.'
    UNION ALL SELECT 'Workstations', 'HP', 'HP Z640 Dual CPU Ready | Refurbished', 'HP Z640', 'Intel Xeon E5', 'v4', 64, 1024, 'SSD', NULL, NULL, 'Windows 11 Pro', 'NVIDIA Quadro M4000', 'Power cable included', '17.5 kg', 6, 'VR-WS-HP-Z640', 'GOOD', 89999.00, 129999.00, 31, 1, b'1', b'0', b'0', 350, 'High-memory workstation for rendering, simulation, and multi-app workloads.'
    UNION ALL SELECT 'Workstations', 'Lenovo', 'Lenovo ThinkStation P330 | Refurbished', 'ThinkStation P330', 'Intel Core i7', '9th Gen', 16, 512, 'SSD', NULL, NULL, 'Windows 11 Pro', 'NVIDIA Quadro P1000', 'Power cable included', '10.6 kg', 6, 'VR-WS-LEN-P330', 'EXCELLENT', 52999.00, 78999.00, 33, 3, b'0', b'0', b'1', 360, 'Compact Lenovo workstation for design, architecture, and development.'
    UNION ALL SELECT 'Workstations', 'Lenovo', 'Lenovo ThinkStation P520 | Refurbished', 'ThinkStation P520', 'Intel Xeon W', 'W-series', 32, 1024, 'NVMe', NULL, NULL, 'Windows 11 Pro', 'NVIDIA Quadro P4000', 'Power cable included', '14.5 kg', 6, 'VR-WS-LEN-P520', 'EXCELLENT', 84999.00, 124999.00, 32, 2, b'1', b'0', b'0', 370, 'Strong graphics workstation for CAD, 3D modeling, and editing.'
    UNION ALL SELECT 'Workstations', 'Dell', 'Dell Precision T3620 Tower | Refurbished', 'Precision T3620', 'Intel Core i7', '6th Gen', 16, 512, 'SSD', NULL, NULL, 'Windows 10 Pro', 'NVIDIA Quadro K1200', 'Power cable included', '9.0 kg', 6, 'VR-WS-DELL-T3620', 'GOOD', 34999.00, 54999.00, 36, 4, b'0', b'0', b'0', 380, 'Entry workstation tower for drafting, business apps, and multitasking.'
    UNION ALL SELECT 'Workstations', 'Dell', 'Dell Precision T7810 Dual Xeon | Refurbished', 'Precision T7810', 'Dual Intel Xeon E5', 'v3', 64, 1024, 'SSD', NULL, NULL, 'Windows 11 Pro', 'NVIDIA Quadro M4000', 'Power cable included', '17.0 kg', 6, 'VR-WS-DELL-T7810', 'GOOD', 99999.00, 149999.00, 33, 1, b'1', b'0', b'0', 390, 'Dual Xeon tower for advanced rendering and compute-heavy professional work.'
    UNION ALL SELECT 'Workstations', 'HP', 'HP Z2 G4 Workstation | Refurbished', 'HP Z2 G4', 'Intel Core i7', '8th Gen', 16, 512, 'SSD', NULL, NULL, 'Windows 11 Pro', 'NVIDIA Quadro P620', 'Power cable included', '7.0 kg', 6, 'VR-WS-HP-Z2G4', 'EXCELLENT', 46999.00, 69999.00, 33, 3, b'0', b'0', b'0', 400, 'Compact HP workstation for engineers, editors, and creators.'

    UNION ALL SELECT 'Monitors', 'Dell', 'Dell P2419H 24 inch Monitor | Refurbished', 'P2419H', NULL, NULL, NULL, NULL, NULL, '24 inch', 'FHD IPS', NULL, NULL, 'Power cable included', '3.3 kg', 6, 'VR-MON-DELL-P2419H', 'EXCELLENT', 8999.00, 13999.00, 36, 14, b'1', b'1', b'0', 410, '24-inch Dell IPS monitor with HDMI, VGA, DisplayPort, and height-adjustable stand.'
    UNION ALL SELECT 'Monitors', 'Dell', 'Dell E2216H 22 inch Monitor | Refurbished', 'E2216H', NULL, NULL, NULL, NULL, NULL, '22 inch', 'FHD LED', NULL, NULL, 'Power cable included', '2.9 kg', 6, 'VR-MON-DELL-E2216H', 'GOOD', 5999.00, 9999.00, 40, 18, b'0', b'0', b'0', 420, 'Budget Dell monitor for office, billing, and study desk setups.'
    UNION ALL SELECT 'Monitors', 'HP', 'HP EliteDisplay E243 24 inch | Refurbished', 'EliteDisplay E243', NULL, NULL, NULL, NULL, NULL, '24 inch', 'FHD IPS', NULL, NULL, 'Power cable included', '4.0 kg', 6, 'VR-MON-HP-E243', 'EXCELLENT', 9499.00, 14999.00, 37, 9, b'1', b'0', b'1', 430, 'HP IPS display with slim bezels and ergonomic stand.'
    UNION ALL SELECT 'Monitors', 'Lenovo', 'Lenovo ThinkVision T24i | Refurbished', 'ThinkVision T24i', NULL, NULL, NULL, NULL, NULL, '24 inch', 'FHD IPS', NULL, NULL, 'Power cable included', '3.6 kg', 6, 'VR-MON-LEN-T24I', 'GOOD', 8499.00, 12999.00, 35, 8, b'0', b'0', b'0', 440, 'ThinkVision monitor with IPS viewing and business-grade build.'
    UNION ALL SELECT 'Monitors', 'Samsung', 'Samsung 24 inch LED Monitor | Refurbished', 'S24F350', NULL, NULL, NULL, NULL, NULL, '24 inch', 'FHD LED', NULL, NULL, 'Adapter included', '3.0 kg', 6, 'VR-MON-SAM-S24F350', 'GOOD', 7999.00, 11999.00, 33, 10, b'0', b'0', b'0', 450, 'Samsung slim LED monitor for home and office use.'
    UNION ALL SELECT 'Monitors', 'LG', 'LG 22MP68VQ 22 inch Monitor | Refurbished', '22MP68VQ', NULL, NULL, NULL, NULL, NULL, '22 inch', 'FHD IPS', NULL, NULL, 'Power cable included', '2.8 kg', 6, 'VR-MON-LG-22MP68', 'GOOD', 6999.00, 10999.00, 36, 7, b'0', b'0', b'1', 460, 'LG IPS monitor with HDMI support and clear viewing angles.'
    UNION ALL SELECT 'Monitors', 'Acer', 'Acer V226HQL 21.5 inch Monitor | Refurbished', 'V226HQL', NULL, NULL, NULL, NULL, NULL, '21.5 inch', 'FHD LED', NULL, NULL, 'Power cable included', '2.7 kg', 6, 'VR-MON-ACER-V226', 'FAIR', 4999.00, 8499.00, 41, 12, b'0', b'0', b'0', 470, 'Value monitor for counters, CCTV view, and basic desktop setups.'
    UNION ALL SELECT 'Monitors', 'BenQ', 'BenQ GW2480 24 inch Monitor | Refurbished', 'GW2480', NULL, NULL, NULL, NULL, NULL, '24 inch', 'FHD IPS', NULL, NULL, 'Power cable included', '3.8 kg', 6, 'VR-MON-BENQ-GW2480', 'EXCELLENT', 8999.00, 13999.00, 36, 6, b'1', b'0', b'0', 480, 'BenQ IPS monitor with eye-care features for long work sessions.'
    UNION ALL SELECT 'Monitors', 'Dell', 'Dell UltraSharp U2415 | Refurbished', 'U2415', NULL, NULL, NULL, NULL, NULL, '24 inch', 'WUXGA IPS', NULL, NULL, 'Power cable included', '4.3 kg', 6, 'VR-MON-DELL-U2415', 'GOOD', 11999.00, 18999.00, 37, 5, b'1', b'0', b'0', 490, 'UltraSharp monitor with 1920x1200 resolution and premium color reproduction.'
    UNION ALL SELECT 'Monitors', 'HP', 'HP V24i 24 inch Monitor | Refurbished', 'V24i', NULL, NULL, NULL, NULL, NULL, '24 inch', 'FHD IPS', NULL, NULL, 'Power cable included', '3.4 kg', 6, 'VR-MON-HP-V24I', 'EXCELLENT', 8999.00, 13999.00, 36, 8, b'0', b'0', b'0', 500, 'HP 24-inch IPS monitor for office, learning, and dual-screen productivity.'

    UNION ALL SELECT 'Accessories', 'Logitech', 'Logitech M331 Silent Wireless Mouse', 'M331', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'AA battery included', '91 g', 3, 'VR-ACC-LOG-M331', 'EXCELLENT', 899.00, 1299.00, 31, 25, b'1', b'1', b'0', 510, 'Silent wireless mouse for laptop and desktop users.'
    UNION ALL SELECT 'Accessories', 'Dell', 'Dell KM5221W Wireless Keyboard Mouse Combo', 'KM5221W', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Batteries included', '550 g', 3, 'VR-ACC-DELL-KM5221W', 'EXCELLENT', 1999.00, 2999.00, 33, 14, b'1', b'0', b'1', 520, 'Dell wireless keyboard and mouse combo for office desks.'
    UNION ALL SELECT 'Accessories', 'HP', 'HP USB-C Dock G5', 'USB-C Dock G5', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Adapter included', '680 g', 3, 'VR-ACC-HP-USBCG5', 'GOOD', 5499.00, 8999.00, 39, 6, b'1', b'0', b'0', 530, 'USB-C docking station with display, USB, LAN, and charging support.'
    UNION ALL SELECT 'Accessories', 'Lenovo', 'Lenovo ThinkPad USB-C Dock', 'ThinkPad USB-C Dock', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Adapter included', '720 g', 3, 'VR-ACC-LEN-USBCD', 'GOOD', 4999.00, 8499.00, 41, 7, b'0', b'0', b'0', 540, 'ThinkPad dock for multi-monitor and desk productivity setups.'
    UNION ALL SELECT 'Accessories', 'WD', 'WD Green 240GB SATA SSD', 'WDS240G3G0A', NULL, NULL, NULL, 240, 'SSD', NULL, NULL, NULL, NULL, 'No external power needed', '32 g', 12, 'VR-ACC-WD-240SSD', 'EXCELLENT', 1399.00, 2199.00, 36, 20, b'1', b'1', b'0', 550, '240GB SATA SSD upgrade for laptops and desktops.'
    UNION ALL SELECT 'Accessories', 'Crucial', 'Crucial 8GB DDR4 Laptop RAM', 'CT8G4SFRA266', NULL, NULL, 8, NULL, NULL, NULL, NULL, NULL, NULL, 'Low power DDR4', '20 g', 12, 'VR-ACC-CRU-8DDR4', 'EXCELLENT', 1499.00, 2499.00, 40, 18, b'1', b'0', b'1', 560, '8GB DDR4 SODIMM RAM upgrade for supported laptops.'
    UNION ALL SELECT 'Accessories', 'TP-Link', 'TP-Link Archer T2U WiFi Adapter', 'Archer T2U', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'USB powered', '20 g', 3, 'VR-ACC-TPL-T2U', 'EXCELLENT', 999.00, 1599.00, 38, 16, b'0', b'0', b'0', 570, 'Dual-band USB Wi-Fi adapter for desktops and laptops.'
    UNION ALL SELECT 'Accessories', 'Zebronics', 'Zebronics 1080p USB Webcam', 'ZEB-Crystal Pro', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'USB plug and play', '120 g', 3, 'VR-ACC-ZEB-WEBCAM', 'GOOD', 1199.00, 1999.00, 40, 10, b'0', b'0', b'0', 580, 'Full HD USB webcam for meetings, classes, and recordings.'
    UNION ALL SELECT 'Accessories', 'Dell', 'Dell 65W Laptop Charger', 'LA65NS2-01', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '65W adapter', '260 g', 3, 'VR-ACC-DELL-65W', 'GOOD', 1299.00, 2299.00, 43, 15, b'0', b'0', b'0', 590, 'Original-style Dell 65W charger for compatible Latitude and Inspiron laptops.'
    UNION ALL SELECT 'Accessories', 'HP', 'HP 65W Laptop Charger', 'PPP009L', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '65W adapter', '260 g', 3, 'VR-ACC-HP-65W', 'GOOD', 1299.00, 2299.00, 43, 15, b'0', b'0', b'0', 600, 'HP 65W charger for compatible ProBook, EliteBook, and Pavilion laptops.'
) x
JOIN categories c ON c.name = x.category_name
JOIN brands b ON b.name = x.brand_name
WHERE NOT EXISTS (SELECT 1 FROM products p WHERE p.sku = x.sku);

INSERT INTO product_images (product_id, image_url, public_id, primary_image, sort_order)
SELECT
    p.id,
    CONCAT('https://placehold.co/900x650/e2e8f0/0f172a?text=', REPLACE(REPLACE(REPLACE(LEFT(p.title, 65), ' ', '+'), '|', ''), '/', '-')),
    NULL,
    b'1',
    0
FROM products p
WHERE p.sku LIKE 'VR-%'
  AND NOT EXISTS (
      SELECT 1
      FROM product_images pi
      WHERE pi.product_id = p.id AND pi.primary_image = b'1'
  );

INSERT INTO product_stores (product_id, store_id)
SELECT p.id, s.id
FROM products p
JOIN stores s ON s.id = (SELECT MIN(id) FROM stores WHERE active = b'1')
WHERE p.sku LIKE 'VR-%'
  AND NOT EXISTS (
      SELECT 1
      FROM product_stores ps
      WHERE ps.product_id = p.id AND ps.store_id = s.id
  );

SELECT
    c.name AS category,
    COUNT(p.id) AS seeded_products
FROM categories c
LEFT JOIN products p ON p.category_id = c.id AND p.sku LIKE 'VR-%'
WHERE c.name IN ('Laptops', 'MacBooks', 'Desktops', 'Workstations', 'Monitors', 'Accessories')
GROUP BY c.name
ORDER BY c.name;
