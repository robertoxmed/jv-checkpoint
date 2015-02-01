package ara.failure;

import peersim.config.Configuration;
import peersim.core.Fallible;
import peersim.core.Network;
import ara.EDProtocolImpl;
import ara.Message;

/**
 * Couche protocolaire gérant la sauvegarde et la restauration.
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public class CheckpointNode extends ara.checkpoint.CheckpointNode implements FailureHandler {
	
	/* Configuration */
	
	/** Le pid du détecteur à utiliser */
	private int detectorPid;
	
	/** Le nombre de wakeups nécessaires pour relancer un noeud */
	private int wakeupCount;
	
	/* Attributs applicatifs du noeud */
	
	/** Le détecteur de défaillances */
	private FailureDetector detector;
	
	/** Le nombre de wakeups reçus durant le crash en cours */
	private int wakeups;
	
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
	public CheckpointNode(String prefix) {
		super(prefix);
		wakeups = 0;
		
		detectorPid = Configuration.getPid(prefix + ".detector");
		wakeupCount = Configuration.getInt(prefix + ".wakeupCount");
	}
	
	/* ****************************************************************
	 *                                                                *
	 *                      Gestion des fautes                        *
	 *                                                                *
	 ******************************************************************/
	
	/**
	 * Crashe ce noeud
	 */
	private void kill() {
		wakeups = 0;
		getNode().setFailState(Fallible.DOWN);
		
		/* LOG */
		log("Crash!!!!!!!!!!");
	}
	
	/**
	 * Tient compte de la détection de la défaillance de ce noeud par un autre noeud
	 */
	private void wakeup() {
		wakeups++;
		if (wakeups == wakeupCount) {
			/* LOG */
			log("Waking up!!!!");
			
			rollback();
			restart();
		}
	}

	@Override
	public void onDown(int nodeId) {
		Message wkupMsg = new Message(this.nodeId, 
				HeartbeatMessageTypes.HBEAT_WKUP, 0);
		Network.get(nodeId).setFailState(Fallible.OK);
		send(wkupMsg, nodeId);
	}

	@Override
	public void onUp(int nodeId) {
		// Rien à faire
	}
	
	@Override
	protected void restart() {
		super.restart();
		detector.restart();
	}
	
	/* ****************************************************************
	 *                                                                *
	 *                      Envoi / réception                         *
	 *                                                                *
	 ******************************************************************/
	
	@Override
	protected void receive(Message event) {
		super.receive(event);
		
		switch (event.getType()) {
		/* Réception d'un kill du contrôleur */
		case HeartbeatMessageTypes.HBEAT_KILL:
			kill();
			break;
			
		case HeartbeatMessageTypes.HBEAT_WKUP:
			wakeup();
			break;
		}
	}
	
	/* ****************************************************************
	 *                                                                *
	 *                             Misc                               *
	 *                                                                *
	 ******************************************************************/
	
	@Override
	public void init(int nodeId) {
		super.init(nodeId);
		
		/* Démarrage du détecteur */
		detector = (FailureDetector) getNode().getProtocol(detectorPid);
		detector.addFailureHandler(this);
		((EDProtocolImpl) detector).init(nodeId);
	}
	
	@Override
	public Object clone () {
		CheckpointNode dolly = new CheckpointNode(prefix);
		return dolly;
	}

}
