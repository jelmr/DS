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
 * The api is relatively easy:
 * - register(url) you register that this socket is bound to a certain URL
 * - addMessageReceivedHandler(handler), if a packet arrives at this socket, this handler should fire
 * - sendMessage(msg, url) send a Message object to another socket bound to some url.
 *
 * Ambiguities:
 * - number of handlers attached, in LocalSocket, infinite, I think the idea may have been 1
 * - number of urls attached to the socket, in LocalSocket, infinite, I also think the idea here
 *    was one.
 *
 * However the order in which these are called *can* differ, so fun timez.
 */
public abstract class Socket {
    public abstract void addMessageReceivedHandler(IMessageReceivedHandler i);

    public abstract void register(String url);

    public abstract void sendMessage(Message m, String url);
}
