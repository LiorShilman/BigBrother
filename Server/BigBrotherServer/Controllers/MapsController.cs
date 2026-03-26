using BigBrotherServer.Models;
using BigBrotherServer.Services;
using Microsoft.AspNetCore.Mvc;

namespace BigBrotherServer.Controllers;

[ApiController]
[Route("api/[controller]")]
public class MapsController : ControllerBase
{
    private readonly ILocationMgr _locationMgr;

    public MapsController(ILocationMgr locationMgr)
    {
        _locationMgr = locationMgr;
    }

    // GET: api/maps
    [HttpGet]
    public ActionResult<List<Marker>> Get()
    {
        return Ok(_locationMgr.GetMarkers());
    }

    // POST: api/maps/addMarker
    [HttpPost("addMarker")]
    public ActionResult<Marker> AddMarker([FromBody] Marker marker)
    {
        Console.WriteLine($"AddMarker: {marker.Name}, Lat={marker.Lat}, Long={marker.Long}, Battery={marker.Battery}");
        _locationMgr.AddMarker(marker);
        return Ok(marker);
    }

    // DELETE: api/maps/deleteAll
    [HttpDelete("deleteAll")]
    public ActionResult<List<Marker>> DeleteAll()
    {
        return Ok(_locationMgr.ClearAll());
    }
}
