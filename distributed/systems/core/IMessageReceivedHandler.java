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
 * Reconstructed from vague guidelines about what it should have been doing
 */
public interface IMessageReceivedHandler {
    public void onMessageReceived(Message message);
}
