using Microsoft.EntityFrameworkCore;
using TheMemoryGame_Backend.DataAccess;
using TheMemoryGame_Backend.Models;
using TheMemoryGame_Backend.Services;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddControllersWithViews();
builder.Services.AddControllers();

builder.Services.AddSession(options =>
{
    options.IdleTimeout = TimeSpan.FromMinutes(20);
    options.Cookie.HttpOnly = true;
    options.Cookie.IsEssential = true;
});

// Inject database context into DI-container
builder.Services.AddDbContext<MyDbContext>(options =>
    options.UseMySql(
        builder.Configuration.GetConnectionString("DefaultConnection"),
        new MySqlServerVersion(new Version(8, 0, 41))
    ).UseLazyLoadingProxies()
);
builder.Services.AddScoped<UserRepository>();
builder.Services.AddScoped<UserService>();

var app = builder.Build();

// Configure the HTTP request pipeline.
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Home/Error");
    // The default HSTS value is 30 days. You may want to change this for production scenarios, see https://aka.ms/aspnetcore-hsts.
    app.UseHsts();
}

// app.UseHttpsRedirection(); // Disabled this middleware for http connection with android
app.UseStaticFiles();

app.UseSession();

app.UseRouting();

app.UseAuthorization();

app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}");

initDB();

app.Run();

void initDB()
{
    // Create the environment to retrieve our database context
    using (var scope = app.Services.CreateScope())
    {
        // Get database context from DI-container
        var ctx = scope.ServiceProvider.GetRequiredService<MyDbContext>();

        if (!ctx.Database.CanConnect())
        {
            ctx.Database.EnsureCreated();
        }
    }
}
