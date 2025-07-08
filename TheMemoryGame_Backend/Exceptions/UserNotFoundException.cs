namespace TheMemoryGame_Backend.Exceptions
{

    public class UserNotFoundException : Exception
    {
        public UserNotFoundException(string username)
            : base($"User '{username}' not found.") { }
    }
}
