using Microsoft.Extensions.Configuration;
using MySql.Data.MySqlClient;

namespace TheMemoryGame_Backend.DataAccess
{
    public class DatabaseHelper
    {
        private readonly string _connectionString;


        public DatabaseHelper(IConfiguration configuration)
        {
            _connectionString = configuration.GetConnectionString("DefaultConnection")
                ?? "";
        }

        public string GetConnectionString() => _connectionString;
    }
}