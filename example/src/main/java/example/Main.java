package example;

import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;

import java.time.Instant;

public class Main {
    public static void main(String[] args) {
        System.out.println("Starting Discord IPC");

        if (!DiscordIPC.start(932987954815696957L, () -> System.out.println("Logged in account: " + DiscordIPC.getUser().username))) {
            System.out.println("Failed to start Discord IPC");
            return;
        }

        RichPresence presence = new RichPresence();
        presence.setDetails("Monkey!!!");
        presence.setState("yes");
        presence.setLargeImage("a", "Large image");
        presence.setSmallImage("b", "Small image");
        presence.setStart(Instant.now().getEpochSecond());
        DiscordIPC.setActivity(presence);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Stopping Discord IPC");
        DiscordIPC.stop();
    }
}
