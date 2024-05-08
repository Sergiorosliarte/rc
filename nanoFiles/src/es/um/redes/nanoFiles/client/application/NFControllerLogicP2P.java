package es.um.redes.nanoFiles.client.application;

import es.um.redes.nanoFiles.client.comm.NFConnector;
import es.um.redes.nanoFiles.server.NFServerSimple;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.regex.*;

public class NFControllerLogicP2P {
	/**
	 * El servidor de ficheros de este peer
	 */
	/**
	 * El cliente para conectarse a otros peers
	 */
	NFConnector nfConnector;
	/**
	 * El controlador que permite interactuar con el directorio
	 */
	private NFControllerLogicDir controllerDir;

	protected NFControllerLogicP2P() {
	}

	protected NFControllerLogicP2P(NFControllerLogicDir controller) {
		// Referencia al controlador que gestiona la comunicación con el directorio
		controllerDir = controller;
	}

	/**
	 * Método para ejecutar un servidor de ficheros en primer plano. Debe arrancar
	 * el servidor y darse de alta en el directorio para publicar el puerto en el
	 * que escucha.
	 * 
	 * 
	 * @param port     El puerto en que el servidor creado escuchará conexiones de
	 *                 otros peers
	 * @param nickname El nick de este peer, parar publicar los ficheros al
	 *                 directorio
	 */
	protected void foregroundServeFiles(int port, String nickname) {
		boolean dirOk = controllerDir.publishLocalFilesToDirectory(port, nickname);
		if (!dirOk)
			System.out.println("No se pudo conectar con el directorio. Comenzando a servir...");
			try {
				NFServerSimple s = new NFServerSimple(port);
				s.run();
			} catch (IOException e) {
				System.out.println("Error creando servidor.");
				e.printStackTrace();
			}
		controllerDir.stopServing(nickname);
	}

	/**
	 * Método para ejecutar un servidor de ficheros en segundo plano. Debe arrancar
	 * el servidor y darse de alta en el directorio para publicar el puerto en el
	 * que escucha.
	 * 
	 * @param port     El puerto en que el servidor creado escuchará conexiones de
	 *                 otros peers
	 * @param nickname El nick de este peer, parar publicar los ficheros al
	 *                 directorio
	 */
	protected void backgroundServeFiles(int port, String nickname) {
		
	}

	/**
	 * Método para establecer una conexión con un peer servidor de ficheros
	 * 
	 * @param nickname El nick del servidor al que conectarse (o su IP:puerto)
	 * @return true si se ha podido establecer la conexión
	 */
	protected boolean browserEnter(String nickname) {
		//Expresión regular para comprobar si el parametro es una IP
		Pattern patron = Pattern.compile("\\d{3}(\\.(\\d){3}){3}:(\\d){4}");
		Matcher m = patron.matcher(nickname);
		if (m.matches()) {
			String ip = nickname.substring(0, nickname.lastIndexOf(":"));
			int port = Integer.parseInt(nickname.substring(nickname.lastIndexOf(":")+1, nickname.length()));
			InetSocketAddress dir = new InetSocketAddress(ip,port);
			nfConnector = new NFConnector(dir);
			System.out.println("Conexión establecida.");
			return true;
		}
		
		else {
			InetSocketAddress dir = controllerDir.lookupUserInDirectory(nickname);
			if (dir == null) {
				System.out.println("No se pudo encontrar la IP del servidor especificado.");
				return false;
			}
			else {
					nfConnector = new NFConnector(dir);
					System.out.println("Conexión establecida.");
					return true;
			}
		}
	}

	/**
	 * Método para descargar un fichero del peer servidor de ficheros al que nos
	 * hemos conectador mediante browser Enter
	 * 
	 * @param targetFileHash El hash del fichero a descargar
	 * @param localFileName  El nombre con el que se guardará el fichero descargado
	 */
	protected void browserDownloadFile(String targetFileHash, String localFileName) {
		File f = new File(localFileName);
		if (!f.exists()) {
		try {
			f.createNewFile();
			nfConnector.download(targetFileHash, f);
		} catch (IOException e) {e.printStackTrace();}
		}
		else System.out.println("El fichero no se puede descargar porque ya hay uno con el mismo nombre en esta máquina.");
		
		

	}

	protected void browserClose() {
		try {
			nfConnector.closeConnection();
		} catch (IOException e) {
			System.out.println("Error al cerrar conexión.");
			e.printStackTrace();
		}
	}

	protected void browserQueryFiles() {
		try {
			nfConnector.queryFiles();
		} catch (IOException e) {
			System.out.println("Error al consultar ficheros disponibles.");
			e.printStackTrace();
		}
	}
}
