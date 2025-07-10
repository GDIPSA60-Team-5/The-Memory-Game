
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

        public User AuthenticateUser(string username, string password)
        {
            var user = _userRepository.GetUserByUsername(username);
            if (user == null) throw new UserNotFoundException(username);
            if (!VerifyPassword(password, user.PasswordHash))
                throw new InvalidPasswordException();

            return user; 
        }

        public Boolean VerifyPassword(string password, string hashedPassword)
        {
            return BCrypt.Net.BCrypt.Verify(password, hashedPassword);
        }
    }
}





