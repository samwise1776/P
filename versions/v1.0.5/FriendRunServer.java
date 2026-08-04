import java.io.*;
import java.net.*;
import java.util.*;

public class FriendRunServer {
    static final int PORT = 4444;
    static final int TIME_LIMIT = 60;
    static final List<Client> waiting = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        int port = PORT;
        if (args.length > 0) port = Integer.parseInt(args[0]);
        ServerSocket ss = new ServerSocket(port);
        System.out.println("FriendRun server listening on port " + port);
        System.out.println("Match time limit: " + TIME_LIMIT + " seconds");
        while (true) {
            Socket s = ss.accept();
            new Thread(new Client(s)).start();
        }
    }

    static class Client implements Runnable {
        Socket socket;
        BufferedReader in;
        PrintWriter out;
        String name = "?";
        int points = 0;
        Match match;
        volatile boolean closed;

        Client(Socket s) {
            socket = s;
        }

        void send(String msg) {
            if (out != null) {
                synchronized (out) {
                    out.println(msg);
                    out.flush();
                }
            }
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String line = in.readLine();
                if (line != null && line.startsWith("JOIN ")) {
                    String[] parts = line.substring(5).split("\\s+");
                    if (parts.length > 0) name = parts[0];
                    if (parts.length > 1) {
                        try { points = Integer.parseInt(parts[1]); } catch (Exception ignored) {}
                    }
                    System.out.println("JOIN " + name + " points=" + points);

                    Client other = null;
                    synchronized (waiting) {
                        if (!waiting.isEmpty()) {
                            other = waiting.remove(0);
                        } else {
                            waiting.add(this);
                        }
                    }
                    if (other == null) {
                        send("WAIT");
                    } else {
                        new Match(this, other).start();
                    }

                    while (!closed) {
                        line = in.readLine();
                        if (line == null) break;
                        handle(line);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                leave();
            }
        }

        void handle(String line) {
            if (match != null) {
                if (line.equals("LEAVE")) {
                    match.end(true);
                    return;
                }
                if (line.startsWith("S ")) {
                    match.forward(this, line);
                }
            }
        }

        void leave() {
            if (closed) return;
            closed = true;
            if (match != null) {
                match.end(true);
            } else {
                synchronized (waiting) {
                    waiting.remove(this);
                }
            }
            try { socket.close(); } catch (Exception ignored) {}
        }
    }

    static class Match {
        Client a, b;
        Timer timer;
        int secondsLeft = TIME_LIMIT;
        int aMoney = 0, bMoney = 0;
        boolean ended;

        Match(Client x, Client y) {
            a = x;
            b = y;
        }

        void start() {
            a.match = this;
            b.match = this;
            a.send("MATCH " + b.name + " " + TIME_LIMIT + " " + b.points);
            b.send("MATCH " + a.name + " " + TIME_LIMIT + " " + a.points);
            System.out.println("Match started: " + a.name + " vs " + b.name);

            timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                public void run() {
                    secondsLeft--;
                    if (secondsLeft <= 0) {
                        timer.cancel();
                        end(false);
                        return;
                    }
                    a.send("TIME " + secondsLeft);
                    b.send("TIME " + secondsLeft);
                }
            }, 1000, 1000);
        }

        void forward(Client from, String msg) {
            String[] p = msg.split("\\s+");
            if (p.length >= 6) {
                try {
                    int m = Integer.parseInt(p[4]);
                    if (from == a) aMoney = m;
                    else bMoney = m;
                } catch (Exception ignored) {}
                Client to = (from == a) ? b : a;
                to.send("O " + p[1] + " " + p[2] + " " + p[3] + " " + p[4] + " " + p[5]);
            }
        }

        void end(boolean forfeit) {
            if (ended) return;
            ended = true;
            if (timer != null) timer.cancel();

            boolean aWins = aMoney > bMoney;
            String resA = aWins ? "WIN" : "LOSE";
            String resB = aWins ? "LOSE" : "WIN";
            if (forfeit) {
                resA = "WIN";
                resB = "LOSE";
            }
            a.send("END " + resA + " " + aMoney + " " + bMoney + " " + b.name);
            b.send("END " + resB + " " + bMoney + " " + aMoney + " " + a.name);
            System.out.println("Match ended: " + a.name + " " + resA + " (" + aMoney + ") vs " + b.name + " " + resB + " (" + bMoney + ")");
            a.closed = true;
            b.closed = true;
            try { a.socket.close(); } catch (Exception ignored) {}
            try { b.socket.close(); } catch (Exception ignored) {}
        }
    }
}
