using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace TheMemoryGame_Backend.Models
{
    public class RecordDTO
    {
        public string Name { get; set; }
        public long? completionTime { get; set; }
    }
}