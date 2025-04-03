package clientPackage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * Classe principale du client de chat.
 * Cette classe permet à un utilisateur de se connecter à un serveur de chat,
 * choisir un pseudo, envoyer et recevoir des messages.
 */
public class ChatClient {

    /**
     * Méthode principale du client de chat.
     *
     * @param args Arguments de la ligne de commande (non utilisés).
     */
    public static void main(String[] args) {

        try {

            //Ouverture du socket sur le port 10080
            Socket client = new Socket("localhost", 10080);

            //Définition des flux de sortie et d'entrée
            OutputStream out = client.getOutputStream();
            InputStream in = client.getInputStream();

            //Création des threads pour l'envoi et la réception de messages
            MessageReceptor msgreceptor = new MessageReceptor(client);
            MessageSender msgsender = new MessageSender(client, msgreceptor);

            //Création du scanner pour la lecture de l'entrée utilisateur
            Scanner sc = new Scanner(System.in);

            //Variable pour le pseudo
            String pseudo;

            //Tableau de bytes pour la lecture des messages
            byte[] b = new byte[1024];

            //Variable pour le message de confirmation d'inscription
            String joined;

            //On demande à l'utilisateur de choisir un pseudo tant qu'il n'est pas valide (c'est à dire que ce n'est pas un pseudo déjà utilisé)
            do {
                System.out.println("Entrez votre pseudo :");
                pseudo = sc.nextLine();
                out.write(pseudo.getBytes());
                int bytesRead = in.read(b);
                joined = new String(b, 0, bytesRead).trim();
                System.out.println(joined);
            } while (joined.contains("Pseudo déjà utilisé, veuillez en choisir un autre"));

            //Délimitation de la zone de chat une fois cette dernière rejointe
            System.out.println("---------------------------------");

            //On lance le thread de réception de messages et le thread d'envoi de messages
            msgreceptor.start();
            msgsender.start();
        } catch (IOException ex) {  //Si jamais le serveur n'est pas lancé ou coupe sa connexion de manière inattendue, on informe l'utilisateur et on ferme proprement le client
            System.out.println("Connexion perdue avec le serveur !");
            System.exit(0);
        }
    }

    /**
     * Classe interne représentant un thread de réception des messages.
     */
    public static class MessageReceptor extends Thread {

        //Socket du client utilisé pour la connexion au serveur
        private Socket client;

        //Variable pour savoir si le thread doit continuer à tourner, nous sert pour arréter le thread proprement
        boolean running = true;

        /**
         * Constructeur de MessageReceptor.
         *
         * @param client Socket du client.
         */
        public MessageReceptor(Socket client) {
            this.client = client;
        }

        //Méthode pour arrêter le thread proprement
        public void stopRunning() {
            running = false;
        }

        @Override
        public void run() {
            try {

                //On récupère le flux d'entrée du client
                InputStream input = client.getInputStream();

                //Tableau de bytes pour la lecture des messages
                byte[] b = new byte[1024];

                //On lit les messages et on les affiche tant que le thread doit tourner
                while (running) {
                    int bytesRead = input.read(b);

                    //Si le client se déconnecte (i.e, on lit la fin du flux, ce qui signifie qu'il a été fermé), on sort de la boucle
                    if (bytesRead == -1) break;

                    //On récupère le message envoyé par le client
                    String response = new String(b, 0, bytesRead).trim();
                    System.out.println(response);
                }
            } catch (IOException ex) {  //Si jamais le serveur coupe sa connexion de manière inattendue, on informe l'utilisateur et on ferme proprement le client
                System.out.println("Connexion perdue avec le serveur !");
                System.exit(0);
            } finally {
                try {
                    client.close();
                } catch (IOException ex) {  //Si jamais le serveur coupe sa connexion de manière inattendue, on informe l'utilisateur et on ferme proprement le client
                    System.out.println("Connexion perdue avec le serveur !");
                    System.exit(0);
                }
            }
        }
    }

    /**
     * Classe interne représentant un thread d'envoi des messages.
     */
    public static class MessageSender extends Thread {

        //Socket du client utilisé pour la connexion au serveur
        private Socket client;

        //Thread de réception de messages, on en a besoin pour pouvoir l'arrêter proprement
        private MessageReceptor receptor;

        /**
         * Constructeur de MessageSender.
         *
         * @param client   Socket du client.
         * @param receptor Thread de réception des messages.
         */
        public MessageSender(Socket client, MessageReceptor receptor) {
            this.client = client;
            this.receptor = receptor;
        }

        @Override
        public void run() {
            try {

                //On récupère le flux de sortie et le flux d'entrée du client
                OutputStream output = client.getOutputStream();
                InputStream input = client.getInputStream();

                //Création du scanner pour la lecture de l'entrée utilisateur
                Scanner sc = new Scanner(System.in);

                //On lit les messages et on les envoie tant que le thread doit tourner
                while (true) {
                    String message = sc.nextLine();
                    output.write(message.getBytes());

                    //Si l'utilisateur souhaite sortir en écrivant "exit" on arrête le thread de réception de messages et on ferme le client
                    if (message.equals("exit")) {
                        receptor.stopRunning();

                        //On attend que le thread de réception de messages se termine avant de fermer le client
                        receptor.join();

                        //On ferme le flux d'entrée, le flux de sortie et le socket
                        input.close();
                        output.close();
                        client.close();
                        break;
                    }
                }
            } catch (IOException | InterruptedException ex) {   //Si jamais le serveur coupe sa connexion de manière inattendue, on informe l'utilisateur et on ferme proprement le client
                System.out.println("Connexion perdue avec le serveur !");
                System.exit(0);
            }
        }
    }

}
