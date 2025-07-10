using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;

namespace TheMemoryGame_Backend.Models
{
    using Microsoft.EntityFrameworkCore;

    public class MyDbContext(DbContextOptions<MyDbContext> options) : DbContext(options)
    {
        public DbSet<User> User { get; set; }
        public DbSet<Record> Record { get; set; }
    }

}