using Microsoft.EntityFrameworkCore;
using TheMemoryGame_Backend.Models;

namespace TheMemoryGame_Backend.DataAccess
{
    public class UserRepository
    {
        private readonly MyDbContext _dbContext;

        public UserRepository(MyDbContext dbContext)
        {
            _dbContext = dbContext;
        }

        public User? GetUserByUsername(string username)
        {
            return _dbContext.User
                .AsNoTracking()
                .FirstOrDefault(u => u.Username == username);
        }
    }
}