
using MySqlConnector;
using TheMemoryGame_Backend.Models;

namespace TheMemoryGame_Backend.DataAccess
{
    public class UserRepository
    {
        private readonly DatabaseHelper _dbHelper;

        public UserRepository(DatabaseHelper dbHelper)
        {
            _dbHelper = dbHelper;
        }

        public User? GetUserByUsername(string username)
        {
            using (var connection = new MySqlConnection(_dbHelper.GetConnectionString()))
            {
                connection.Open();
                var command = new MySqlCommand("SELECT * FROM User WHERE Username = @Username", connection);
                command.Parameters.AddWithValue("@Username", username);
                using (var reader = command.ExecuteReader())
                {
                    if (reader.Read())
                    {
                        return new User
                        {
                            Id = reader.GetString("Id"),
                            Username = reader.GetString("Username"),
                            PasswordHash = reader.GetString("PasswordHash")
                        };
                    }
                }
            }
            return null;
        }
    }
}