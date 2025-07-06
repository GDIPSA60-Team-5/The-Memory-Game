using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace TheMemoryGame_Backend.Models
{
    public class Record
    {
        public Record()
        {
            Id = Guid.NewGuid().ToString();
        }
        public String Id { get; set; }
        public long? CompletionTime { get; set; }
        public virtual User? User { get; set; } 
    }
}