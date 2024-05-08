package es.um.redes.nanoFiles.client.application;

import java.io.IOException;
import java.net.InetSocketAddress;
import es.um.redes.nanoFiles.directory.connector.DirectoryConnector;

public class NFControllerLogicDir {
	// Conector para enviar y recibir mensajes del directorio
	private DirectoryConnector directoryConnector;

	/**
	 * Método para conectar con el directorio y obtener el número de peers que están
	 * sirviendo ficheros
	 * 
	 * @param directoryHostname el nombre de host/IP en el que se está ejecutando el
	 *                          directorio
	 * @return true si se ha conseguido contactar con el directorio.
	 */
	boolean logIntoDirectory(String directoryHostname) {

		try {
			this.directoryConnector = new DirectoryConnector(directoryHostname);
		} catch (IOException e) {
			System.out.println("EL servidor al que se quiso conectar no existe.\n");
			return false;
		}
		int response = directoryConnector.logIntoDirectory();
		if (response >= 0) {
			System.out.println("Servidores conectados: "+response+".\n");
			return true;
		}
		else
		{
			System.out.println("Error inesperado al intentar el login.\n");
			return false;
		}
	}

	/**
	 * Método para registrar el nick del usuario en el directorio
	 * 
	 * @param nickname el nombre de usuario a registrar
	 * @return true si el nick es válido (no contiene ":") y se ha registrado
	 *         nickname correctamente con el directorio (no estaba duplicado), falso
	 *         en caso contrario.
	 */
	boolean registerNickInDirectory(String nickname) {
		
		boolean result = directoryConnector.registerNickname(nickname);
		if (result) {
			System.out.println("Registro realizado.");
			return true;
		}
		else {
			System.out.println("El nick escogido ya está registrado.");
			return false;
		}
	}

	/**
	 * Método para obtener de entre los peer servidores registrados en el directorio
	 * la IP:puerto del peer con el nick especificado
	 * 
	 * @param nickname el nick del peer por cuya IP:puerto se pregunta
	 * @return La dirección de socket del peer identificado por dich nick, o null si
	 *         no se encuentra ningún peer con ese nick.
	 */
	InetSocketAddress lookupUserInDirectory(String nickname) {
		return directoryConnector.lookupUsername(nickname);
	}

	/**
	 * Método para publicar la lista de ficheros que este peer está compartiendo.
	 * 
	 * @param port     El puerto en el que este peer escucha solicitudes de conexión
	 *                 de otros peers.
	 * @param nickname El nick de este peer, que será asociado a lista de ficheros y
	 *                 su IP:port
	 */
	public boolean publishLocalFilesToDirectory(int port, String nickname) {
		return directoryConnector.serveFiles(port, nickname);
	}
	
	public boolean stopServing(String nick) {
		return directoryConnector.communicateServingStop(nick);
	}

	/**
	 * Método para obtener y mostrar la lista de nicks registrados en el directorio
	 */
	public boolean getUserListFromDirectory() {
		String response = directoryConnector.getDirectoryUserList();
		if (response != null) {
			System.out.println(response);
			return true;
		}
		return false;
	}

	/**
	 * Método para desconectarse del directorio (cerrar sesión)
	 */
	public void logout(String nick) {
		boolean result = directoryConnector.logout(nick);
		if (result) {
			System.out.println("Nick dado de baja del directorio.");
		}
		else {
			System.out.println("Error dando de baja el nick.");
		}
	}
}
