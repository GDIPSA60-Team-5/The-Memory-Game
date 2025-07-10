-- Insert User: Aung
INSERT INTO User (Id, Username, PasswordHash, UserType)
VALUES ('11111111-1111-1111-1111-111111111111', 'Aung', '$2a$12$jdnFClWhMoWJcyvQ3fxGnurs2ETNKbGEjC7n7eEOGyM1nyKGYheja', 'free');

-- Insert User: Haziq
INSERT INTO User (Id, Username, PasswordHash, UserType)
VALUES ('22222222-2222-2222-2222-222222222222', 'Haziq', '$2a$12$.VLD5PLJVaOL9Gt/x9q1TutmI2hBtmtvvy46FSYyg2kVApBP9Uula', 'paid');

-- Insert User: Paul
INSERT INTO User (Id, Username, PasswordHash, UserType)
VALUES ('33333333-3333-3333-3333-333333333333', 'Paul', '$2a$12$nfKENxDQmI0GvaYtzaiGt.DIIJEoDV7xkNsUOOySOJTPCqO6erKNe', 'free');

-- Insert User: Jingjia
INSERT INTO User (Id, Username, PasswordHash, UserType)
VALUES ('44444444-4444-4444-4444-444444444444', 'Jingjia', '$2a$12$CZLDY9SsD8vKYbr60u1Y9Op5dRVMWFNGkHpMeWmS86Tl0KXTgmtaa', 'paid');

-- Insert User: Simba
INSERT INTO User (Id, Username, PasswordHash, UserType)
VALUES ('55555555-5555-5555-5555-555555555555', 'Simba', '$2a$12$.FdLMrybWJsSqJ3TBKD9z.P9Wm59eryhTcUx68Hme60E8v7x/uwy2', 'free');

-- Insert User: Zhangrui
INSERT INTO User (Id, Username, PasswordHash, UserType)
VALUES ('66666666-6666-6666-6666-666666666666', 'Zhangrui', '$2a$12$oCtcb6.tcZsfp3m/w5QwXOhMwiw26Zgr3.dv8NORvFpnNVsL7Nage', 'paid');

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