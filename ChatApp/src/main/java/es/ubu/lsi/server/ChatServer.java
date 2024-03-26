package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;

/**
 * Interfaz que define las operaciones disponibles en un servidor de chat.
 */
public interface ChatServer {
    
    /**
     * Inicia el servidor de chat.
     * Este método se encarga de iniciar el servidor y prepararlo para aceptar conexiones de clientes.
     */
    void startup();
    
    /**
     * Detiene el servidor de chat.
     * Este método se encarga de detener el servidor de chat y liberar los recursos asociados.
     */
    void shutdown();
    
    /**
     * Envía un mensaje a todos los clientes conectados.
     * 
     * @param message El mensaje que se desea enviar a todos los clientes.
     */
    void broadcast(ChatMessage message);
    
    /**
     * Elimina un cliente de la lista de clientes.
     * 
     * @param id El ID del cliente que se desea eliminar.
     */
    void remove(int id);
}
