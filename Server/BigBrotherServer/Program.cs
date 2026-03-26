using System.Text.Json;
using BigBrotherServer.Hubs;
using BigBrotherServer.Services;

var builder = WebApplication.CreateBuilder(args);

// Add services - use PascalCase JSON to match Android & Web clients
builder.Services.AddControllers()
    .AddJsonOptions(o =>
    {
        o.JsonSerializerOptions.PropertyNamingPolicy = null;
        o.JsonSerializerOptions.PropertyNameCaseInsensitive = true;
    });
builder.Services.AddSignalR()
    .AddJsonProtocol(o => o.PayloadSerializerOptions.PropertyNamingPolicy = null);
builder.Services.AddSingleton<ILocationMgr, LocationMgr>();

// CORS - allow Android app and web clients
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyHeader()
              .AllowAnyMethod()
              .AllowCredentials()
              .SetIsOriginAllowed(_ => true);
    });
});

var app = builder.Build();

app.UseCors();
app.MapControllers();
app.MapHub<BigBrotherHub>("/bigbrotherhub");

// Listen on all interfaces - port 26500
app.Urls.Add("http://0.0.0.0:26500");

Console.WriteLine("BigBrother Server starting on http://0.0.0.0:26500");
Console.WriteLine("SignalR Hub: /bigbrotherhub");
Console.WriteLine("REST API:   /api/maps/addMarker");

app.Run();
