package es.um.redes.nanoFiles.directory.message;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class DirMessage {

	public static final int PACKET_MAX_SIZE = 65507;

	public static final byte OPCODE_SIZE_BYTES = 1;

	private byte opcode;

	private String userName;
	
	private int port;

	public DirMessage(byte operation) {
		assert (operation == DirMessageOps.OPCODE_LOGIN || operation == DirMessageOps.OPCODE_GETUSERS);
		opcode = operation;
	}
	
	public DirMessage(byte operation, String nick) {
		assert (opcode == DirMessageOps.OPCODE_REGISTER_USERNAME || opcode == DirMessageOps.OPCODE_LOOKUP_USERNAME
				|| opcode == DirMessageOps.OPCODE_LOGOUT || opcode == DirMessageOps.OPCODE_STOPPED_SERVING);
		opcode = operation;
		userName = nick;
	}
	
	public DirMessage(byte operation, String nick, int port) {
		assert (opcode == DirMessageOps.OPCODE_SERVE_FILES);
		opcode = operation;
		userName = nick;
		this.port = port;
	}

	/**
	 * Método para obtener el tipo de mensaje (opcode)
	 * 
	 * @return
	 */
	public byte getOpcode() {
		return opcode;
	}

	public String getUserName() {
		if (userName == null) {
			System.err.println(
					"PANIC: DirMessage.getUserName called but 'userName' field is not defined for messages of type "
							+ DirMessageOps.opcodeToOperation(opcode));
			System.exit(-1);
		}
		return userName;
	}
	
	public int getPort() {
		return port;
	}

	/**
	 * Método de clase para parsear los campos de un mensaje y construir el objeto
	 * DirMessage que contiene los datos del mensaje recibido
	 * 
	 * @param data El
	 * @return
	 */
	public static DirMessage buildMessageFromReceivedData(byte[] data) {
		ByteBuffer bb = ByteBuffer.wrap(data);
		byte op = bb.get();
		switch(op) {
			case (DirMessageOps.OPCODE_LOGIN):
				return new DirMessage(op);
			case (DirMessageOps.OPCODE_GETUSERS):
				return new DirMessage(op);
			case (DirMessageOps.OPCODE_REGISTER_USERNAME):
				int l = bb.getInt();
				byte[] n = new byte[l];
				bb.get(n);
				return new DirMessage(op, new String(n));
			case (DirMessageOps.OPCODE_LOOKUP_USERNAME):
				int a = bb.getInt();
				byte[] ni = new byte[a];
				bb.get(ni);
				return new DirMessage(op, new String(ni));
			case (DirMessageOps.OPCODE_SERVE_FILES):
				int lo = bb.getInt();
				byte[] nick = new byte[lo];
				bb.get(nick);
				int p = bb.getInt();
				return new DirMessage(op,new String(nick), p);
			case (DirMessageOps.OPCODE_STOPPED_SERVING):
				int nic1 = bb.getInt();
				byte[] n1 = new byte[nic1];
				bb.get(n1);
				return new DirMessage(op, new String(n1));
			case (DirMessageOps.OPCODE_LOGOUT):
				int nic2 = bb.getInt();
				byte[] n2 = new byte[nic2];
				bb.get(n2);
				return new DirMessage(op, new String(n2));
		}
		return null;
	}

	/**
	 * Método para construir una solicitud de ingreso en el directorio
	 * 
	 * @return El array de bytes con el mensaje de solicitud de login
	 */
	public static byte[] buildLoginRequestMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_LOGIN);
		return bb.array();
	}

	/**
	 * Método para construir una respuesta al ingreso del peer en el directorio
	 * 
	 * @param numServers El número de peer registrados como servidor en el
	 *                   directorio
	 * @return El array de bytes con el mensaje de solicitud de login
	 */
	public static byte[] buildLoginOKResponseMessage(int numServers) {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES + Integer.BYTES);
		bb.put(DirMessageOps.OPCODE_LOGIN_OK);
		bb.putInt(numServers);
		return bb.array();
	}

	/**
	 * Método que procesa la respuesta a una solicitud de login
	 * 
	 * @param data El mensaje de respuesta recibido del directorio
	 * @return El número de peer servidores registrados en el directorio en el
	 *         momento del login, o -1 si el login en el servidor ha fallado
	 */
	public static int processLoginResponseMessage(byte[] data) {
		if (data == null) {
			System.out.println("Directorio inaccesible, sal con <quit>");
			return -1;
		}
		ByteBuffer buf = ByteBuffer.wrap(data);
		byte opcode = buf.get();
		if (opcode == DirMessageOps.OPCODE_LOGIN_OK) {
			return buf.getInt(); // Return number of available file servers
		} else {
			return -1;
		}
	}
	
	public static byte[] buildRegisterRequestMessage(String nick) {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES+Integer.BYTES+nick.length());
		bb.put(DirMessageOps.OPCODE_REGISTER_USERNAME);
		bb.putInt(nick.length());
		bb.put(nick.getBytes());
		return bb.array();
	}

	public static byte[] buildRegisterOKResponseMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_REGISTER_USERNAME_OK);
		return bb.array();
	}

	public static byte[] buildRegisterNOTOKResponseMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_REGISTER_USERNAME_FAIL);
		return bb.array();
	}

	public static boolean processRegisterResponseMessage(byte[] data) {
		if (data == null) {
			System.out.println("Directorio inaccesible, sal con <quit>");
			return false;
		}
		ByteBuffer buf = ByteBuffer.wrap(data);
		byte opcode = buf.get();
		if (opcode == DirMessageOps.OPCODE_REGISTER_USERNAME_OK) {
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public static byte[] buildUserListRequestMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_GETUSERS);
		return bb.array();
	}

	public static byte[] buildUserListResponseMessage(int tam, String nombres) {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES+Integer.BYTES+tam);
		bb.put(DirMessageOps.OPCODE_USERLIST);
		bb.putInt(tam);
		bb.put(nombres.getBytes());
		return bb.array();
	}

	public static String processUserListResponseMessage(byte[] data) {
		if (data == null) {
			System.out.println("Directorio inaccesible, sal con <quit>");
			return null;
		}
		ByteBuffer buf = ByteBuffer.wrap(data);
		byte opcode = buf.get();
		if (opcode != DirMessageOps.OPCODE_USERLIST) {
			return "Error al consultar la base de datos de usuarios.";
		}
		else {
			int l = buf.getInt();
			byte[] list = new byte[l];
			buf.get(list);
			return new String(list);
		}
	}
	
	public static byte[] buildLookupUsernameRequestMessage (String nick) {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES+Integer.BYTES+nick.length());
		bb.put(DirMessageOps.OPCODE_LOOKUP_USERNAME);
		bb.putInt(nick.length());
		bb.put(nick.getBytes());
		return bb.array();
	}
	
	public static byte[] buildUsernameFoundResponseMessage (String user, InetSocketAddress serverAddr) {
		InetAddress serverIP = serverAddr.getAddress();
		byte[] IP = serverIP.getAddress();
		int port = serverAddr.getPort();
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES+4+Integer.BYTES);
		bb.put(DirMessageOps.OPCODE_LOOKUP_USERNAME_FOUND);
		bb.put(IP);
		bb.putInt(port);
		return bb.array();
		
	}
	
	public static byte[] buildUsernameNOTFoundResponseMessage () {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_LOOKUP_USERNAME_NOTFOUND);
		return bb.array();
	}
	
	public static InetSocketAddress processLookupUsernameResponseMessage(byte[] data) {
		if (data == null) {
			System.out.println("Directorio inaccesible, sal con <quit>");
			return null;
		}
		ByteBuffer bb = ByteBuffer.wrap(data);
		byte opcode = bb.get();
		if (opcode != DirMessageOps.OPCODE_LOOKUP_USERNAME_FOUND) {
			return null;
		}
		else {
			byte[] ip = new byte[4];
			bb.get(ip);
			InetAddress r;
			try {
				r = InetAddress.getByAddress(ip);
			} catch (UnknownHostException e) {
				return null;
			}
			InetSocketAddress response = new InetSocketAddress(r, bb.getInt());
			return response;
		}
	}
	
	public static byte[] buildServingRequestMessage (int port, String nick) {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES+Integer.BYTES+nick.length()+Integer.BYTES);
		bb.put(DirMessageOps.OPCODE_SERVE_FILES);
		bb.putInt(nick.length());
		bb.put(nick.getBytes());
		bb.putInt(port);
		return bb.array();
	}
	
	public static byte[] buildAlreadyServingResponseMessage () {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_SERVE_FILES_ALREADY);
		return bb.array();
	}
	
	public static byte[] buildServingOKResponseMessage () {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_SERVE_FILES_OK);
		return bb.array();
	}
	
	public static byte[] buildServingNOTOKResponseMessage () {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_SERVE_FILES_FAIL);
		return bb.array();
	}
	
	public static boolean processServingResponseMessage (byte[] data) {
		if (data == null) {
			System.out.println("Directorio inaccesible, sal con <quit>");
			return false;
		}
		ByteBuffer buf = ByteBuffer.wrap(data);
		byte opcode = buf.get();
		switch (opcode) {
		case (DirMessageOps.OPCODE_SERVE_FILES_OK):
			return true;
		case (DirMessageOps.OPCODE_SERVE_FILES_FAIL):
			System.out.println("Otro servidor ya usa ese nickname.");
			return false;
		case (DirMessageOps.OPCODE_SERVE_FILES_ALREADY):
			System.out.println("Ya se están sirviendo ficheros con ese nickname.");
			return false;
		}
		return false;
	}
	
	public static byte[] buildServingStopRequestMessage (String nick) {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES+Integer.BYTES+nick.length());
		bb.put(DirMessageOps.OPCODE_STOPPED_SERVING);
		bb.putInt(nick.length());
		bb.put(nick.getBytes());
		return bb.array();
	}
	
	public static byte[] buildServingStopResponseMessage () {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_STOPPED_SERVING_OK);
		return bb.array();
	}
	
	public static boolean processServingStopResponseMessage (byte[] data) {
		if (data == null) {
			System.out.println("Directorio inaccesible, sal con <quit>");
			return false;
		}
		ByteBuffer buf = ByteBuffer.wrap(data);
		byte opcode = buf.get();
		assert (opcode == DirMessageOps.OPCODE_STOPPED_SERVING_OK);
		return true;
	}
	
	public static byte[] buildLogoutRequestMessage(String nick) {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES+Integer.BYTES+nick.length());
		bb.put(DirMessageOps.OPCODE_LOGOUT);
		bb.putInt(nick.length());
		bb.put(nick.getBytes());
		return bb.array();
	}

	public static byte[] buildLogoutResponseMessage() {
		ByteBuffer bb = ByteBuffer.allocate(DirMessage.OPCODE_SIZE_BYTES);
		bb.put(DirMessageOps.OPCODE_LOGOUT_OK);
		return bb.array();
	}

	public static boolean processLogoutResponseMessage(byte[] data) {
		if (data == null) {
			System.out.println("Directorio inaccesible, sal con <quit>");
			return false;
		}
		ByteBuffer buf = ByteBuffer.wrap(data);
		byte opcode = buf.get();
		if (opcode == DirMessageOps.OPCODE_LOGOUT_OK) {
			return true;
		}
		else return false;
	}

}
