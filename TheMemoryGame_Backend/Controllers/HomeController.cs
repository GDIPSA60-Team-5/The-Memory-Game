using System.Diagnostics;
using Microsoft.AspNetCore.Mvc;
using TheMemoryGame_Backend.Models;

namespace TheMemoryGame_Backend.Controllers;

public class HomeController : Controller
{
    private readonly MyDbContext db;
    public HomeController(MyDbContext db) {
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
}
