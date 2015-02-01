package ara.failure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import ara.EDProtocolImpl;
import ara.Message;

/**
 * Un détecteur de défaillances par Heartbeat
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public class HeartbeatDetector extends EDProtocolImpl implements FailureDetector {
	
	/* Configuration */
	
	/** Le délai entre deux détections */
	private int heartbeatCheckDelay;
	
	/** Le délai entre deux envois de heartbeat */
	private int heartbeatSendDelay;
	
	/* Attributs applicatifs du noeud */
	
	/** Les gestionnaires de fautes enregistrés */
	private List<FailureHandler> handlers;

	/** L'ensemble des noeuds suspectés */
	private Set<Integer> suspects;

	/** La date de dernière réception d'un message pour chaque noeud */
	private int[] lastBeat;
	
	/** La date du prochain beat */
	private int nextBeat;

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
	public HeartbeatDetector(String prefix) {
		super(prefix);
		nextBeat = 0;

		handlers = new ArrayList<>();
		suspects = new HashSet<>();
		lastBeat = new int[Network.size()];

		for (int i = 0; i < Network.size(); i++) {
			lastBeat[i] = 0;
		}
		
		heartbeatCheckDelay = Configuration.getInt(prefix + ".heartbeatCheckDelay");
		heartbeatSendDelay = Configuration.getInt(prefix + ".heartbeatSendDelay");
	}
	
	/* ****************************************************************
	 *                                                                *
	 *                     Détection de fautes                        *
	 *                                                                *
	 ******************************************************************/

	@Override
	public boolean isUp(int nodeId) {
		return !suspects.contains(nodeId);
	}

	@Override
	public void addFailureHandler(FailureHandler handler) {
		handlers.add(handler);
	}

	/**
	 * Tient compte de la nouvelle suspicion d'un noeud
	 * 
	 * @param nodeId
	 * 		l'id du noeud suspecté
	 */
	private void down(int nodeId) {
		suspects.add(nodeId);
		
		/* LOG */
		log("Suspecting " + nodeId);
		
		for (FailureHandler handler: handlers) {
			handler.onDown(nodeId);
		}
	}
	
	/**
	 * Programme le prochain battement
	 */
	private void setNextBeat() {
		nextBeat = CommonState.getIntTime() + heartbeatSendDelay;
		schedule(HeartbeatMessageTypes.HBEAT_SELF, heartbeatSendDelay);
	}
	
	/**
	 * Déclenche un battement de coeur de ce noeud
	 */
	private void beat() {
		Message hbeat = new Message(nodeId, HeartbeatMessageTypes.HBEAT, 0);
		for (int i = 0; i < Network.size(); i++) {
			if (i != nodeId) {
				send(hbeat, i);
			}
		}
		setNextBeat();
	}

	/**
	 * Tient compte de la réception d'un battement de coeur
	 * 
	 * @param nodeId
	 * 		l'id du noeud émetteur
	 */
	private void receiveBeat(int nodeId) {
		lastBeat[nodeId] = CommonState.getIntTime();
		if (suspects.contains(nodeId)) {
			suspects.remove(nodeId);
			
			/* LOG */
			log(nodeId + " is not suspected anymore");
			
			for (FailureHandler handler: handlers) {
				handler.onUp(nodeId);
			}
		}
		
		/* Programmation d'une vérification à l'issue du délai heartbeat */
		schedule(HeartbeatMessageTypes.HBEAT_CHECK, heartbeatCheckDelay, nodeId);
	}
	
	@Override
	public void restart() {
		if (CommonState.getIntTime() >= nextBeat) {
			setNextBeat();
		}
		for (int i = 0; i < Network.size(); i++) {
			if ((i != nodeId) && (lastBeat[i] 
					+ heartbeatCheckDelay <= CommonState.getIntTime())) {
				down(i);
			}
		}
	}

	/* ****************************************************************
	 *                                                                *
	 *                      Envoi / réception                         *
	 *                                                                *
	 ******************************************************************/


	@Override
	protected void receive(Message event) {
		switch (event.getType()) {
		/* "Je suis vivant" */
		case HeartbeatMessageTypes.HBEAT:
			receiveBeat(event.getSource());
			break;
			
		/* Détection de faute */
		case HeartbeatMessageTypes.HBEAT_CHECK:
			if (lastBeat[(int) event.getContent()] 
					+ heartbeatCheckDelay <= CommonState.getIntTime()) {
				down((int) event.getContent());
			}
			break;
			
		/* Envoi d'un heartbeat */
		case HeartbeatMessageTypes.HBEAT_SELF:
			beat();
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
		
		/* Battement de coeur initial */
		beat();
	}

	@Override
	public Object clone () {
		HeartbeatDetector dolly = new HeartbeatDetector(prefix);
		return dolly;
	}
}
