import java.io.*;
import java.net.*;
import java.util.*;

public class PweetterServer implements Runnable {
    private String userid;
    private final String ENCODING = "ISO-8859-1";
    private String latestStatus;
    private Boolean updated;
    private Random randgen;
    private Properties data;
    private final static String fn = "participants.properties";
    private HashMap<String, String> ip;
    private HashMap<String, String> pseudo;
    private HashMap<String, String> status;
    private HashMap<String, Integer> port;

    private HashMap<String, Integer> clocks;

    private DatagramSocket socket;

    private HashMap<String, Long> lastRes;
    private Set<String> userids;

    public Boolean canEnter;

    private Integer localClock;


    public PweetterServer(String userid) {
        this.userid = userid;
        this.updated = false;
        this.latestStatus = "";
        ip = new HashMap<String, String>();
        pseudo = new HashMap<String, String>();
        status = new HashMap<String, String>();
        port = new HashMap<String, Integer>();
        clocks = new HashMap<String, Integer>();
        userids = new HashSet<String>();
        status = new  HashMap<String, String>();
        lastRes = new HashMap<String, Long>();
        this.randgen = new Random();
        canEnter = true;
        localClock = 0;
        try {
            loadProperty();
            socket = new DatagramSocket(this.getPort(this.userid));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setUpdate(boolean val) {
        synchronized (updated) {
            this.updated = val;
        }
    }

    private boolean getUpdate() {
        boolean val;
        synchronized (updated) {
            val = this.updated;
        }
        return val;
    }

    private void setEnter(boolean val) {
        synchronized (canEnter) {
            this.canEnter = val;
        }
    }

    public boolean getEnter() {
        boolean val;
        synchronized (canEnter) {
            val = this.canEnter;
        }
        return val;
    }

    private void setClock(int newclck) {
        synchronized(this.localClock) {
            this.localClock = newclck;
        }
    }

    private int getClock() {
        int clock = 0;
        synchronized(this.localClock) {
            clock = this.localClock;
        }
        return clock;
    }

    @Override
    public void run() {
        for (;;) {
            if (getClock() != 0) {
                try {
                    Thread.sleep(1000 + randgen.nextInt(2000));
                    broadcast();
                } catch (IOException|InterruptedException e) {

                    try {
                        broadcast();
                    } catch (IOException e1) {}
                    print();
                    this.setEnter(true);
                }

            }
        }

    }

    private void print() {
        System.out.println("### P2P tweets ###");
        for (String keys: userids) {
            String line = printline(keys);
            if (line != null) {
                System.out.println("# " + line);
            }
        }
        System.out.println("### End tweets ###");
    }

    private String printline(String userid) {
        String line = "";
        if (isIdle(userid)) {
            line = "[" + getPseudo(userid) + " (" + userid + "): idle" + "]";
            setClocks(userid, 0);
            return line;
        } else if (hasLeft(userid)) {
            return null;
        } else if(!hasInit(userid)) {
            line = "[" + getPseudo(userid) + " (" + userid + "): not yet initialized" + "]";
        } else {
            String key = (this.userid.equals(userid))? "myself" : userid;
            line = getPseudo(userid) + " (" + key + "): " + getStatus(userid);
            return line;
        }
        return line;
    }

    private boolean isIdle(String userid) {
        return !this.userid.equals(userid) && System.currentTimeMillis() - this.getLastRes(userid) >= 10000 &&  System.currentTimeMillis() - this.getLastRes(userid) < 20000;
    }

    private boolean hasInit(String userid) {
        return !this.getStatus(userid).trim().equals("");
    }

    private boolean hasLeft(String userid) {
        return !this.userid.equals(userid) && System.currentTimeMillis() - this.getLastRes(userid) >= 20000;
    }

    public void update(String status) {

        setClock(getClock() + 1);
        this.latestStatus = status;
        this.setEnter(false);
        this.setStatus(this.userid, status);

        this.setUpdate(true);
    }

    private void broadcast() throws IOException {

        for (String key: userids) {
            byte[] bytebuffer = encode(this.userid + ":" + this.latestStatus.replaceAll(":", "\\\\:") + ":" + localClock);
            DatagramPacket packet = new DatagramPacket(bytebuffer,
                    bytebuffer.length, InetAddress.getByName(getIp(key)), this.getPort(key));
            socket.send(packet);
        }

    }

    public void listen() throws IOException {
        byte[] buf = new byte[1024];
        DatagramPacket received = new DatagramPacket(buf, 1024);
        socket.receive(received);
        unmarshall(received);
    }

    private void unmarshall(DatagramPacket received) {
        String raw = this.decode(received.getData());
        String sender = raw.substring(0, raw.indexOf(':'));

        String newStatus = raw.substring(raw.indexOf(':') + 1).split("(?<!\\\\):")[0];
        String seqnum = null;
        if (raw.substring(raw.indexOf(':') + 1).split("(?<!\\\\):").length > 1) {

            seqnum = raw.substring(raw.indexOf(':') + 1).split("(?<!\\\\):")[1];
        }
        newStatus = newStatus.replaceAll("\\\\:", ":").trim();
        if(seqnum == null || seqnum.trim().equals("")) {
            this.setStatus(sender, newStatus);
            this.setLastRes(sender, System.currentTimeMillis());
        } else if(isNumber(seqnum)) {
            int newclck = Integer.valueOf(seqnum.trim()).intValue();
            if(this.getClocks(sender) <= newclck) {
                this.setClocks(sender, newclck);
                this.setStatus(sender, newStatus);
                this.setLastRes(sender, System.currentTimeMillis());
            }
        } else {
            this.setStatus(sender, newStatus);
            this.setLastRes(sender, System.currentTimeMillis());

        }

    }

    private String decode(byte[] b) {
        try {
            return new String(b, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ENCODING;
    }

    private byte[] encode(String s) {
        try {
            return s.getBytes(ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadProperty() throws FileNotFoundException, IOException {
        data = new Properties();
        data.load(new FileInputStream(fn));
        String[] peerList = data.getProperty("participants").split(",");
        for (String peer: peerList) {
            String peerUnikey = data.getProperty(peer + ".userid");
            String peerPseudo = data.getProperty(peer + ".pseudo");
            Integer peerPort = Integer.parseInt(data.getProperty(peer + ".port"));
            String peerIp = data.getProperty(peer + ".ip");
            this.userids.add(peerUnikey);
            this.setIp(peerUnikey, peerIp);
            this.setPort(peerUnikey, peerPort.intValue());
            this.setPseudo(peerUnikey, peerPseudo);
            this.setStatus(peerUnikey, "");
            this.setLastRes(peerUnikey, System.currentTimeMillis());
            this.setClocks(peerUnikey, 0);
        }
    }

    public static boolean isNumber(String str) {
        if (str == null) {
            return false;
        }
        try {
            Double.parseDouble(str);
        }
        catch(NumberFormatException e) {
            return false;
        }
        return true;
    }

    public synchronized String getIp(String userid) {
        return this.ip.get(userid);
    }

    public synchronized void setIp(String userid, String ip) {
        this.ip.put(userid, ip);
    }

    public synchronized String getPseudo(String userid) {
        return pseudo.get(userid);
    }

    public synchronized void setPseudo(String userid, String pseudo) {
        this.pseudo.put(userid, pseudo);
    }

    public synchronized String getStatus(String userid) {
        return status.get(userid);
    }

    public synchronized void setStatus(String userid, String status) {
        this.status.put(userid, status);
    }

    public synchronized int getPort(String userid) {
        return port.get(userid);
    }

    public synchronized void setPort(String userid, int port) {
        this.port.put(userid, port);
    }

    public synchronized int getClocks(String userid) {
        return clocks.get(userid);
    }

    public synchronized void setClocks(String userid, int clock) {
        clocks.put(userid, Integer.valueOf(clock));
    }

    public synchronized long getLastRes(String userid) {
        return lastRes.get(userid);
    }

    public synchronized void setLastRes(String userid, long res) {
        this.lastRes.put(userid, Long.valueOf(res));
    }
}
