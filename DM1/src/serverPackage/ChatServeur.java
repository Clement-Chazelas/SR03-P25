package serverPackage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Map;


/**
 * Classe representant un serveur de chat permettant la connexion de plusieurs clients.
 */
public class ChatServeur {

    //Tableau de clients connectés au serveur, la clé de ce tableau est le pseudo du client pour garantir son l'unicité
    public static Hashtable<String, Socket> clients = new Hashtable<>();


    /**
     * Méthode principale qui lance le serveur et gère les connexions clients.
     * @param args Arguments de la ligne de commande (non utilisés).
     */
    public static void main(String[] args) {

        //Variable pour le pseudo
        String pseudo = "";
        try {
            //Ouverture du socket sur le port 10080
            ServerSocket conn = new ServerSocket(10080);

            //On attend en permanence une connexion d'un client
            while (true) {
                try {
                    //Dès qu'on a une connexion on l'accepte
                    Socket comm = conn.accept();

                    //On récupère les flux de sortie et d'entrée
                    OutputStream out = comm.getOutputStream();
                    InputStream in = comm.getInputStream();

                    //Tableau de bytes pour la lecture des messages
                    byte[] b = new byte[1024];

                    //On demande à l'utilisateur de choisir un pseudo tant qu'il n'est pas valide (c'est à dire que ce n'est pas un pseudo déjà utilisé)
                    while (true) {
                        int bytesRead = in.read(b);
                        pseudo = new String(b, 0, bytesRead).trim();
                        if (clients.containsKey(pseudo)) {
                            out.write("Pseudo déjà utilisé, veuillez en choisir un autre\n".getBytes());
                            out.flush();
                        } else {    //Si le pseudo est bien unique, on sort de la boucle
                            break;
                        }
                    }

                    //On ajoute le client à la liste des clients connectés
                    clients.put(pseudo, comm);

                    //On envoie un message à tous les clients pour les prévenir de l'arrivée d'un nouveau client
                    for (Socket socket : clients.values()) {
                        OutputStream output = socket.getOutputStream();
                        output.write((pseudo + " a rejoint la conversation").getBytes());
                    }

                    //Création et lancement du thread qui gère la réception des messages pour ce client
                    MessageInterceptor msginterceptor = new MessageInterceptor(comm);
                    msginterceptor.start();
                } catch (IOException ex) {  //Si jamais le client se déconnecte de manière inattendue, on le marque dans la console du serveur, on informe le reste des clients et on ferme proprement le socket
                    System.out.println("Déconnexion inopinée de " + pseudo);
                    clients.remove(pseudo);
                    try {
                        for (Socket socket : clients.values()) {
                            OutputStream out = socket.getOutputStream();
                            out.write((pseudo + " a quitté la conversation de manière inopinée").getBytes());
                        }
                    } catch (IOException e) {
                        System.out.println("Erreur lors de l'envoi du message de déconnexion");
                    }
                }
            }
        } catch (IOException ex) {      //Si jamais le client se déconnecte de manière inattendue, on le marque dans la console du serveur, on informe le reste des clients et on ferme proprement le socket
            System.out.println("Déconnexion inopinée de " + pseudo);
            clients.remove(pseudo);
            try {
                for (Socket socket : clients.values()) {
                    OutputStream out = socket.getOutputStream();
                    out.write((pseudo + " a quitté la conversation de manière inopinée").getBytes());
                }
            } catch (IOException e) {
                System.out.println("Erreur lors de l'envoi du message de déconnexion");
            }
        }
    }

    /**
     * Classe interne gérant la réception des messages envoyés par un client.
     */
    public static class MessageInterceptor extends Thread {

        //Socket du client utilisé pour la connexion au serveur
        private Socket client;

        /**
         * Constructeur de la classe MessageInterceptor.
         * @param client La socket du client associé.
         */
        public MessageInterceptor(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {

            //On récupère le pseudo du client
            String pseudo = getKeyByValue(clients, client);
            try {

                //On récupère le flux d'entrée et le flux de sortie du client
                InputStream input = client.getInputStream();
                OutputStream output = client.getOutputStream();

                //Tableau de bytes pour la lecture des messages
                byte[] b = new byte[1024];

                //On lit les messages et on les affiche tant que le client ne s'est pas déconnecté
                while (true) {
                    int bytesRead = input.read(b);

                    //Si le client se déconnecte (i.e, on lit la fin du flux, ce qui signifie qu'il a été fermé), on sort de la boucle
                    if (bytesRead == -1) {
                        break;
                    }
                    //On récupère le message envoyé par le client
                    String response = new String(b, 0, bytesRead).trim();

                    //Si le client envoie "exit", on le déconnecte, on l'enlève de la liste des clients connectés et on ferme ses flux d'entrée, de sortie et le socket
                    if (response.contains("exit")) {
                        clients.remove(pseudo);
                        input.close();
                        output.close();
                        client.close();

                        //On envoie un message à tous les clients pour les prévenir de la déconnexion du client
                        for (Socket socket : clients.values()) {
                            OutputStream out = socket.getOutputStream();
                            out.write((pseudo + " a quitté la conversation").getBytes());
                        }
                        break;      //On sort de la boucle pour arréter le thread
                    } else {        //Si le client n'a pas envoyé "exit", on envoie le message à tous les clients connectés
                        String sender = getKeyByValue(clients, client);
                        for (Socket socket : clients.values()) {
                            OutputStream out = socket.getOutputStream();
                            out.write((sender + " a dit : " + response).getBytes());
                        }
                    }
                }
            } catch (IOException ex) {      //Si jamais le client se déconnecte de manière inattendue, on le marque dans la console du serveur, on informe le reste des clients et on ferme proprement le socket
                System.out.println("Déconnexion inopinée de " + pseudo);
                clients.remove(pseudo);
                try {
                    for (Socket socket : clients.values()) {
                        OutputStream out = socket.getOutputStream();
                        out.write((pseudo + " a quitté la conversation de manière inopinée").getBytes());
                    }
                } catch (IOException e) {
                    System.out.println("Erreur lors de l'envoi du message de déconnexion");
                }
            }
        }
    }

    /**
     * Récupère le pseudo du client associé à une socket.
     * @param allclients La liste des clients connectés.
     * @param socket La socket du client.
     * @return Le pseudo du client, ou null si non trouvé.
     */
    public static String getKeyByValue(Hashtable<String, Socket> allclients, Socket socket) {

        //On parcourt le tableau de clients et si la valeur (socket) est égale à la socket du client, on retourne la clé (pseudo)
        for (Map.Entry<String, Socket> element : allclients.entrySet()) {
            if (element.getValue().equals(socket)) {
                return element.getKey();
            }
        }
        return null;        //Si jamais on ne trouve pas la socket dans le tableau de clients, on retourne null
    }

}
