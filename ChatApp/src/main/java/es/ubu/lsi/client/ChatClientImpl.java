package es.ubu.lsi.client;

import es.ubu.lsi.common.ChatMessage;
import es.ubu.lsi.common.ChatMessage.MessageType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.io.ObjectInputStream;

/**
 * Implementación de la interfaz ChatClient que se conecta a un servidor de chat.
 * 
 * @author Pablo Echavarria
 */
public class ChatClientImpl implements ChatClient {
    private String server;
    private String username;
    private int port;
    private boolean carryOn = true;
    private Socket socket;

    /**
     * Constructor de la clase ChatClientImpl.
     * 
     * @param server   La dirección del servidor
     * @param port     El puerto del servidor
     * @param username El nombre de usuario del cliente
     */
    public ChatClientImpl(String server, int port, String username) {
        this.server = server;
        this.port = port;
        this.username = username;
    }

    @Override
    public boolean start() {
        try {
            // Conexión al servidor
            socket = new Socket(server, port);
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(new ChatMessage(0, MessageType.MESSAGE, username + " has joined the chat."));

            // Inicia un hilo separado para escuchar mensajes del servidor
            ChatClientListener listener = new ChatClientListener(socket);
            Thread listenerThread = new Thread(listener);
            listenerThread.start();

            System.out.println("Connected to server.");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void sendMessage(ChatMessage msg) {
        try {
            // Envía el mensaje al servidor
            ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.writeObject(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnect() {
        try {
            // Desconexión del servidor
            carryOn = false;
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Método main para iniciar el cliente desde la línea de comandos.
     * 
     * @param args Entrada de servidor y usuario
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java es.ubu.lsi.client.ChatClientImpl <server> <username>");
            return;
        }

        String server = args[0];
        String username = args[1];
        int port = 1500; // Puerto predeterminado

        ChatClientImpl client = new ChatClientImpl(server, port, username);
        if (!client.start()) {
            System.out.println("Failed to connect to server.");
            return;
        }

        // Bucle principal para manejar la entrada del usuario
        BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
        while (client.carryOn) {
            try {
                String messageText = consoleReader.readLine();
                if (messageText.equalsIgnoreCase("logout")) {
                    client.sendMessage(new ChatMessage(0, MessageType.LOGOUT, ""));
                    break;
                }
                client.sendMessage(new ChatMessage(0, MessageType.MESSAGE, messageText));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        client.disconnect();
    }

    /**
     * Clase interna que implementa un hilo para escuchar mensajes del servidor.
     */
    private class ChatClientListener implements Runnable {
        private Socket socket;

        /**
         * Constructor de la clase ChatClientListener.
         * 
         * @param socket El socket del cliente
         */
        public ChatClientListener(Socket socket) {
            this.socket = socket;
        }

        /**
         * Ejecuta el hilo para escuchar mensajes del servidor.
         */
        @Override
        public void run() {
            try {
                ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

                // Escucha continuamente mensajes del servidor
                while (carryOn) {
                    ChatMessage receivedMessage = (ChatMessage) inputStream.readObject();
                    MessageType type = receivedMessage.getType();

                    switch (type) {
                        case MESSAGE:
                            // Muestra los mensajes entrantes del servidor
                            System.out.println(receivedMessage.getId() + ": " + receivedMessage.getMessage());
                            break;
                        case LOGOUT:
                            // Informa al usuario y desconecta
                            System.out.println("You have been logged out.");
                            disconnect();
                            break;
                        case SHUTDOWN:
                            // Informa al usuario y desconecta
                            System.out.println("Server has been shut down.");
                            disconnect();
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}

