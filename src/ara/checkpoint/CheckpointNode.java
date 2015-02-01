package ara.checkpoint;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import ara.Message;
import ara.app.AppMessageTypes;
import ara.app.ApplicationLayer;

/**
 * Couche protocolaire gérant la sauvegarde et la restauration.
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public class CheckpointNode extends ApplicationLayer {
	
	/* Configuration */
	
	/** Le délai min entre deux sauvegardes */
	private int minBackupDelay;
	
	/** Le délai max entre deux sauvegardes */
	private int maxBackupDelay;
	
	/** La durée maximale d'un rollback */
	private long maxRollbackDuration;
	
	/* Attributs applicatifs du noeud */
	
	/** Le compteur de messages reçus de chaque noeud */
	private long[] receiveTab;
	
	/** Le compteur de messages envoyés à chaque noeud */
	private long[] sendTab;
	
	/** Les points de sauvegarde */
	private List<Checkpoint> backups;
	
	/** La date du dernier rollback */
	private int lastRollback;
	
	/** La date de la prochaine sauvegarde */
	private int nextSave;
	
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
		super(prefix);
		receiveTab = new long[Network.size()];
		sendTab = new long[Network.size()];
		backups = new LinkedList<Checkpoint>();
		lastRollback = -1;
		nextSave = 0;

		for (int i = 0; i < Network.size(); i++) {
			receiveTab[i] = 0;
			sendTab[i] = 0;
		}
		
		minBackupDelay = Configuration.getInt(prefix + ".minBackupDelay");
		maxBackupDelay = Configuration.getInt(prefix + ".maxBackupDelay");
		maxRollbackDuration = Configuration.getInt(prefix + ".rollbacktimeout");
	}
	
	/* ****************************************************************
	 *                                                                *
	 *                 Traitement des self-messages                   *
	 *                                                                *
	 ******************************************************************/

	@Override
	protected void nextStep () {		
		/* Détecte la fin d'un rollback et relance l'application */
		if (lastRollback != -1 
				&& CommonState.getIntTime() >= lastRollback + maxRollbackDuration) {
			applicativeBlocked = false;
			lastRollback = -1;
			
			/* LOG */
			log("Relaunch application");
		}
		
		super.nextStep();
	}
	
	/* ****************************************************************
	 *                                                                *
	 *                   Gestion des checkpoints                      *
	 *                                                                *
	 ******************************************************************/

	/**
	 * Programme la prochaine sauvegarde
	 */
	private void setNextSave() {
		int delay = CommonState.r.nextInt(maxBackupDelay - minBackupDelay + 1) + minBackupDelay;
		nextSave = CommonState.getIntTime() + delay;
		schedule(CheckpointMessageTypes.CHKPT_BCKP, delay);	
	}
	
	/**
	 * Crée un point de restauration à partir de l'état courant
	 * 
	 * @see Checkpoint
	 * @see #restore()
	 */
	public void save () {
		Checkpoint chkpt;
		long[] rTab = Arrays.copyOf(receiveTab, Network.size());
		long[] sTab = Arrays.copyOf(sendTab, Network.size());
		
		chkpt = new Checkpoint(state, rTab, sTab);
		backups.add(chkpt);

		/* LOG */
		log("State saved: " + state);

		/* Programmation de la prochaine sauvegarde */
		setNextSave();
	}
	
	@Override
	protected void restart() {
		super.restart();
		if (CommonState.getIntTime() >= nextSave) {
			setNextSave();
		}
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
	protected void rollback () {
		/* LOG */
		log("Rollback!");
		
		/* On bloque l'envoi de message applicatifs jusqu'à la fin du rollback */
		applicativeBlocked = true;
		lastRollback = CommonState.getIntTime();
				
		/* Broadcast */
		for(int i = 0; i < Network.size(); i++) {
			if (nodeId != i) {
				Message rollMsg = new Message(nodeId, CheckpointMessageTypes.CHKPT_RLBK, sendTab[i]);
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
	
	@Override
	protected void receive(Message event) {
		super.receive(event);
		switch (event.getType()) {
		/* Message indiquant à ce noeud d'effectuer un point de sauvegarde */
		case CheckpointMessageTypes.CHKPT_BCKP:
			save();
			break;
			
		/* Message applicatif */
		case AppMessageTypes.APP_MSG:
			this.receiveTab[event.getSource()]++;
			break;
			
		/* Message indiquant le début d'un rollback */
		case CheckpointMessageTypes.CHKPT_RLBK:
			receiveRollback(event.getSource(), event.getContent());
		break;
		
		/* Message indiquant que ce noeud doit simuler un crash et se restaurer */
		case CheckpointMessageTypes.CHKPT_FAIL:
			/* LOG */
			log("Failing...");
			
			restore();
			rollback();
			break;
		}
	}
	
	@Override
	public void send(Message chkptMsg, int destId) {
		sendTab[destId]++;
		super.send(chkptMsg, destId);
	}
	
	/* ****************************************************************
	 *                                                                *
	 *                             Misc                               *
	 *                                                                *
	 ******************************************************************/
	
	@Override
	public void init(int nodeId) {
		super.init(nodeId);
		
		/* Sauvegarde initiale */
		save();
	}
	
	@Override
	public Object clone () {
		CheckpointNode dolly = new CheckpointNode(prefix);
		return dolly;
	}
}
