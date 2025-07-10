using System.Diagnostics;
using Microsoft.AspNetCore.Mvc;
using TheMemoryGame_Backend.Models;
using Microsoft.EntityFrameworkCore;

namespace TheMemoryGame_Backend.Controllers;

public class HomeController : Controller
{
    private readonly MyDbContext db;
    public HomeController(MyDbContext db)
    {
        this.db = db;
    }

    public IActionResult Index()
    {
        var images = new List<string>();
        for (var i = 1; i < 12; i++)
        {
            if (i < 10)
            {
                images.Add("ad_0" + i + ".png");
            }
            else
            {
                images.Add("ad_" + i + ".png");
            }
        }
        ViewData["Images"] = images;
        return View();
    }

    public IActionResult Privacy()
    {
        return View();
    }

    public string SaveCompletionTime(long completionTime)
    {
        // TODO: Check session to get user or user id instead of this
        User? user = db.User.FirstOrDefault(x => x.Username == "Aung");
        if (user == null)
        {
            return "user not found";
        }
        Record record = new Record
        {
            CompletionTime = completionTime,
            User = user
        };
        db.Add(record);
        db.SaveChanges();

        return "saved";
    }

    [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
    public IActionResult Error()
    {
        return View(new ErrorViewModel { RequestId = Activity.Current?.Id ?? HttpContext.TraceIdentifier });
    }

    [HttpGet]
    [Route("api/home/top5")]
    public async Task<ActionResult<IEnumerable<RecordDTO>>> GetTop5()
    {
        var top5 = await db.Record.Where(r => r.User != null).OrderBy(r => r.CompletionTime).Take(5).Select(r => new RecordDTO
        {
            Name = r.User.Username,
            completionTime = r.CompletionTime
        })
        .ToListAsync();

        return Ok(top5);

    }

    [HttpGet]
    [Route("api/home/rank")]
    public async Task<ActionResult<int>> GetRank([FromQuery] long time)
    {
        var rank = await db.Record.CountAsync(r => r.CompletionTime < time);
        return Ok(rank + 1);
    }
}
