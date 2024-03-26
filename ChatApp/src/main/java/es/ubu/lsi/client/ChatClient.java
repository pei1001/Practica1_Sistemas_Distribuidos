package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;

/**
 * La interfaz ChatClient define el contrato que deben seguir las clases que actúan como clientes en el sistema de chat.
 * Permite iniciar una conexión con el servidor, enviar mensajes y desconectarse.
 */
public interface ChatClient {
    
    /**
     * Inicia la conexión del cliente con el servidor de chat.
     * 
     * @return true si la conexión se establece correctamente, false en caso contrario.
     */
    boolean start();
    
    /**
     * Envía un mensaje al servidor de chat.
     * 
     * @param msg El mensaje que se desea enviar.
     */
    void sendMessage(ChatMessage msg);
    
    /**
     * Desconecta al cliente del servidor de chat.
     */
    void disconnect();
}