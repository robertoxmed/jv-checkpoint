package checkpoint;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

/**
 * Un contrôleur chargé de crasher un noeud pour provoquer un rollback.
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public class FailController implements Control{
	/** Le pid de la couche protocolaire applicative */
	private int checkpointPid;

	/**
	 * Constructeur initialisant un nouveau module
	 * 
	 * @param prefix
	 * 		la chaîne préfixe permettant d'accéder à la 
	 * 		configuration du module
	 */
	public FailController (String prefix) {
		checkpointPid = Configuration.getPid(prefix + ".checkpointProtocolPid");
	}

	@Override
	public boolean execute() {
		/* 
		 * Test pour éviter un crash dès le début du programme: on attend au moins une période d'exécution
		 */
		if (CommonState.getIntTime() > 0) {
			Node dest;
			Message failMsg;
			int victim = CommonState.r.nextInt(Network.size());
			dest = Network.get(victim);
			failMsg = new Message(Network.size(), Message.CHKPT_FAIL, 0);
			EDSimulator.add(0, failMsg, dest, checkpointPid);
		}
		return false;
	}

}
