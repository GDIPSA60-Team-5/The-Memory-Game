using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;

namespace TheMemoryGame_Backend.Models
{
    public class MyDbContext : DbContext
    {
        public MyDbContext() { }
        protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
        {
            optionsBuilder.UseMySql(
                // Provide database connection string
                "server=localhost;user=root;password=root;database=TheMemoryGame;",
                new MySqlServerVersion(new Version(8, 0, 41))
            );
            optionsBuilder.UseLazyLoadingProxies();
        }
        // Our database tables
        public DbSet<User> User { get; set; }
        public DbSet<Record> Record { get; set; }
    }
}