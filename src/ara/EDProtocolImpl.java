package ara;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

/**
 * Squelette de couche protocolaire
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public abstract class EDProtocolImpl implements EDProtocol {
	
	/* Configuration */
	
	/** L'id de la couche de transport utilisée */
	private int transportPid;

	/** L'id de cette couche protocolaire */
	private int myPid;
	
	/* Gestion des noeuds PeerSim */

	/** La couche de transport */
	private TransportLayer transport;
	
	/** La chaîne préfixe permettant d'accéder à la configuration du noeud */
	protected String prefix;
	
	/** L'id du noeud */
	protected int nodeId;
	
	/* ****************************************************************
	 *                                                                *
	 *                         Constructeur                           *
	 *                                                                *
	 ******************************************************************/

	/**
	 * Constructeur initialisant une nouvelle couche protocolaire
	 * 
	 * @param prefix
	 * 		la chaîne préfixe permettant d'accéder à la 
	 * 		configuration de la couche protocolaire
	 */
	protected EDProtocolImpl(String prefix) {
		super();
		this.prefix = prefix;

		transportPid = Configuration.getPid(prefix + ".transport");
		myPid = Configuration.getPid(prefix + ".myself");
	}

	/* ****************************************************************
	 *                                                                *
	 *                      Envoi / réception                         *
	 *                                                                *
	 ******************************************************************/
	
	/**
	 * Méthode de traitement des messages reçus
	 * 
	 * @param event
	 * 		le message reçu
	 */
	protected abstract void receive(Message event);
	
	@Override
	public void processEvent(Node node, int pid, Object event) {
		receive((Message)event);
	}
	
	/**
	 * Envoie un message à un noeud
	 * 
	 * @param msg
	 * 		le message à envoyer
	 * @param destId
	 * 		l'identifiant du noeud destinataire
	 */
	public void send(Message msg, int destId) {
		transport.send(getNode(), Network.get(destId), msg, myPid);
	}
	
	/**
	 * Programme un évènement sur ce noeud
	 * 
	 * @param event
	 * 		l'évènement à programmer
	 * @param delay
	 * 		le temps restant avant l'évènement
	 * @see Message
	 */
	protected void schedule(int event, int delay) {
		schedule(event, delay, 0);
	}
	
	/**
	 * Programme un évènement sur ce noeud
	 * 
	 * @param event
	 * 		l'évènement à programmer
	 * @param delay
	 * 		le temps restant avant l'évènement
	 * @param value
	 * 		la valuer associée à l'évènement
	 * @see Message
	 */
	protected void schedule(int event, int delay, long value) {
		Message selfMsg = new Message(nodeId, event, value);
		EDSimulator.add(delay, selfMsg, getNode(), myPid);
	}
	
	/* ****************************************************************
	 *                                                                *
	 *                             Misc                               *
	 *                                                                *
	 ******************************************************************/

	/**
	 * Fixe l'identifiant du noeud courant et en déduit la couche transport à utiliser
	 * 
	 * @param nodeId
	 * 		l'identifiant du noeud courant		
	 */
	public void init(int nodeId) {
		this.nodeId = nodeId;
		transport = (TransportLayer) getNode().getProtocol(transportPid);
	}

	/**
	 * Récupère le noeud auquel appartient cette couche protocolaire
	 * 
	 * @return
	 * 		le noeud courant
	 */
	protected Node getNode() {
		return Network.get(nodeId);
	}
	
	/**
	 * Affiche un message sur la sortie standard
	 * 
	 * @param str	
	 * 		la chaîne à afficher
	 */
	protected void log(String str) {
		System.out.println("[" + CommonState.getTime() + "] " + this + "> " + str);		
	}
	
	@Override
	public Object clone () {
		return this;
	}

	@Override
	public String toString() {
		return "Node "+ nodeId;
	}

}
