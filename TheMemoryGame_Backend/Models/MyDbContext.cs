using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;

namespace TheMemoryGame_Backend.Models
{
    public class MyDbContext : DbContext
    {
        public MyDbContext() { }

        private readonly IConfiguration _configuration;
        public MyDbContext(IConfiguration configuration)
        {
            _configuration = configuration;
        }

        protected override void OnConfiguring(DbContextOptionsBuilder optionsBuilder)
        {
            if (!optionsBuilder.IsConfigured)
            {
                var connectionString = _configuration.GetConnectionString("DefaultConnection");
                optionsBuilder.UseMySql(
                    connectionString,
                    new MySqlServerVersion(new Version(8, 0, 41))
                );
                optionsBuilder.UseLazyLoadingProxies();
            }
        }

        // Our database tables
        public DbSet<User> User { get; set; }
        public DbSet<Record> Record { get; set; }
    }
}