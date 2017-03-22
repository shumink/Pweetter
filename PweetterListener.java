
import java.io.IOException;

public class PweetterListener implements Runnable {
    PweetterServer server;
    int i;

    public PweetterListener(PweetterServer server) {
        this.server = server;
        this.i = 0;
    }

    public void run() {
        while(true) {
            try {
                this.server.listen();
            } catch (IOException var2 ) {
                break;
            }
        }
    }
}
