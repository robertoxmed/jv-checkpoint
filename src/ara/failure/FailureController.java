package ara.failure;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;
import ara.Message;

/**
 * Un contrôleur chargé de provoquer des crashs.
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public class FailureController implements Control {
	/** Le pid de la couche protocolaire applicative */
	private int appLayerPid;
	
	/** La probabilité qu'un noeud tombe à chaque exécution */
	private double crashProba;
	
	/** Le délai entre deux exécution du contrôleur */
	private int step;
	
	/**
	 * Constructeur initialisant un nouveau module
	 * 
	 * @param prefix
	 * 		la chaîne préfixe permettant d'accéder à la 
	 * 		configuration du module
	 */
	public FailureController (String prefix) {
		super();
		appLayerPid = Configuration.getPid(prefix + ".appLayerPid");
		crashProba = Configuration.getDouble(prefix + ".crashProba");
		step = Configuration.getInt(prefix + ".step");
	}
	
	@Override
	public boolean execute() {
		double rand = CommonState.r.nextDouble();
		if (rand < crashProba) {
			Node dest;
			Message failMsg;
			int delay = CommonState.r.nextInt(step);
			int victim = CommonState.r.nextInt(Network.size());
			dest = Network.get(victim);
			failMsg = new Message(Network.size(), HeartbeatMessageTypes.HBEAT_KILL, 0);
			EDSimulator.add(delay, failMsg, dest, appLayerPid);
		}
		return false;
	}

}
