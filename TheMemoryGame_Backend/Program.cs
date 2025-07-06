using TheMemoryGame_Backend.Models;

var builder = WebApplication.CreateBuilder(args);

// Add services to the container.
builder.Services.AddControllersWithViews();

// Inject database context into DI-container
builder.Services.AddDbContext<MyDbContext>();

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
