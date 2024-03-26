package es.ubu.lsi.common;

/**
 * Enumeración MessageType que define los tipos de mensajes admitidos en el sistema de chat.
 * Los tipos de mensajes incluyen texto, cierre de sesión, apagado del servidor, bloqueo y desbloqueo de usuarios.
 */
public enum MessageType {
    
    /** Mensaje de texto enviado entre usuarios. */
    TEXT,
    
    /** Mensaje de cierre de sesión de un usuario. */
    LOGOUT,
    
    /** Mensaje de apagado del servidor. */
    SHUTDOWN,
    
    /** Mensaje de bloqueo de un usuario. */
    BLOCK,
    
    /** Mensaje de desbloqueo de un usuario. */
    UNBLOCK
}