package meteordevelopment.discordipc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import meteordevelopment.discordipc.connection.Connection;

import java.lang.management.ManagementFactory;
import java.util.function.BiConsumer;

public class DiscordIPC {
    private static final Gson GSON = new Gson();

    private static BiConsumer<Integer, String> onError = DiscordIPC::defaultErrorCallback;

    private static Connection c;
    private static Runnable onReady;

    private static boolean receivedDispatch;
    private static JsonObject queuedActivity;

    private static IPCUser user;

    /** Sets the error callback */
    public static void setOnError(BiConsumer<Integer, String> onError) {
        DiscordIPC.onError = onError;
    }

    /**
     * Tries to open a connection to a locally running Discord instance
     * @param appId the application id to use
     * @param onReady callback called when a successful connection happens, from that point {@link #getUser()} will return non-null object up until {@link #stop()} is called or an error happens
     * @return true if a connection was opened successfully
     */
    public static boolean start(long appId, Runnable onReady) {
        // Open connection
        c = Connection.open(DiscordIPC::onPacket);
        if (c == null) return false;

        DiscordIPC.onReady = onReady;

        // Handshake
        JsonObject o = new JsonObject();
        o.addProperty("v", 1);
        o.addProperty("client_id", Long.toString(appId));
        c.write(Opcode.Handshake, o);

        return true;
    }

    /**
     * @return true if it is currently connected to a local Discord instance
     */
    public static boolean isConnected() {
        return c != null;
    }

    /**
     * @return the user that is logged in in the connected Discord instance
     */
    public static IPCUser getUser() {
        return user;
    }

    /**
     * Sets account's activity
     * @param presence the rich presence to set the activity to
     */
    public static void setActivity(RichPresence presence) {
        if (c == null) return;

        queuedActivity = presence.toJson();
        if (receivedDispatch) sendActivity();
    }

    /** Closes the connection to the locally running Discord instance if it is open */
    public static void stop() {
        if (c != null) {
            c.close();

            c = null;
            onReady = null;
            receivedDispatch = false;
            queuedActivity = null;
            user = null;
        }
    }

    private static void sendActivity() {
        JsonObject args = new JsonObject();
        args.addProperty("pid", getPID());
        args.add("activity", queuedActivity);

        JsonObject o = new JsonObject();
        o.addProperty("cmd", "SET_ACTIVITY");
        o.add("args", args);

        c.write(Opcode.Frame, o);
        queuedActivity = null;
    }

    private static void onPacket(Packet packet) {
        // Close
        if (packet.opcode() == Opcode.Close) {
            if (onError != null) onError.accept(packet.data().get("code").getAsInt(), packet.data().get("message").getAsString());
            stop();
        }
        // Frame
        else if (packet.opcode() == Opcode.Frame) {
            // Error
            if (packet.data().has("evt") && packet.data().get("evt").getAsString().equals("ERROR")) {
                JsonObject d = packet.data().getAsJsonObject("data");
                if (onError != null) onError.accept(d.get("code").getAsInt(), d.get("message").getAsString());
            }
            // Dispatch
            else if (packet.data().has("cmd") && packet.data().get("cmd").getAsString().equals("DISPATCH")) {
                receivedDispatch = true;
                user = GSON.fromJson(packet.data().getAsJsonObject("data").getAsJsonObject("user"), IPCUser.class);

                if (onReady != null) onReady.run();
                if (queuedActivity != null) sendActivity();
            }
        }
    }

    private static int getPID() {
        String pr = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(pr.substring(0, pr.indexOf('@')));
    }

    private static void defaultErrorCallback(int code, String message) {
        System.err.println("Discord IPC error " + code + " with message: " + message);
    }
}
