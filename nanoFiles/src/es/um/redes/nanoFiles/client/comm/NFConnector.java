package es.um.redes.nanoFiles.client.comm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import es.um.redes.nanoFiles.message.PeerMessage;
import es.um.redes.nanoFiles.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileDigest;

//Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor de NanoChat
public class NFConnector {
	private Socket socket;
	protected DataOutputStream dos;
	protected DataInputStream dis;

	public NFConnector(InetSocketAddress serverAddress) {
		try {
			socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());
			dos = new DataOutputStream (socket.getOutputStream());
			dis = new DataInputStream (socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Método que utiliza el Shell para ver si hay datos en el flujo de entrada.
	 * Permite "sondear" el socket con el fin evitar realizar una lectura bloqueante
	 * y así poder realizar otras tareas mientras no se ha recibido ningún mensaje.
	 * 
	 * @return
	 * @throws IOException
	 */
	public boolean isDataAvailable() throws IOException {
		return (dis.available() != 0);
	}

	/**
	 * Método para descargar un fichero a través del socket mediante el que estamos
	 * conectados con un peer servidor.
	 * 
	 * @param targetFileHashSubstr El hash del fichero a descargar
	 * @param file                 El objeto File que referencia el nuevo fichero
	 *                             creado en el cual se escriben los datos
	 *                             descargados del servidor (contenido del fichero
	 *                             remoto)
	 * @return Verdadero si la descarga se completa con éxito, falso en caso
	 *         contrario.
	 * @throws IOException Si se produce algún error al leer/escribir del socket.
	 */
	public boolean download(String targetFileHashSubstr, File file) throws IOException {
		boolean downloaded = false;
		
		dos.writeUTF(buildDownloadRequest(targetFileHashSubstr));
		
		String s = dis.readUTF();
		PeerMessage p = PeerMessage.fromString(s);
		if (p.getOperation().equals(PeerMessageOps.OP_DOWNLOAD_FAIL)) {
			System.out.println("El fichero que se quiere consultar no existe en este servidor");
			return false;
		}
		else if (p.getOperation().equals(PeerMessageOps.OP_FILESIZE)) {
			int packs = p.getFilesize();
			System.out.println("Descargando "+targetFileHashSubstr+". Se descargarán "+packs+" paquetes.");
			FileOutputStream fo = new FileOutputStream(file);
			for (int i=0; i<packs; i++){
				String m =  dis.readUTF();
				PeerMessage code = PeerMessage.fromString(m);
				assert (code.getOperation() == PeerMessageOps.OP_FILECODE);
				byte[] codigo = java.util.Base64.getDecoder().decode(code.getFilehash());
				fo.write(codigo);
			}
			fo.close();
			if (FileDigest.getChecksumHexString(FileDigest.computeFileChecksum(file.getName())).equals(targetFileHashSubstr)) {
				System.out.println("Fichero descargado.");
				downloaded = true;
			}
				
			else System.out.println("Error descargando fichero corrupto.");
		}

		return downloaded;
	}
	
	public boolean closeConnection() throws IOException{
		dos.writeUTF(buildCloseRequest());
		
		String s = dis.readUTF();
		PeerMessage p = PeerMessage.fromString(s);
		assert (p.getOperation() == PeerMessageOps.OP_CLOSE);
		return true;
	}
	
	public boolean queryFiles() throws IOException{
		dos.writeUTF(buildQueryRequest());
		
		String s = dis.readUTF();
		PeerMessage p = PeerMessage.fromString(s);
		assert (p.getOperation() == PeerMessageOps.OP_QUERY_RESPONSE);
		System.out.println(p.getFilehash());
		return true;
	}
	
	private String buildDownloadRequest (String fileHash) {
		PeerMessage m = new PeerMessage(PeerMessageOps.OP_DOWNLOAD, fileHash);
		return m.toEncodedString();
	}
	
	private String buildCloseRequest () {
		PeerMessage m = new PeerMessage(PeerMessageOps.OP_CLOSE);
		return m.toEncodedString();
	}
	
	private String buildQueryRequest () {
		PeerMessage m = new PeerMessage(PeerMessageOps.OP_QUERY);
		return m.toEncodedString();
	}
}