package es.ubu.lsi.server;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementación del servidor para el chat en modo distribuido.
 * 
 * @author Pablo Echavarria
 */
public class ChatServerImpl implements ChatServer {
    /** Puerto predeterminado para el servidor */
    private static final int DEFAULT_PORT = 1500;
    /** Puerto del servidor */
    private int port;
    /** Estado del servidor */
    private boolean alive;
    /** Lista de hilos de servidor para clientes */
    private List<ServerThreadForClient> clients;
    /** Lista de usuarios conectados */
    private List<String> users;

    /**
     * Constructor sin parámetros que establece el puerto predeterminado y crea la lista de clientes y usuarios.
     */
    public ChatServerImpl() {
        this.port = DEFAULT_PORT;
        this.clients = new ArrayList<>();
        this.users = new ArrayList<>();
    }

    /**
     * Constructor que permite especificar el puerto del servidor y crea la lista de clientes y usuarios.
     *
     * @param port Puerto del servidor
     */
    public ChatServerImpl(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
        this.users = new ArrayList<>();
    }

    /**
     * Inicia el servidor.
     * Espera y acepta peticiones de clientes, crea un hilo de servidor para cada cliente y lo inicia.
     */
    @Override
    public void startup() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            alive = true;
            System.out.println("Server started on port " + port);

            while (alive) {
                Socket socket = serverSocket.accept();
                System.out.print("Enter your username: ");
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());
                String username = (String) inputStream.readObject(); // Obtener el nombre de usuario del usuario
                ServerThreadForClient clientThread = new ServerThreadForClient(socket);
                clientThread.setUsername(username);
                clients.add(clientThread);
                clientThread.start();
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Detiene el servidor.
     */
    @Override
    public void shutdown() {
        try {
            alive = false;
            for (ServerThreadForClient clientThread : clients) {
                clientThread.shutdown();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Envía un mensaje a todos los clientes conectados.
     *
     * @param message Mensaje a enviar
     */
    @Override
    public void broadcast(ChatMessage message) {
        for (ServerThreadForClient clientThread : clients) {
            // Verificar si el remitente está en la lista negra del destinatario
                clientThread.sendMessage(message);           
        }
    }

    /**
     * Elimina un cliente de la lista de clientes.
     *
     * @param id ID del cliente a eliminar
     */
    @Override
    public void remove(int id) {
        for (ServerThreadForClient clientThread : clients) {
            if (clientThread.getID() == id) {
                clients.remove(clientThread);
                return;
            }
        }
    }

    /**
     * Método principal para iniciar el servidor.
     *
     * @param args Argumentos de la línea de comandos
     */
    public static void main(String[] args) {
        ChatServerImpl server = new ChatServerImpl();
        server.startup();
    }

    /**
     * Clase interna que representa un hilo de servidor para un cliente.
     */
    private class ServerThreadForClient extends Thread {
        private Socket socket;
        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;
        private int id;
        private String username;
        /** Conjunto para almacenar los usuarios bloqueados */
        private Set<String> blacklist = new HashSet<>();

        /**
         * Constructor que inicializa el hilo del servidor para un cliente.
         *
         * @param socket Socket del cliente
         */
        public ServerThreadForClient(Socket socket) {
            this.socket = socket;
            // Inicialización de streams de entrada y salida
            try {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                inputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Ejecuta el hilo de servidor para un cliente.
         * Escucha los mensajes del cliente y maneja las acciones correspondientes.
         */
        @Override
        public void run() {
            try {
                while (true) {
                    ChatMessage receivedMessage = (ChatMessage) inputStream.readObject();
                    handleMessage(receivedMessage);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Envía un mensaje al cliente.
         *
         * @param message Mensaje a enviar
         */
        public void sendMessage(ChatMessage message) {
            try {
                outputStream.writeObject(message);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Establece el ID del cliente.
         *
         * @param id ID del cliente
         */
        public void setID(int id) {
            this.id = id;
        }

        /**
         * Obtiene el username del cliente.
         *
         * @return Username del cliente
         */
        public String getUsername() {
            return username;
        }
        
        /**
         * Establece el username del cliente.
         *
         * @param username username del cliente
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * Obtiene el ID del cliente.
         *
         * @return ID del cliente
         */
        public int getID() {
            return id;
        }

        /**
         * Verifica si un usuario está bloqueado por este cliente.
         *
         * @param sender El nombre de usuario del remitente
         * @return true si el remitente está bloqueado, false de lo contrario
         */
        public boolean isBlocked(String sender) {
            return blacklist.contains(sender);
        }

        /**
         * Detiene el hilo del servidor para el cliente.
         *
         * @throws IOException Si ocurre un error de E/S
         */
        public void shutdown() throws IOException {
            socket.close();
        }

        /**
         * Maneja los mensajes recibidos del cliente y ejecuta las acciones correspondientes.
         *
         * @param message El mensaje recibido del cliente.
         */
        private void handleMessage(ChatMessage message) {
            switch (message.getType()) {
                // Caso: Mensaje de texto
                case MESSAGE:
                    // Reenvía el mensaje a todos los clientes conectados
                    broadcast(message);
                    break;
                // Caso: Cierre de sesión
                case LOGOUT:
                    try {
                        // Elimina al cliente de la lista de clientes
                        remove(getID());
                        // Notifica a los demás usuarios sobre el cierre de sesión
                        broadcast(new ChatMessage(getID(), MessageType.MESSAGE, "User " + username + " has logged out."));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                // Caso: Apagado del servidor
                case SHUTDOWN:
                    try {
                        // Apaga el servidor
                        shutdown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                // Caso: Bloqueo de usuario
                case BLOCK:
                    // Obtiene el nombre de usuario a bloquear del mensaje
                    String userToBlock = message.getMessage();
                    // Agrega el usuario a la lista negra (blacklist)
                    blacklist.add(userToBlock);
                    // Notifica a todos los clientes sobre el bloqueo del usuario
                    broadcast(new ChatMessage(getID(), MessageType.MESSAGE, "User " + getUsername() + " has blocked " + userToBlock));
                    break;
                // Caso: Desbloqueo de usuario
                case UNBLOCK:
                    // Obtiene el nombre de usuario a desbloquear del mensaje
                    String userToUnblock = message.getMessage();
                    // Elimina el usuario de la lista negra (blacklist)
                    blacklist.remove(userToUnblock);
                    // Notifica a todos los clientes sobre el desbloqueo del usuario
                    broadcast(new ChatMessage(getID(), MessageType.MESSAGE, "User " + getUsername() + " has unblocked " + userToUnblock));
                    break;
                // Otros tipos de mensajes (desconocidos)
                default:
                    System.out.println("Received unknown message type from " + getUsername());
                    break;
            }
        }

    }
}

