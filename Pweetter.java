import java.util.Scanner;

public class Pweetter implements Runnable{

    PweetterServer server;
    PweetterListener listener;
    String userid;
    Thread mainThread;

    public Pweetter(String userid, PweetterServer server, PweetterListener listener) {
        this.server = server;
        this.listener = listener;
        this.userid = userid;

        this.mainThread = new Thread(server);
        mainThread.start();
        new Thread(listener).start();
    }

    @Override
    public void run() {
        Scanner in = new Scanner(System.in);
        
        String status;

        for(;;) {
            if (server.getEnter()) {
                System.out.print("Status: ");
                if (!in.hasNextLine()) {
                    break;
                }
                status = in.nextLine();
                if (status.trim().length() <= 0) {
                    System.out.println("Status is empty. Retry.");
                } else if (status.trim().length() > 140) {
                    System.out.println("Status is too long, 140 characters max. Retry.");
                } else {
                    this.server.update(status);
                    this.mainThread.interrupt();
                }
            }
        }
    }


    @SuppressWarnings("resource")
    public static void main(String[] args) {
        String key = args[0];
        PweetterServer server = new PweetterServer(key);

        PweetterListener listener  = new PweetterListener(server);
        new Thread(new Pweetter(key, server, listener)).start();
    }
}
