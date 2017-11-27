package distributed.systems.core;


/**
 * by
 *      e               d8   ,e,
 *     d8b     888¯~\ ¯d88¯¯  "   ¯¯¯d88P
 *    /Y88b    888     888   888    d88P
 *   /  Y88b   888     888   888   d88P
 *  /¯¯¯¯Y88b  888     888   888  d88P
 * /      Y88b 888     "88_/ 888 d88P____
 *
 * Created on 23-11-17.
 *
 * I wasn't fully sure how this class was expected to be implemented, so made
 * it a dummy class. I think the intend was to wait on sendMessage until an
 * ACK message was recieved or something, but it feels kind of weird communicating
 * over a socket like tcp or localsocket = memory and confirming.
 */
public class SynchronizedSocket extends Socket {
    private Socket realSocket;

    public SynchronizedSocket(Socket s) {
        realSocket = s;
    }

    @Override
    public void addMessageReceivedHandler(IMessageReceivedHandler handler) {
        realSocket.addMessageReceivedHandler(handler);
    }

    @Override
    public void register(String url) {
        realSocket.register(url);
    }

    @Override
    public void sendMessage(Message m, String url) {
        realSocket.sendMessage(m, url);
    }
}
