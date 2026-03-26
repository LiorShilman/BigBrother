using BigBrotherServer.Hubs;
using BigBrotherServer.Models;
using Microsoft.AspNetCore.SignalR;

namespace BigBrotherServer.Services;

public class LocationMgr : ILocationMgr
{
    private readonly Dictionary<string, Marker> _markers = new();
    private readonly IHubContext<BigBrotherHub, IBigBrotherClient> _hubContext;

    public LocationMgr(IHubContext<BigBrotherHub, IBigBrotherClient> hubContext)
    {
        _hubContext = hubContext;
    }

    public void AddMarker(Marker marker)
    {
        _markers[marker.Name] = marker;

        // Notify all web clients
        var allMarkers = GetMarkers();
        _hubContext.Clients.Group(Constants.WEB_GROUP)
            .MarkersUpdated(allMarkers.ToArray());
    }

    public List<Marker> ClearAll()
    {
        _markers.Clear();
        return GetMarkers();
    }

    public List<Marker> GetMarkers()
    {
        return _markers.Values.ToList();
    }
}
