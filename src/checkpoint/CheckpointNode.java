package checkpoint;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDProtocol;
import peersim.edsim.EDSimulator;

/**
 * Couche protocolaire gérant la sauvegarde et la restauration.
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public class CheckpointNode implements EDProtocol {
	
	/* Configuration */
	
	/** L'id de la couche de transport utilisée */
	private int transportPid;

	/** L'id de cette couche protocolaire */
	private int myPid;
	
	/** le délai minimal entre deux avancements d'un noeud */
	private int minStep;
	
	/** le délai maximal entre deux avancements d'un noeud */
	private int maxStep;
	
	/** La probabilité qu'un message soit envoyé lors de l'avancement de l'appli */
	private double appProba;
	
	/** La probabilité d'un broadcast lors de l'avancement de l'appli */
	private double appBroadcastProba;
	
	/** Le délai min entre deux sauvegardes */
	private int minBackupDelay;
	
	/** Le délai max entre deux sauvegardes */
	private int maxBackupDelay;
	
	/** La durée maximale d'un rollback */
	private long maxRollbackDuration;
	
	/* Gestion des noeuds PeerSim */

	/** La couche de transport */
	private CKPTransport transport;
	
	/** La chaîne préfixe permettant d'accéder à la configuration du noeud */
	private String prefix;
	
	/** L'id du noeud */
	private int nodeId;
	
	/* Attributs applicatifs du noeud */
	
	/** L'état applicatif du noeud */
	private long state;
	
	/** Le compteur de messages reçus de chaque noeud */
	private long[] receiveTab;
	
	/** Le compteur de messages envoyés à chaque noeud */
	private long[] sendTab;
	
	/** Les points de sauvegarde */
	private List<Checkpoint> backups;
	
	/** Booléen interdisant l'envoi de messages applicatifs durant un rollback */
	private boolean applicativeBlocked;
	
	/** La date du dernier rollback */
	private int lastRollback;
	
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
	public CheckpointNode (String prefix) {
		super();
		this.prefix = prefix;
		receiveTab = new long[Network.size()];
		sendTab = new long[Network.size()];
		state = 0;
		backups = new LinkedList<Checkpoint>();
		transport = null;
		applicativeBlocked = false;
		lastRollback = -1;

		for (int i = 0; i < Network.size(); i++) {
			receiveTab[i] = 0;
			sendTab[i] = 0;
		}

		transportPid = Configuration.getPid(prefix + ".transport");
		myPid = Configuration.getPid(prefix + ".myself");
		minStep = Configuration.getInt(prefix + ".minStep");
		maxStep = Configuration.getInt(prefix + ".maxStep");
		appProba = Configuration.getDouble(prefix + ".appMessageProba");
		appBroadcastProba = Configuration.getDouble(prefix + ".broadcastProba");
		minBackupDelay = Configuration.getInt(prefix + ".minBackupDelay");
		maxBackupDelay = Configuration.getInt(prefix + ".maxBackupDelay");
		maxRollbackDuration = Configuration.getInt(prefix + ".rollbacktimeout");
	}
	
	/* ****************************************************************
	 *                                                                *
	 *                 Traitement des self-messages                   *
	 *                                                                *
	 ******************************************************************/

	/**
	 * Evolution de l'état interne de l'application et envoi des messages applicatifs
	 */
	private void nextStep () {
		double rand;
		int delay;
		
		/* Avancement de l'état du noeud */
		state++;
		
		/* LOG */
		log("State : " + state);
		
		/* Détecte la fin d'un rollback et relance l'application */
		if (lastRollback != -1 
				&& CommonState.getIntTime() >= lastRollback + maxRollbackDuration) {
			applicativeBlocked = false;
			lastRollback = -1;
			
			/* LOG */
			log("Relaunch application");
			
		}

		/* Envoi (éventuel) d'un message applicatif */
		rand = CommonState.r.nextDouble();
		if (rand <= appProba && !applicativeBlocked) {
			Message appMsg = new Message(nodeId, Message.CHKPT_APP, 0);
			int destId = CommonState.r.nextInt(Network.size());
			send(appMsg, destId);	
			
			/* LOG */
			log("Send to : " + destId
					+ " canal (" + sendTab[destId]+ ")" );
		}

		/* Broadcast (éventuel) */
		rand = CommonState.r.nextDouble();
		if (rand <= appBroadcastProba && !applicativeBlocked) {
			for (int i = 0; i < Network.size(); i++) {
				Message chkptMsg = new Message(nodeId, Message.CHKPT_APP, 0);
				send(chkptMsg, i);
				
				/* LOG */
				log("Send to : " + i
						+ " canal (" + sendTab[i]+ ")" );
			}
		}

		/* Programmation du prochain avancement */
		delay = CommonState.r.nextInt(maxStep - minStep + 1) + minStep;
		schedule(Message.CHKPT_SELF, delay);
	}
	
	/* ****************************************************************
	 *                                                                *
	 *                   Gestion des checkpoints                      *
	 *                                                                *
	 ******************************************************************/

	/**
	 * Crée un point de restauration à partir de l'état courant
	 * 
	 * @see Checkpoint
	 * @see #restore()
	 */
	public void save () {
		int delay;
		Checkpoint chkpt;
		long[] rTab = Arrays.copyOf(receiveTab, Network.size());
		long[] sTab = Arrays.copyOf(sendTab, Network.size());
		
		chkpt = new Checkpoint(state, rTab, sTab);
		backups.add(chkpt);

		/* LOG */
		log("State saved: " + state);

		/* Programmation de la prochaine sauvegarde */
		delay = CommonState.r.nextInt(maxBackupDelay - minBackupDelay + 1) + minBackupDelay;
		schedule(Message.CHKPT_BCKP, delay);	
	}

	/**
	 * Restaure l'état courant à partir du dernier point de restauration
	 * 
	 * @see Checkpoint
	 * @see #save
	 */
	private void restore () {
		if (backups.size() > 0) {
			Checkpoint chkpt = backups.get(backups.size() - 1);
			backups.remove(backups.size() - 1);
			state = chkpt.getState();
			receiveTab = chkpt.getReceiveTab();
			sendTab = chkpt.getSendTab();
	
			/* LOG */
			log("State restored: " + state);
		}
	}

	/**
	 * Déclenche un rollback du système
	 * 
	 * @see #receiveRollback(int, long)
	 */
	private void rollback () {
		/* LOG */
		log("Rollback!");
		
		/* On bloque l'envoi de message applicatifs jusqu'à la fin du rollback */
		applicativeBlocked = true;
		lastRollback = CommonState.getIntTime();
				
		/* Broadcast */
		for(int i = 0; i < Network.size(); i++) {
			if (nodeId != i) {
				Message rollMsg = new Message(nodeId, Message.CHKPT_RLBK, sendTab[i]);
				send(rollMsg, i);
			}
		}
	}

	/**
	 * Traitement d'un message de rollback reçu: algorithme de Juang-Venkatesan
	 * 
	 * @param source
	 * 		l'émetteur du rollback
	 * @param content
	 * 		le nombre de messages que source se souvient avoir émis au noeud courant
	 * @see	#rollback()
	 */
	private void receiveRollback (int source, long content) {
		/* LOG */
		log("Received rollback message from " + source + " value " + content);
		
		/* On bloque l'envoi de message applicatifs jusqu'à la fin du rollback */
		applicativeBlocked = true;
		lastRollback = CommonState.getIntTime();
		
		/* On teste si il est nécessaire de restaurer le noeud courant */
		if (receiveTab[source] > content) {
			/* On revient en arrière autant de fois que nécessaire */
			while (receiveTab[source] > content) {
				restore();
			}
			
			/* LOG */
			log("Final restore state: " + state);
			
			/* On informe les autres de cette nouvelle restauration */
			rollback();
		}
		else {
			/* LOG */
			log("No restore necessary");
		}
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
	private void receive(Message event) {
		switch (event.getType()) {
		/* Avancement de l'état applicatif interne du noeud et envoi des messages applicatifs */
		case Message.CHKPT_SELF:
			nextStep();
			break;
			
		/* Message indiquant à ce noeud d'effectuer un point de sauvegarde */
		case Message.CHKPT_BCKP:
			save();
			break;
			
		/* Message applicatif */
		case Message.CHKPT_APP:
			this.receiveTab[event.getSource()]++;
				
			/* LOG */
			log("Received applicative " + event.getContent()
					+ " from : " + event.getSource()
					+ " canal (" + receiveTab[event.getSource()]+ ")" );
			break;
			
		/* Message indiquant le début d'un rollback */
		case Message.CHKPT_RLBK:
			receiveRollback(event.getSource(), event.getContent());
		break;
		
		/* Message indiquant que ce noeud doit simuler un crash et se restaurer */
		case Message.CHKPT_FAIL:
			/* LOG */
			log("Failing...");
			
			restore();
			rollback();
			break;
		}
	}
	
	@Override
	public void processEvent(Node node, int pid, Object event) {
		this.receive((Message)event);
	}
	
	/**
	 * Envoie un message à un noeud
	 * 
	 * @param chkptMsg
	 * 		le message à envoyer
	 * @param destId
	 * 		l'identifiant du noeud destinataire
	 */
	public void send(Message chkptMsg, int destId) {
		sendTab[destId]++;
		transport.send(getNode(), Network.get(destId), chkptMsg, myPid);
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
	private void schedule(int event, int delay) {
		Message selfMsg = new Message(nodeId, event, 0);
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
	 * 		l'identifiant du noeud courant	 * 		
	 */
	public void setTransportLayer(int nodeId) {
		this.nodeId = nodeId;
		transport = (CKPTransport) getNode().getProtocol(transportPid);
	}

	/**
	 * Récupère le noeud auquel appartient cette couche protocolaire
	 * 
	 * @return
	 * 		le noeud courant
	 */
	private Node getNode() {
		return Network.get(nodeId);
	}
	
	/**
	 * Affiche un message sur la sortie standard
	 * 
	 * @param str	
	 * 		la chaîne à afficher
	 */
	private void log(String str) {
		System.out.println("[" + CommonState.getTime() + "] " + this + "> " + str);		
	}
	
	@Override
	public Object clone () {
		CheckpointNode dolly = new CheckpointNode(prefix);
		return dolly;
	}

	@Override
	public String toString() {
		return "Node "+ nodeId;
	}
}
