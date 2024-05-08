package es.um.redes.nanoFiles.message;

public class PeerMessageOps {

	/*
	 * TODO: Añadir aquí todas las constantes que definen los diferentes tipos de
	 * mensajes del protocolo entre pares.
	 */
	public static final String OP_DOWNLOAD = "download";
	public static final String OP_DOWNLOAD_FAIL = "file_not_found";
	public static final String OP_FILESIZE = "filesize";
	public static final String OP_FILECODE = "code";
	public static final String OP_QUERY = "queryfiles";
	public static final String OP_QUERY_RESPONSE = "queryresponse";
	public static final String OP_CLOSE = "close";

}
