
using TheMemoryGame_Backend.DataAccess;
using TheMemoryGame_Backend.Exceptions;
using TheMemoryGame_Backend.Models;

namespace TheMemoryGame_Backend.Services
{
    public class UserService
    {
        private readonly UserRepository _userRepository;
        private readonly ILogger<UserService> _logger;

        public UserService(UserRepository userRepository, ILogger<UserService> logger)
        {
            _userRepository = userRepository;
            _logger = logger;
        }

        public User? GetUserByUsername(string username)
        {
            _logger.LogDebug("Attempting to retrieve user with username: {Username}", username);
            var user = _userRepository.GetUserByUsername(username);

            if (user == null)
            {
                _logger.LogWarning("User not found: {Username}", username);
            }
            else
            {
                _logger.LogDebug("User found: {Username} (ID: {UserId})", user.Username, user.Id);
            }

            return user;
        }

        public bool ValidateUserCredentials(string username, string password)
        {
            var user = _userRepository.GetUserByUsername(username);

            if (user == null)
            {
                _logger.LogDebug($"User not found: {username}");
                throw new UserNotFoundException(username);
            }

            _logger.LogDebug($"Found user: Username={user.Username}, PasswordHash={user.PasswordHash}");
            _logger.LogDebug($"Input password (plaintext): {password}");

            bool isPasswordValid = BCrypt.Net.BCrypt.Verify(password, user.PasswordHash);

            if (!isPasswordValid)
            {
                _logger.LogDebug($"Password verification failed for user: {user.Username}");
                throw new InvalidPasswordException();
            }

            _logger.LogDebug($"Password verified successfully for user: {user.Username}");
            return true;
        }
    }
}

//aung123 $2a$12$5JZ7Qw8eN1XmYhFpVtB6E.9zTkL2vCxR3DnGfHjKlMnOpQrSsTu
//haziq123	$2a$12$8KpL3vR2sDfGhJiNlOo.1uXyZbC4dE5F6G7H8I9J0K1L2M3N4O5P
//paul123	$2a$12$WXcVbNmKjHfGdSe.QwErZ.1a2b3c4d5e6f7g8h9i0j1k2l3m4n5o
//jingjia123	$2a$12$3RtYuIoPqAsDfGhJkLzZx.7w8e9r0t1y2u3v4w5x6y7z8A9B0C1D
//simba123	$2a$12$LmNoPqRsTuVwXyZ012345.6a7b8c9d0e1f2g3h4i5j6k7l8m9n
//zhangrui123	$2a$12$BcDeFgHiJkLmNoPqRsTuV.1w2x3y4z5A6B7C8D9E0F1G2H3I4J5



