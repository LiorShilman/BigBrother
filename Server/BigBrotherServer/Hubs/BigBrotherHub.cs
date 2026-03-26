using BigBrotherServer.Models;
using Microsoft.AspNetCore.SignalR;
using System.Collections.Concurrent;

namespace BigBrotherServer.Hubs;

public interface IBigBrotherClient
{
    Task MarkersUpdated(Marker[] markers);
    Task SubscribeFinished();
    Task HeadsUpNotification(string title, string message);
}

public class BigBrotherHub : Hub<IBigBrotherClient>
{
    private static readonly ConcurrentDictionary<string, string> UserConnections = new();

    public async Task Subscribe(string name, string channel)
    {
        try
        {
            await Groups.AddToGroupAsync(Context.ConnectionId, channel);
            Console.WriteLine($"Add to group {name}, {Context.ConnectionId}, {channel}");

            if (channel == Constants.WEB_GROUP)
            {
                await Clients.Caller.SubscribeFinished();
                Console.WriteLine($"SubscribeFinished to {Context.ConnectionId}");
            }
            else
            {
                UserConnections[name] = Context.ConnectionId;
                Console.WriteLine($"Add/Update dict {name}, {Context.ConnectionId}");
            }

            Console.WriteLine($"Subscribe: {Context.ConnectionId}, Name: {name}, Channel: {channel}");
        }
        catch (Exception e)
        {
            Console.WriteLine($"Subscribe error: {e.Message}");
        }
    }

    public async Task Unsubscribe(string name, string channel)
    {
        try
        {
            await Groups.RemoveFromGroupAsync(Context.ConnectionId, channel);
            Console.WriteLine($"Remove from group {name}, {Context.ConnectionId}, {channel}");

            if (channel == Constants.ANDROID_GROUP)
            {
                UserConnections.TryRemove(name, out _);
                Console.WriteLine($"Remove from dict {name}, Channel: {channel}");
            }
        }
        catch (Exception e)
        {
            Console.WriteLine($"Unsubscribe error: {e.Message}");
        }
    }

    public async Task SendHeadsUpNotification(string name, string title, string message)
    {
        try
        {
            if (UserConnections.TryGetValue(name, out var connectionId))
            {
                await Clients.Client(connectionId).HeadsUpNotification(title, message);
                Console.WriteLine($"SendHeadsUpNotification to {connectionId}");
            }
        }
        catch (Exception e)
        {
            Console.WriteLine($"SendHeadsUpNotification error: {e.Message}");
        }
    }

    public override Task OnConnectedAsync()
    {
        Console.WriteLine($"OnConnected: {Context.ConnectionId}");
        return base.OnConnectedAsync();
    }

    public override Task OnDisconnectedAsync(Exception? exception)
    {
        // Remove disconnected user from dictionary
        var entry = UserConnections.FirstOrDefault(x => x.Value == Context.ConnectionId);
        if (entry.Key != null)
        {
            UserConnections.TryRemove(entry.Key, out _);
        }

        Console.WriteLine($"OnDisconnected: {Context.ConnectionId}");
        return base.OnDisconnectedAsync(exception);
    }
}
