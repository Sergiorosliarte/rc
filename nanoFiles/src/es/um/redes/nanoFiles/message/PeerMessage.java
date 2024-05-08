package es.um.redes.nanoFiles.message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.stream.Collectors;

/**
 * Clase que modela los mensajes del protocolo de comunicación entre pares para
 * implementar el explorador de ficheros remoto (servidor de ficheros). Estos
 * mensajes son intercambiados entre las clases NFServerComm y NFConnector, y se
 * codifican como texto en formato "campo:valor".
 * 
 * @author rtitos
 *
 */
public class PeerMessage {
	private static final char DELIMITER = ':'; // Define el delimitador
	private static final char END_LINE = '\n'; // Define el carácter de fin de línea

	/**
	 * Nombre del campo que define el tipo de mensaje (primera línea)
	 */
	private static final String FIELDNAME_OPERATION = "operation";
	private static final String FIELDNAME_FILEHASH = "filehash";
	private static final String FIELDNAME_FILES = "files";
	private static final String FIELDNAME_SIZE = "size";
	/**
	 * Tipo del mensaje, de entre los tipos definidos en PeerMessageOps.
	 */
	private String operation;
	private String filehash;
	private Integer filesize;
	  
	public PeerMessage (String op, int size) {
		this.operation = op;
		this.filesize = size;
	}
	
	public PeerMessage (String op, String filehash) {
		this.operation = op;
		this.filehash = filehash;
	}
	
	public PeerMessage (String op) {
		this.operation = op;
	}

	public String getOperation() {
		return operation;
	}
	
	public String getFilehash() {
		return filehash;
	}
	
	public int getFilesize() {
		return filesize;
	}

	/**
	 * Método que convierte un mensaje codificado como una cadena de caracteres, a
	 * un objeto de la clase PeerMessage, en el cual los atributos correspondientes
	 * han sido establecidos con el valor de los campos del mensaje.
	 * 
	 * @param message El mensaje recibido por el socket, como cadena de caracteres
	 * @return Un objeto PeerMessage que modela el mensaje recibido (tipo, valores,
	 *         etc.)
	 * @throws IOException 
	 */
	public static PeerMessage fromString(String message) throws IOException {
		BufferedReader reader = new BufferedReader(new StringReader(message));
		String line = reader.readLine();
		int idx = line.indexOf(DELIMITER);
		String op = line.substring(idx+1).trim();
		if (op.equals(PeerMessageOps.OP_FILECODE)){
			String f = reader.lines().collect(Collectors.joining());
					
			return new PeerMessage (op,f);
		}
		
		if (op.equals(PeerMessageOps.OP_QUERY_RESPONSE)){
			String value = new String();
			line = reader.readLine();
			if (!line.equals("\n")) { 
				idx = line.indexOf(DELIMITER);
				value = line.substring(idx+1);
				}
			String f = value;
			f = f.concat("\n");
			f = f.concat(reader.lines().collect(Collectors.joining(System.lineSeparator())));
					
			return new PeerMessage (op,f);
		}
		
		String value = new String();
		line = reader.readLine();
		if (!line.equals("\n")) { //Si no hemos llegado al fin del mensaje seguimos
			idx = line.indexOf(DELIMITER); //Posición del delimitador
			value = line.substring(idx+1).trim(); //trim() borra espacios
			}

		switch (op) {
		case (PeerMessageOps.OP_DOWNLOAD):
			return new PeerMessage (op,value);
		case (PeerMessageOps.OP_DOWNLOAD_FAIL):
			return new PeerMessage (op);
		case (PeerMessageOps.OP_FILESIZE):
			return new PeerMessage (op,Integer.parseInt(value));
		case (PeerMessageOps.OP_QUERY):
			return new PeerMessage (op);
		case (PeerMessageOps.OP_CLOSE):
			return new PeerMessage (op);
		}
		return null;
	}

	/**
	 * Método que devuelve una cadena de caracteres con la codificación del mensaje
	 * según el formato campo:valor, a partir del tipo y los valores almacenados en
	 * los atributos.
	 * 
	 * @return La cadena de caracteres con el mensaje a enviar por el socket.
	 */
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();
		sb.append(FIELDNAME_OPERATION+DELIMITER+operation+END_LINE);
		switch (operation) {
		case (PeerMessageOps.OP_DOWNLOAD):
			sb.append(FIELDNAME_FILEHASH+DELIMITER+filehash+END_LINE+END_LINE);
			break;
		case (PeerMessageOps.OP_DOWNLOAD_FAIL):
			sb.append(END_LINE);
			break;
		case (PeerMessageOps.OP_FILESIZE):
			sb.append(FIELDNAME_SIZE+DELIMITER+filesize.toString()+END_LINE+END_LINE);
			break;
		case (PeerMessageOps.OP_FILECODE):
			sb.append(filehash);
			break;
		case (PeerMessageOps.OP_QUERY):
			sb.append(END_LINE);
			break;
		case (PeerMessageOps.OP_QUERY_RESPONSE):
			sb.append(FIELDNAME_FILES+DELIMITER+filehash+END_LINE);
			break;
		case (PeerMessageOps.OP_CLOSE):
			sb.append(END_LINE);
			break;
		}
		return sb.toString();
	}
	
}
