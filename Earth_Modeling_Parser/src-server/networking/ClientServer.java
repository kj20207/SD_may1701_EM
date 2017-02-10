/**
 * @author Anish Kunduru
 * 
 *         Server-side class that handles client connections and interactions between a ClientThread and the main daemon.
 */

package networking;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

import javax.net.ssl.SSLServerSocketFactory;

import org.pmw.tinylog.Logger;

import main.EarthModellingDaemon;
import utils.FileLocations;

public class ClientServer implements Runnable {

	private Set<ClientThread> clients;
	private Map<String, String> approvedClients;
	private int serverPort;
	private boolean run;

	/**
	 * Creates a new Client server and starts its operation
	 * 
	 * @param portNumber
	 *           The port that the server should listen for connections on.
	 * @throws IOException
	 *            If there is an error reading from the approvedClients.txt file.
	 */
	public ClientServer(int portNumber) throws IOException {
		serverPort = portNumber;
		clients = new HashSet<ClientThread>();
		run = true;

		approvedClients = new HashMap<String, String>();
		addFromApprovedList();
	}

	/**
	 * Scans the approvedClients.txt file and adds all values to the HashMap.
	 * 
	 * @throws IOException
	 *            Can't read from the existing approvedClients.txt file!
	 */
	private void addFromApprovedList() throws IOException {
		BufferedReader buffR;
		buffR = new BufferedReader(new FileReader(FileLocations.CONVERTED_FILE_LOCATION));

		Scanner sc = new Scanner(buffR);
		while (sc.hasNextLine()) {
			String line = sc.nextLine().trim();
			StringTokenizer tok = new StringTokenizer(line);
			if (tok.countTokens() == 2)
				approvedClients.put(tok.nextToken(), tok.nextToken());
		}

		sc.close();
		buffR.close();
	}

	/**
	 * Starts the server and waits for connections.
	 */
	@Override
	public void run() {
		SSLServerSocketFactory ssf = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

		try {
			ServerSocket serverSocket = ssf.createServerSocket(serverPort);

			Logger.info("Waiting for clients on port: {}", serverPort);

			while (run) {
				Socket clientSocket = serverSocket.accept();

				if (!run)
					break; // Previous call is blocking, so check again to avoid IOExceptions or stalled threads.

				ClientThread client = new ClientThread(clientSocket, this);
				clients.add(client);

				client.run();
			} // end run loop

			try {
				serverSocket.close();

				for (ClientThread client : clients)
					client.close();
			} catch (IOException ioe) {
				Logger.error("Error closing the serverSocket: {}", ioe);
			}
		} catch (IOException ioe) {
			Logger.error("IOException while creating ServerSocket on port: {}\n{}", serverPort, ioe);
		}
	}

	/**
	 * Gracefully shuts the server down by tripping the flag.
	 */
	public void stop() {
		run = false;

		// Break a blocking call by connecting to the socket.
		try {
			new Socket("localhost", serverPort);
		} catch (IOException ioe) {
			Logger.error("Error trying to stop the server: {}", ioe);
		}
	}

	/**
	 * Creates a new map by calling the appropriate daemon methods.
	 * 
	 * @param afm
	 *           The AsciiFileMessage that represents the instructions for this map's creation.
	 * @param client
	 *           A reference to the ClientThread that is making the call (to return error or success messages).
	 */
	public synchronized void parseAsciiFileMessage(AsciiFileMessage afm, ClientThread client) {
		try {
			if (afm.getOverwriteExisting()) {
				if (!EarthModellingDaemon.removeExistingMap(afm.getMapProperties()))
					client.bufferMessage(new StringMessage(StringMessage.Type.ERROR_MESSAGE, "There was an issue removing the existing map. Check the server logs for more information."));
			}

			if (!EarthModellingDaemon.createMap(afm.getFile(), afm.getMapProperties()))
				client.bufferMessage(new StringMessage(StringMessage.Type.ERROR_MESSAGE, "There was an issue creating the new map. Is it possible that the map you wish to create already exists? If not, check the server logs."));
			else
				client.bufferMessage(new StringMessage(StringMessage.Type.MESSAGE, "The map " + afm.getMapProperties().toString() + " was sucessfully created"));
		} catch (IllegalAccessException iae) {
			Logger.error("StringMessage message was defined with incorrect parameters: {}", iae);
		}
	}

	/**
	 * Remove the stored reference after the client disconnects.
	 * 
	 * @param client
	 *           The ClientThread that will be disconnected.
	 */
	public synchronized void removeClient(ClientThread client) {
		clients.remove(client);
	}

	/**
	 * Validates a user by checking against the approvedClients HashMap.
	 * 
	 * @param username
	 *           The username of the client to validate.
	 * @param password
	 *           The password associated with the username you wish to validate.
	 * @param socket
	 *           The Socket that the client is connecting on (for logging functionality).
	 * @return true if the user is authorized to access the web interface; false otherwise.
	 */
	public synchronized boolean validateUser(String username, String password, Socket socket) {
		if (!approvedClients.containsKey(username)) {
			Logger.warn("Authentication failure with username: {} @ {}", username, socket.getInetAddress());
			return false;
		}

		// TO-DO
		// Salt passwords instead of storing as plaintext.
		if (!approvedClients.get(username).equals(password)) {
			Logger.warn("Authentication failure with username: {}, password: {}, @ {}", username, password, socket.getInetAddress());
			return false;
		}

		Logger.info("Username: {} connected from: {}", username, socket.getInetAddress());
		return true;
	}
}