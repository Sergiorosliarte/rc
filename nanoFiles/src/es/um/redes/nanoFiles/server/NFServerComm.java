package es.um.redes.nanoFiles.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import es.um.redes.nanoFiles.client.application.NanoFiles;
import es.um.redes.nanoFiles.message.PeerMessage;
import es.um.redes.nanoFiles.message.PeerMessageOps;
import es.um.redes.nanoFiles.util.FileInfo;

public class NFServerComm {

	public static void serveFilesToClient(Socket socket) {
		boolean clientConnected = true;
		// Bucle para atender mensajes del cliente
		try {
			DataInputStream dis = new DataInputStream(socket.getInputStream());
			DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

			while (clientConnected) { // Bucle principal del servidor
				String dataFromClient = dis.readUTF();
				PeerMessage request = PeerMessage.fromString(dataFromClient);
				System.out.print("Petición recibida: ");
				switch (request.getOperation()) {
				case (PeerMessageOps.OP_DOWNLOAD):
					System.out.println("fichero solicitado - "+request.getFilehash());
					String[] p = FileForClient(request.getFilehash());
					dos.writeUTF(p[0]);
					if (p.length > 1) {
						for (int i=1; i<p.length; i++) {
							dos.writeUTF(p[i]);
						}
					}
					break;
				case (PeerMessageOps.OP_QUERY):
					System.out.println("información de ficheros solicitada.");
					dos.writeUTF(FilesListForClient());
					break;
				case (PeerMessageOps.OP_CLOSE):
					System.out.println("fin de conexión.");
					dos.writeUTF(endConfirmationForClient());
					clientConnected = false;
					break;
				}
			}
			socket.close();
		} catch (IOException e) {
			System.out.println("Imposible establecer conexión con cliente.");
			e.printStackTrace();
		}
	}

	public static String[] FileForClient(String filehash) {
		String fpath = NanoFiles.db.lookupFilePath(filehash);
		File f = new File(fpath);
		long filelength = f.length();
		byte data[] = new byte[(int) filelength];

		try {
			FileInputStream fis = new FileInputStream(f);
			fis.read(data);
			fis.close();
		} catch (FileNotFoundException e) {
			System.out.println("Fichero no encontrado");
			String[] z = new String[1];
			PeerMessage x = new PeerMessage(PeerMessageOps.OP_DOWNLOAD_FAIL);
			z[0] = x.toEncodedString();
			return z;
		} catch (IOException e) {
			System.out.println("Error de inputstream cargando fichero");
			e.printStackTrace();
		}
		String file = java.util.Base64.getEncoder().encodeToString(data);
		int packages = ((int)file.length() / 65520)+1;
		String[] ms = new String[packages+1];
		ms[0] = new PeerMessage(PeerMessageOps.OP_FILESIZE,packages).toEncodedString();
		if (packages >= 2) {
		for (int i = 0; i < packages-1; i++) {
			ms[i+1] = new PeerMessage(PeerMessageOps.OP_FILECODE,file.substring(0,65520)).toEncodedString();
			file = file.substring(65520);
		}
		ms[packages] = new PeerMessage(PeerMessageOps.OP_FILECODE,file).toEncodedString();
		}
		else {
			ms[1] = new PeerMessage(PeerMessageOps.OP_FILECODE,file).toEncodedString();
		}
		return ms;
	}

	public static String FilesListForClient() {
		int tam = 0;
		String cad = new String();
		FileInfo[] files = NanoFiles.db.getFiles();
		for (FileInfo file : files) {
			String s = file.toEncodedString();
			if (tam < (65499 - s.length() -1)) {
				cad = cad.concat(s);
				cad = cad.concat("\n");
				tam += s.length() +1;
			}
		}
		PeerMessage m = new PeerMessage(PeerMessageOps.OP_QUERY_RESPONSE, cad);
		return m.toEncodedString();
	}

	public static String endConfirmationForClient() {
		PeerMessage m = new PeerMessage(PeerMessageOps.OP_CLOSE);
		return m.toEncodedString();
	}
	
}
