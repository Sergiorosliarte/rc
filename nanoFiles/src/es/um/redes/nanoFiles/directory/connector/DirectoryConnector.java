package es.um.redes.nanoFiles.directory.connector;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import es.um.redes.nanoFiles.directory.message.DirMessage;

/**
 * Cliente con métodos de consulta y actualización específicos del directorio
 */
public class DirectoryConnector {
	/**
	 * Puerto en el que atienden los servidores de directorio
	 */
	private static final int DEFAULT_PORT = 6868;
	/**
	 * Tiempo máximo en milisegundos que se esperará a recibir una respuesta por el
	 * socket antes de que se deba lanzar una excepción SocketTimeoutException para
	 * recuperar el control
	 */
	private static final int TIMEOUT = 1000;
	/**
	 * Número de intentos máximos para obtener del directorio una respuesta a una
	 * solicitud enviada. Cada vez que expira el timeout sin recibir respuesta se
	 * cuenta como un intento.
	 */
	private static final int MAX_NUMBER_OF_ATTEMPTS = 5;

	/**
	 * Socket UDP usado para la comunicación con el directorio
	 */
	private DatagramSocket socket;
	/**
	 * Dirección de socket del directorio (IP:puertoUDP)
	 */
	private InetSocketAddress directoryAddress;

	public DirectoryConnector(String address) throws IOException {
		this.directoryAddress = new InetSocketAddress(InetAddress.getByName(address), DEFAULT_PORT);
		this.socket = new DatagramSocket();
	}

	/**
	 * Método para enviar y recibir datagramas al/del directorio
	 * 
	 * @param requestData los datos a enviar al directorio (mensaje de solicitud)
	 * @return los datos recibidos del directorio (mensaje de respuesta)
	 */
	public byte[] sendAndReceiveDatagrams(byte[] requestData) {
		byte responseData[] = new byte[DirMessage.PACKET_MAX_SIZE];

		DatagramPacket packetToServer = new DatagramPacket(requestData, requestData.length, directoryAddress);
		DatagramPacket receivedPacket = new DatagramPacket(responseData, responseData.length);
		boolean received = false;
		int aux = 0;
		while(!received && aux < MAX_NUMBER_OF_ATTEMPTS) {
			try{
				socket.send(packetToServer);
				try {
					socket.setSoTimeout(TIMEOUT);
					socket.receive(receivedPacket);
					received = true;
				}catch (SocketException s) {}
			}
			catch (IOException e) {System.out.println("No se recibió nada, reenviando paquete."); aux++;}
		}
		if (!received) 
			return null;
		else return responseData;
	}

	public int logIntoDirectory() { // Returns number of file servers
		byte[] requestData = DirMessage.buildLoginRequestMessage();
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processLoginResponseMessage(responseData);
	}

	public boolean registerNickname(String nick) {
		byte[] requestData = DirMessage.buildRegisterRequestMessage(nick);
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processRegisterResponseMessage(responseData);
	}
	
	public String getDirectoryUserList() {
		byte[] requestData = DirMessage.buildUserListRequestMessage();
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processUserListResponseMessage(responseData);
	}
	
	public InetSocketAddress lookupUsername (String nick) {
		byte[] requestData = DirMessage.buildLookupUsernameRequestMessage(nick);
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processLookupUsernameResponseMessage(responseData);
	}
	
	public boolean serveFiles(int port, String nick) { // Returns number of file servers
		byte[] requestData = DirMessage.buildServingRequestMessage(port, nick);
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processServingResponseMessage(responseData);
	}
	
	public boolean communicateServingStop(String nick) { // Returns number of file servers
		byte[] requestData = DirMessage.buildServingStopRequestMessage(nick);
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processServingStopResponseMessage(responseData);
	}
	
	public boolean logout(String nick) {
		byte[] requestData = DirMessage.buildLogoutRequestMessage(nick);
		byte[] responseData = this.sendAndReceiveDatagrams(requestData);
		return DirMessage.processLogoutResponseMessage(responseData);
	}
}
