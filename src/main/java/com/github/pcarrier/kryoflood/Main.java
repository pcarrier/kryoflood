package com.github.pcarrier.kryoflood;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.github.pcarrier.kryoflood.protocol.SimpleTest;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    static final int udpPort = 40999;
    static final int tcpPort = 40998;

    private static void setupKryo(Kryo k) {
        k.register(SimpleTest.class);
    }

    private static void runServer(final int tcpPort, final int udpPort, final boolean isUdp) throws IOException {
        final AtomicInteger nextClientId = new AtomicInteger(0);
        final Server server = new Server();
        setupKryo(server.getKryo());

        server.addListener(new Listener() {
            @Override
            public void connected(final Connection connection) {
                new Thread() {
                    @Override
                    public void run() {
                        this.setName("Server@" + nextClientId.getAndIncrement());
                        final Random rand = new Random();
                        if (isUdp)
                            //noinspection InfiniteLoopStatement
                            while (true)
                                connection.sendUDP(new SimpleTest(rand.nextInt(), "Hello!"));
                        else
                            //noinspection InfiniteLoopStatement
                            while (true)
                                connection.sendTCP(new SimpleTest(rand.nextInt(), "Hello!"));
                    }
                }.start();
            }
        });

        server.start();
        server.bind(tcpPort, udpPort);
    }

    private static long updateTimestamp(int id, long oldTS, long increments) {
        final long newTS = System.currentTimeMillis();
        final long diff = newTS - oldTS;
        final float rate = (float) (1000 * increments) / (float) diff;
        System.err.println(id + ": " + rate + " msg/s");
        return newTS;
    }

    private static void runClient(final int id, final String host, final int tcpPort, final int udpPort) throws IOException {
        final long increments = 100000;
        final AtomicInteger msgReceived = new AtomicInteger(0);
        final long[] ts = {updateTimestamp(id, System.currentTimeMillis(), increments)};

        final Client client = new Client();
        setupKryo(client.getKryo());

        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.err.println(id + " connected!");
            }

            @Override
            public void received(Connection connection, Object o) {
                if (o instanceof SimpleTest) {
                    final int received = msgReceived.incrementAndGet();
                    if (received % increments == 0) ts[0] = updateTimestamp(id, ts[0], increments);
                } else {
                    System.err.println(id + ": Unknown message received!");
                }
            }
        });

        client.start();
        client.connect(5000, host, tcpPort, udpPort);
    }

    public static void main(String[] args) throws IOException {
        Log.DEBUG();
        if (args.length == 0 || args[0].contains("s")) {
            if (args.length == 1 && args[0].contains("u"))
                runServer(tcpPort, udpPort, true);
            else
                runServer(tcpPort, udpPort, false);
        }
        if (args.length == 0 || args[0].contains("c")) {
            final int clients = (args.length > 1) ? Integer.parseInt(args[1]) : 1;
            for (int i = 0; i < clients; i++)
                runClient(i, "127.0.0.1", tcpPort, udpPort);
        }
    }
}
