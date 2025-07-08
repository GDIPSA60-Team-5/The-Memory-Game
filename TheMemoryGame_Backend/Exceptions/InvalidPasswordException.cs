namespace TheMemoryGame_Backend.Exceptions
{
    public class InvalidPasswordException : Exception
    {
        public InvalidPasswordException() 
        : base("Invalid password. Please try again.") { }
    }
}
