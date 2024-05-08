package es.um.redes.nanoFiles.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class NFServerSimple {

	private static final int SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS = 8000;
	private static final String STOP_SERVER_COMMAND = "fgstop";
	private ServerSocket serverSocket = null;

	public NFServerSimple(int port) throws IOException {
		
		InetSocketAddress FileServerSocketAddress = new InetSocketAddress(port);
		serverSocket = new ServerSocket();
		serverSocket.bind(FileServerSocketAddress);
		serverSocket.setSoTimeout(SERVERSOCKET_ACCEPT_TIMEOUT_MILISECS);
	}

	/**
	 * Método para ejecutar el servidor de ficheros en primer plano. Sólo es capaz
	 * de atender una conexión de un cliente. Una vez se lanza, ya no es posible
	 * interactuar con la aplicación a menos que se implemente la funcionalidad de
	 * detectar el comando STOP_SERVER_COMMAND (opcional)
	 * @throws  
	 * 
	 */
	public void run()  {

		boolean stopServer = false;
		BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter '" + STOP_SERVER_COMMAND + "' to stop the server");
		while (!stopServer) {
			
			try (Socket socket = serverSocket.accept();){
				NFServerComm.serveFilesToClient(socket);
			} catch (IOException e) {
				System.out.println("Listening...");
			}
			
			try {
				if (bf.ready()) {
					String b = bf.readLine();
					if (b.equals("fgstop"))
						stopServer = true;
				}
			} catch (IOException e) {}
			
		}
		System.out.println("NFServerSimple stopped");

	}
}
