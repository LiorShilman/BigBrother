using BigBrotherServer.Models;

namespace BigBrotherServer.Services;

public interface ILocationMgr
{
    void AddMarker(Marker marker);
    List<Marker> ClearAll();
    List<Marker> GetMarkers();
}
