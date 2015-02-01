package ara.checkpoint;

/**
 * Un point de sauvegarde tel qu'utilisé dans l'algorithme de Juang-Venkatesan
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public class Checkpoint {
	/** L'état applicatif du noeud */
	private long state;
	
	/** Les nombres de messages reçus, triés par émetteur */
	private long[] receiveTab;
	
	/** Les nombres de messages émis, triés par destinataires */
	private long[] sendTab;

	/**
	 * Constructeur spécifiant l'état du noeud à enregistrer
	 * 
	 * @param state
	 * 		l'état applicatif du noeud au moment de la sauvegarde
	 * @param receiveTab
	 * 		les messages reçus
	 * @param sendTab
	 * 		les messages émis
	 */
	public Checkpoint(long state, long[] receiveTab, long[] sendTab) {
		super();
		this.state = state;
		this.receiveTab = receiveTab;
		this.sendTab = sendTab;
	}
	
	/**
	 * @return
	 * 		l'état applicatif du noeud au moment de la sauvegarde
	 */
	public long getState() {
		return state;
	}

	/**
	 * @return
	 * 		les nombres de messages reçus avant la sauvegarde, triés par émetteur
	 */
	public long[] getReceiveTab() {
		return receiveTab;
	}

	/**
	 * @return
	 * 		les nombres de messages émis avant la sauvegarde, triés par destinataires
	 */
	public long[] getSendTab() {
		return sendTab;
	}
}
