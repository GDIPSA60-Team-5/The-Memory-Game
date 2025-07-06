-- Insert User: Aung
INSERT INTO User (Id, Username, UserType)
VALUES ('11111111-1111-1111-1111-111111111111', 'Aung', 'free');

-- Insert User: Haziq
INSERT INTO User (Id, Username, UserType)
VALUES ('22222222-2222-2222-2222-222222222222', 'Haziq', 'paid');

-- Insert User: Paul
INSERT INTO User (Id, Username, UserType)
VALUES ('33333333-3333-3333-3333-333333333333', 'Paul', 'free');

-- Insert User: Jingjia
INSERT INTO User (Id, Username, UserType)
VALUES ('44444444-4444-4444-4444-444444444444', 'Jingjia', 'paid');

-- Insert User: Simba
INSERT INTO User (Id, Username, UserType)
VALUES ('55555555-5555-5555-5555-555555555555', 'Simba', 'free');

-- Insert User: Zhangrui
INSERT INTO User (Id, Username, UserType)
VALUES ('66666666-6666-6666-6666-666666666666', 'Zhangrui', 'paid');

-- Record for Aung - 21 seconds
INSERT INTO Record (Id, CompletionTime, UserId)
VALUES ('aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 21000, '11111111-1111-1111-1111-111111111111');

-- Record for Haziq - 25 seconds
INSERT INTO Record (Id, CompletionTime, UserId)
VALUES ('aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 25000, '22222222-2222-2222-2222-222222222222');

-- Record for Paul - 22 seconds
INSERT INTO Record (Id, CompletionTime, UserId)
VALUES ('aaaaaaa3-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 22000, '33333333-3333-3333-3333-333333333333');

-- Record for Jingjia - 28 seconds
INSERT INTO Record (Id, CompletionTime, UserId)
VALUES ('aaaaaaa4-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 28000, '44444444-4444-4444-4444-444444444444');

-- Record for Simba - 24 seconds
INSERT INTO Record (Id, CompletionTime, UserId)
VALUES ('aaaaaaa5-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 24000, '55555555-5555-5555-5555-555555555555');

-- Record for Zhangrui - 27 seconds
INSERT INTO Record (Id, CompletionTime, UserId)
VALUES ('aaaaaaa6-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 27000, '66666666-6666-6666-6666-666666666666');
