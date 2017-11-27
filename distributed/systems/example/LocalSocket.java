package distributed.systems.example;
import distributed.systems.core.IMessageReceivedHandler;
import distributed.systems.core.Message;
import distributed.systems.core.Socket;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;


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
 * Reconstructed from vague guidelines about what it should have been doing.
 *
 * Essentially it allows for a sort of in-memory dns sending of messages.
 *
 * register(url) allows for an arbitrary number of urls, although it's not optimized for this purpose
 *      (calling the register function multiple times may become expensive due to choice for ArrayList rather than Set)
 *
 * addMessageReceivedHandler(handler) also allows for an arbitrary number of handlers.
 *
 * ALSO order does not matter due to rest of program apparently not caring about order either.
 *  URLs have to be unique though!
 */
public class LocalSocket extends Socket {
    private static HashMap<String, List<IMessageReceivedHandler>> URL2Handler = new HashMap<>();

    private ArrayList<String>                  boundUrls;
    private ArrayList<IMessageReceivedHandler> boundHandlers;

    public LocalSocket() {
        boundUrls     = new ArrayList<>();
        boundHandlers = new ArrayList<>();
    }

    @Override
    public void addMessageReceivedHandler(IMessageReceivedHandler handler) {
        if (boundHandlers.contains(handler))
            return;

        System.out.println("LocalSocket registering handler");
        boundHandlers.add(handler);

        for (String url : boundUrls) {
            register(handler, url);
        }
    }

    private void register(IMessageReceivedHandler handler, String url) {
        List<IMessageReceivedHandler> urlHandlerList = URL2Handler.get(url);

        if (urlHandlerList != null)
            urlHandlerList.add(handler);
        else {
            urlHandlerList = new ArrayList<>();
            urlHandlerList.add(handler);
            URL2Handler.put(url, urlHandlerList);
        }
    }

    @Override
    public void register(String url) {
        if (boundUrls.contains(url))
            return;

        System.out.printf("LocalSocket registering url: %s\n", url);
        boundUrls.add(url);

        for (IMessageReceivedHandler handler : boundHandlers)
            register(handler, url);
    }

    @Override
    public void sendMessage(Message m, String url) {
        if (!url.startsWith("localsocket://")) {
            RuntimeException up = new RuntimeException(String.format("Attempt to send to non local url: %s\n", url));
            throw up; // hahaha;
        }
        url = url.substring("localsocket://".length());

        List<IMessageReceivedHandler> handlers = URL2Handler.get(url);

        if (handlers == null) {
            RuntimeException up = new RuntimeException(String.format("Couldn't send message, no handlers attached to url %s\n", url));
            throw up; // hahaha;
        }

        for (IMessageReceivedHandler handler : handlers)
            handler.onMessageReceived(m);
    }
}
