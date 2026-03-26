namespace BigBrotherServer.Models;

public class Marker
{
    public string Name { get; set; } = "";
    public string Telephone { get; set; } = "";
    public double Battery { get; set; }
    public bool IsBatteryPlugged { get; set; }
    public string StreetViewImage_Minus90 { get; set; } = "";
    public string StreetViewImage0 { get; set; } = "";
    public string StreetViewImage90 { get; set; } = "";
    public string StreetViewImage180 { get; set; } = "";
    public string Street { get; set; } = "";
    public double Lat { get; set; }
    public double Long { get; set; }
    public double Alt { get; set; }
    public float Accuracy { get; set; }
    public string Time { get; set; } = "";
}
