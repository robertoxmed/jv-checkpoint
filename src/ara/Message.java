package ara;

/**
 * Un message envoyé d'un noeud à un autre
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public class Message {
	/** Le type du message */
	private int type;
	
	/** La valeur contenue dans le message */
	private long content;
	
	/** L'émetteur du message */
	private int source;
	
	/**
	 * Constructeur initialisant un nouveau message.
	 * 
	 * @param source
	 * 		l'émetteur du message
	 * @param type
	 * 		le type de message
	 * @param content
	 * 		le contenu du message
	 */
	public Message (int source, int type, long content) {
		this.type = type;
		this.content = content;
		this.source = source;
	}

	/**
	 * @return
	 * 		la valeur contenue dans le message
	 */
	public long getContent() {
		return content;
	}

	/**
	 * Retourne le type du message.
	 * 
	 * @return
	 * 		le type du message
	 */
	public int getType() {
		return type;
	}

	/**
	 * @return
	 * 		l'id du noeud émetteur de ce message
	 */
	public int getSource() {
		return source;
	}
}
