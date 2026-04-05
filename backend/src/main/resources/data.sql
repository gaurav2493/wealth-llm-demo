-- Default admin: admin / admin123
INSERT INTO admin (username, password_hash)
VALUES ('admin', '$2a$10$ka9JnZOdSqIW/EEKZM3QwOGDP0WJuD4pbEF6iDhpTTY89p1HwiHMm')
ON CONFLICT (username) DO NOTHING;
