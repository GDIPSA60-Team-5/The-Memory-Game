using System.Diagnostics;
using Microsoft.AspNetCore.Mvc;
using TheMemoryGame_Backend.Models;
using Microsoft.EntityFrameworkCore;

namespace TheMemoryGame_Backend.Controllers;

public class HomeController : Controller
{
    private readonly MyDbContext _db;

    public HomeController(MyDbContext db)
    {
        _db = db;
    }

    public IActionResult Index()
    {
        var images = Enumerable.Range(1, 11)
            .Select(i => i < 10 ? $"ad_0{i}.png" : $"ad_{i}.png")
            .ToList();

        ViewData["Images"] = images;
        return View();
    }

    public IActionResult Privacy() => View();

    [HttpPost]
    [Route("api/home/save")]
    public async Task<ActionResult<string>> SaveCompletionTime([FromBody] long completionTime)
    {
        if (completionTime <= 0)
            return BadRequest("Invalid completion time");

        var user = await GetCurrentUserAsync();
        if (user == null)
            return Unauthorized("Not logged in or user not found");

        var record = new Record
        {
            CompletionTime = completionTime,
            User = user
        };

        _db.Record.Add(record);
        await _db.SaveChangesAsync();

        return Ok("saved");
    }

    [ResponseCache(Duration = 0, Location = ResponseCacheLocation.None, NoStore = true)]
    public IActionResult Error()
    {
        var errorModel = new ErrorViewModel
        {
            RequestId = Activity.Current?.Id ?? HttpContext.TraceIdentifier
        };
        return View(errorModel);
    }

    [HttpGet]
    [Route("api/home/top5")]
    public async Task<ActionResult<IEnumerable<RecordDTO>>> GetTop5()
    {
        var top5 = await _db.Record
            .Where(r => r.User != null)
            .OrderBy(r => r.CompletionTime)
            .Take(5)
            .Select(r => new RecordDTO
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
        var rank = await _db.Record.CountAsync(r => r.CompletionTime < time);
        return Ok(rank + 1);
    }

    [HttpGet]
    [Route("api/home/me")]
    public async Task<ActionResult<string>> GetCurrentUsername()
    {
        var user = await GetCurrentUserAsync();
        if (user == null)
            return Unauthorized("Not logged in or user not found");

        return Ok(user.Username);
    }

    [HttpGet]
    [Route("api/home/user/can-see-ads")]
    public async Task<ActionResult<bool>> CanSeeAds()
    {
        var user = await GetCurrentUserAsync();
        // Show ads if user is null or UserType is not "paid"
        return Ok(user == null || user.UserType?.ToLower() != "paid");
    }

    // Helper method to get current logged in user from session
    private async Task<User?> GetCurrentUserAsync()
    {
        var userId = HttpContext.Session.GetString("UserId");
        if (string.IsNullOrEmpty(userId))
            return null;

        return await _db.User.FirstOrDefaultAsync(u => u.Id == userId);
    }
}
