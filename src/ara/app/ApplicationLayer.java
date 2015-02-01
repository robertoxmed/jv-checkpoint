package ara.app;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import ara.EDProtocolImpl;
import ara.Message;

/**
 * Couche protocolaire gérant la sauvegarde et la restauration.
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public class ApplicationLayer extends EDProtocolImpl {
	
	/* Configuration */
	
	/** le délai minimal entre deux avancements d'un noeud */
	private int minStep;
	
	/** le délai maximal entre deux avancements d'un noeud */
	private int maxStep;
	
	/** La probabilité qu'un message soit envoyé lors de l'avancement de l'appli */
	private double appProba;
	
	/** La probabilité d'un broadcast lors de l'avancement de l'appli */
	private double appBroadcastProba;
	
	/* Attributs applicatifs du noeud */
	
	/** L'état applicatif du noeud */
	protected long state;
	
	/** Booléen interdisant l'envoi de messages applicatifs durant un rollback */
	protected boolean applicativeBlocked;
	
	/** La date du prochain avancement */
	private int nextStep;
	
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
	public ApplicationLayer (String prefix) {
		super(prefix);
		state = 0;
		nextStep = 0;
		applicativeBlocked = false;
		
		minStep = Configuration.getInt(prefix + ".minStep");
		maxStep = Configuration.getInt(prefix + ".maxStep");
		appProba = Configuration.getDouble(prefix + ".appMessageProba");
		appBroadcastProba = Configuration.getDouble(prefix + ".broadcastProba");
	}
	
	/* ****************************************************************
	 *                                                                *
	 *                 Traitement des self-messages                   *
	 *                                                                *
	 ******************************************************************/

	/**
	 * Programme le prochain avancement de l'application
	 */
	private void setNextStep() {
		int delay = CommonState.r.nextInt(maxStep - minStep + 1) + minStep;
		nextStep = CommonState.getIntTime() + delay;
		schedule(AppMessageTypes.APP_SELF, delay);
	}
	
	/**
	 * Evolution de l'état interne de l'application et envoi des messages applicatifs
	 */
	protected void nextStep () {
		double rand;
		
		/* Avancement de l'état du noeud */
		state++;
		
		/* LOG */
		log("State : " + state);

		/* Envoi (éventuel) d'un message applicatif */
		rand = CommonState.r.nextDouble();
		if (rand <= appProba && !applicativeBlocked) {
			Message appMsg = new Message(nodeId, AppMessageTypes.APP_MSG, 0);
			int destId = CommonState.r.nextInt(Network.size());
			send(appMsg, destId);	
			
			/* LOG */
			log("Send to : " + destId);
		}

		/* Broadcast (éventuel) */
		rand = CommonState.r.nextDouble();
		if (rand <= appBroadcastProba && !applicativeBlocked) {
			for (int i = 0; i < Network.size(); i++) {
				Message chkptMsg = new Message(nodeId, AppMessageTypes.APP_MSG, 0);
				send(chkptMsg, i);
				
				/* LOG */
				log("Send to : " + i);
			}
		}

		/* Programmation du prochain avancement */
		setNextStep();
	}
	
	/**
	 * Relance le noeud après un crash
	 */
	protected void restart() {
		if (CommonState.getIntTime() >= nextStep) {
			setNextStep();
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
		/* Avancement de l'état applicatif interne du noeud et envoi des messages applicatifs */
		case AppMessageTypes.APP_SELF:
			nextStep();
			break;
			
		/* Message applicatif */
		case AppMessageTypes.APP_MSG:	
			/* LOG */
			log("Received " + event.getContent()
					+ " from : " + event.getSource());
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

		/* Programmation du premier avancement */
		setNextStep();
	}
	
	@Override
	public Object clone () {
		ApplicationLayer dolly = new ApplicationLayer(prefix);
		return dolly;
	}
}
