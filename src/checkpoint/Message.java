package checkpoint;

/**
 * Un message envoyé d'un noeud à un autre
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public class Message {
	/** Type de message: avancement de l'état */
	public final static int CHKPT_SELF = 0;
	
	/** Type de message: message applicatif */
	public final static int CHKPT_APP = 1;
	
	/** Type de message: rappel de sauvegarde */
	public final static int CHKPT_BCKP = 2;
	
	/** Type de message: rollback */
	public final static int CHKPT_RLBK = 3;
	
	/** Type de message: crash */
	public final static int CHKPT_FAIL = 4;
	
	/** Le type du message */
	private int type;
	
	/** La valeur contenue dans le message */
	private long content;
	
	/** L'émetteur du message */
	private int source;
	
	/**
	 * Constructeur initialisant un nouveau message. Types de messages possibles:<br />
	 * {@link #CHKPT_SELF}<br />
	 * {@link #CHKPT_APP}<br />
	 * {@link #CHKPT_BCKP}<br />
	 * {@link #CHKPT_RLBK}<br />
	 * {@link #CHKPT_FAIL}
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
	 * Retourne le type du message. Valeurs possibles:<br />
	 * {@link #CHKPT_SELF}<br />
	 * {@link #CHKPT_APP}<br />
	 * {@link #CHKPT_BCKP}<br />
	 * {@link #CHKPT_RLBK}<br />
	 * {@link #CHKPT_FAIL}
	 * 
	 * @return
	 * 		le type du message
	 */
	public int getType() {
		return type;
	}

	public int getSource() {
		return source;
	}
}
