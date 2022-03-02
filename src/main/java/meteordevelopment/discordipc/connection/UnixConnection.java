package meteordevelopment.discordipc.connection;

import com.google.gson.JsonParser;
import meteordevelopment.discordipc.Opcode;
import meteordevelopment.discordipc.Packet;

import java.io.IOException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.function.Consumer;

public class UnixConnection extends Connection {
    private final Selector s;
    private final SocketChannel sc;
    private final Consumer<Packet> callback;

    public UnixConnection(String name, Consumer<Packet> callback) throws IOException {
        this.s = Selector.open();
        this.sc = SocketChannel.open(UnixDomainSocketAddress.of(name));
        this.callback = callback;

        sc.configureBlocking(false);
        sc.register(s, SelectionKey.OP_READ);

        Thread thread = new Thread(this::run);
        thread.setName("Discord IPC - Read thread");
        thread.start();
    }

    private void run() {
        State state = State.Opcode;

        ByteBuffer intB = ByteBuffer.allocate(4);
        ByteBuffer dataB = null;

        Opcode opcode = null;

        try {
            while (true) {
                s.select();

                switch (state) {
                    case Opcode -> {
                        sc.read(intB);
                        if (intB.hasRemaining()) break;

                        opcode = Opcode.valueOf(Integer.reverseBytes(intB.getInt(0)));
                        state = State.Length;

                        intB.rewind();
                    }
                    case Length -> {
                        sc.read(intB);
                        if (intB.hasRemaining()) break;

                        dataB = ByteBuffer.allocate(Integer.reverseBytes(intB.getInt(0)));
                        state = State.Data;

                        intB.rewind();
                    }
                    case Data -> {
                        sc.read(dataB);
                        if (dataB.hasRemaining()) break;

                        String data = Charset.defaultCharset().decode(dataB.rewind()).toString();
                        callback.accept(new Packet(opcode, JsonParser.parseString(data).getAsJsonObject()));

                        dataB = null;
                        state = State.Opcode;
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void write(ByteBuffer buffer) {
        try {
            sc.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            s.close();
            sc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private enum State {
        Opcode,
        Length,
        Data
    }
}
