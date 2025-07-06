using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace TheMemoryGame_Backend.Models
{
    public class User
    {
        public User()
        {
            Id = Guid.NewGuid().ToString();
        }
        public String Id { get; set; }
        public String? Username { get; set; }
        public String? Password { get; set; }
        public String? UserType { get; set; }
        public virtual ICollection<Record>? Record { get; set; }
    }
}