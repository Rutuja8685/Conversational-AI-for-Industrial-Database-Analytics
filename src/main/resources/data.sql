-- Seed Departments
INSERT IGNORE INTO departments (id, name, code, location, budget) VALUES 
(1, 'Industrial Internet of Things (IIoT)', 'DEPT-IIOT', 'Building A - Floor 2', 450000.00),
(2, 'Facilities & Infrastructure', 'DEPT-FAC', 'Building B - Basement', 250000.00),
(3, 'Quality Assurance & Testing', 'DEPT-QA', 'Building C - Floor 1', 180000.00);

-- Seed Users with Passwords and RBAC Roles
INSERT IGNORE INTO users (id, username, password, full_name, email, role, department_id) VALUES 
(1, 'sarah_admin', 'admin123', 'Sarah Jenkins', 'sarah.jenkins@factory.io', 'ADMIN', 1),
(2, 'alex_engineer', 'engineer123', 'Alex Rivera', 'alex.rivera@factory.io', 'ENGINEER', 1),
(3, 'sam_viewer', 'viewer123', 'Sam Taylor', 'sam.taylor@factory.io', 'VIEWER', 2);

-- Seed Sensors
INSERT IGNORE INTO sensors (id, sensor_code, name, sensor_type, location, status, last_reading, department_id) VALUES 
(1, 'TEMP-101', 'HVAC Thermal Sensor North', 'Temperature', 'Building A - Zone 1', 'ACTIVE', 24.50, 1),
(2, 'TEMP-102', 'Boiler Thermal Probe', 'Temperature', 'Building B - Boiler Room', 'OFFLINE', 88.20, 2),
(3, 'PRES-201', 'Main Hydraulic Pressure Line', 'Pressure', 'Building A - Heavy Ops', 'ACTIVE', 142.80, 1),
(4, 'VIB-301', 'Conveyor Motor 3 Vibration', 'Vibration', 'Building C - Assembly', 'MAINTENANCE', 12.40, 3),
(5, 'FLOW-401', 'Coolant Fluid Flow Meter', 'Flow', 'Building A - Zone 3', 'FAULT', 0.00, 1);

-- Seed Maintenance Logs
INSERT IGNORE INTO maintenance_logs (id, sensor_id, user_id, log_date, description, cost, status) VALUES 
(1, 2, 2, '2026-08-15 10:30:00', 'Replaced failed thermocouple probe on Boiler Probe', 650.00, 'COMPLETED'),
(2, 4, 2, '2026-09-01 14:15:00', 'Motor bearing lubrication and dynamic alignment check', 1200.00, 'IN_PROGRESS'),
(3, 5, 1, '2026-09-10 09:00:00', 'Emergency flow valve replacement and sensor recalibration', 3400.00, 'PENDING'),
(4, 1, 2, '2026-09-18 16:45:00', 'Routine annual calibration and dust clearing', 150.00, 'COMPLETED');
