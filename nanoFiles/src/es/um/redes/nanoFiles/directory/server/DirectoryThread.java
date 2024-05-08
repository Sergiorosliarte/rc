package es.um.redes.nanoFiles.directory.server;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;

import es.um.redes.nanoFiles.directory.message.DirMessage;
import es.um.redes.nanoFiles.directory.message.DirMessageOps;

public class DirectoryThread extends Thread {

	/**
	 * Socket de comunicación UDP con el cliente UDP (DirectoryConnector)
	 */
	protected DatagramSocket socket = null;

	/**
	 * Probabilidad de descartar un mensaje recibido en el directorio (para simular
	 * enlace no confiable y testear el código de retransmisión)
	 */
	protected double messageDiscardProbability;

	/**
	 * Estructura para guardar los nicks de usuarios registrados, y la fecha/hora de
	 * registro
	 * 
	 */
	private HashMap<String, LocalDateTime> nicks;
	/**
	 * Estructura para guardar los usuarios servidores (nick, direcciones de socket
	 * TCP)
	 */
	// TCP)
	private HashMap<String, InetSocketAddress> servers;

	public DirectoryThread(int directoryPort, double corruptionProbability) throws SocketException {
		this.socket = new DatagramSocket(directoryPort);
		messageDiscardProbability = corruptionProbability;
		this.nicks = new HashMap<String, LocalDateTime>();
		this.servers = new HashMap<String, InetSocketAddress>();

	}

	public void run() {
		byte[] receptionBuffer = new byte[DirMessage.PACKET_MAX_SIZE];
		DatagramPacket requestPacket = new DatagramPacket(receptionBuffer, receptionBuffer.length);
		InetSocketAddress clientId = null;

		System.out.println("Directory starting...");

		while (true) {
			try {

				socket.receive(requestPacket);
				clientId = (InetSocketAddress) requestPacket.getSocketAddress();
				
				// Vemos si el mensaje debe ser descartado por la probabilidad de descarte

				double rand = Math.random();
				if (rand < messageDiscardProbability) {
					System.err.println("Directory DISCARDED datagram from " + clientId);
					continue;
				}

				// Analizamos la solicitud y la procesamos

				if (requestPacket.getLength() > 0) {
					processRequestFromClient(requestPacket.getData(), requestPacket.getLength(), clientId);
				} else {
					System.err.println("Directory received EMPTY datagram from " + clientId);
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Terminal error, connection interrupted.");
				break;
			}
		}
		// Cerrar el socket
		socket.close();
	}

	// Método para procesar la solicitud enviada por clientAddr
	public void processRequestFromClient(byte[] data, int lenght, InetSocketAddress clientAddr) throws IOException {
		DirMessage clientM = DirMessage.buildMessageFromReceivedData(data);
		System.out.println("Message received from user "+clientAddr.toString());
		switch (clientM.getOpcode()) {
		case (DirMessageOps.OPCODE_LOGIN):
			sendLoginOK(clientAddr);
			break;
		case (DirMessageOps.OPCODE_REGISTER_USERNAME):
			sendRegisterConfirmation(clientM.getUserName(), clientAddr);
			break;
		case (DirMessageOps.OPCODE_GETUSERS):
			sendUserList(clientAddr);
			break;
		case (DirMessageOps.OPCODE_LOOKUP_USERNAME):
			sendUsernameLookup(clientM.getUserName(), clientAddr);
			break;
		case (DirMessageOps.OPCODE_SERVE_FILES):
			sendServingConfirmation(clientM.getUserName(), clientM.getPort(), clientAddr);
			break;
		case (DirMessageOps.OPCODE_STOPPED_SERVING):
			sendServingStopConfirmation(clientM.getUserName(), clientAddr);
			break;
	case (DirMessageOps.OPCODE_LOGOUT):
		sendLogoutConfirmation(clientM.getUserName(), clientAddr);
		break;
		}
	}

	// Método para enviar la confirmación del registro
	private void sendLoginOK(InetSocketAddress clientAddr) throws IOException {
		byte[] responseData = DirMessage.buildLoginOKResponseMessage(servers.size());
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
	}

	private void sendRegisterConfirmation(String nick, InetSocketAddress clientAddr) throws IOException {
		if (nicks.containsKey(nick)) {
			byte responseData[] = DirMessage.buildRegisterNOTOKResponseMessage();
			DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
			socket.send(responsePacket);
		} else {
			nicks.put(nick, LocalDateTime.now());
			byte responseData[] = DirMessage.buildRegisterOKResponseMessage();
			DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
			socket.send(responsePacket);
		}
	}

	private void sendUserList(InetSocketAddress clientAddr) throws IOException {
		LinkedList<String> nombres = new LinkedList<>(nicks.keySet());
		
		int tam = 7;
		String cad = "nicks: ";
		boolean end = false;
		while (!end) {
			if (nombres.isEmpty()) {
				end = true;
			}
			else {
				String s = nombres.getFirst();
				if (servers.containsKey(s)) {
					if (tam < (65502-s.length()-6)) {
						tam += s.length() + 6;
						cad = cad.concat(s);
						cad = cad.concat(" - S; ");
						nombres.removeFirst();
					}
					else end = true;
				}
				else {
					if (tam < (65502-s.length()-2)) {
						tam += s.length() + 2;
						cad = cad.concat(s);
						cad = cad.concat("; ");
						nombres.removeFirst();
					}
					else end = true;
				}
			}
			
		}
		byte responseData[] = DirMessage.buildUserListResponseMessage(tam, cad);
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
		
	}
	
	private void sendUsernameLookup (String user, InetSocketAddress clientAddr) throws IOException {
		if (servers.containsKey(user)) {
			byte responseData[] = DirMessage.buildUsernameFoundResponseMessage(user, servers.get(user));
			DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
			socket.send(responsePacket);
		}
		else {
			byte responseData[] = DirMessage.buildUsernameNOTFoundResponseMessage();
			DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
			socket.send(responsePacket);
		}
	}
	
	private void sendServingConfirmation(String nick, int port, InetSocketAddress clientAddr) throws IOException {
		InetSocketAddress dir = new InetSocketAddress(clientAddr.getAddress(),port);
		if (servers.containsKey(nick)) {
			if (servers.get(nick).equals(dir)) {
				byte responseData[] = DirMessage.buildAlreadyServingResponseMessage();
				DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
				socket.send(responsePacket);
			}
			else {
				byte responseData[] = DirMessage.buildServingNOTOKResponseMessage();
				DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
				socket.send(responsePacket);
			}
			
		} else {
			servers.put(nick, dir);
			byte responseData[] = DirMessage.buildServingOKResponseMessage();
			DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
			socket.send(responsePacket);
		}
	}
	
	private void sendServingStopConfirmation(String nick, InetSocketAddress clientAddr) throws IOException {
		assert (servers.containsKey(nick));
		servers.remove(nick);
		byte responseData[] = DirMessage.buildServingStopResponseMessage();
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
	}
	
	private void sendLogoutConfirmation(String nick, InetSocketAddress clientAddr) throws IOException {
		assert (nicks.containsKey(nick));
		nicks.remove(nick);
		byte responseData[] = DirMessage.buildLogoutResponseMessage();
		DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddr);
		socket.send(responsePacket);
	}

}
