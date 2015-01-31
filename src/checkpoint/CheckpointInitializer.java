package checkpoint;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

/**
 * Le module chargé d'initialiser la simulation
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public class CheckpointInitializer implements Control {
	/** Le pid de la couche protocolaire applicative */
	private int checkpointPid;
	
	/**
	 * Constructeur initialisant un nouveau module
	 * 
	 * @param prefix
	 * 		la chaîne préfixe permettant d'accéder à la 
	 * 		configuration du module
	 */
	public CheckpointInitializer (String prefix) {
		checkpointPid = Configuration.getPid(prefix + ".checkpointProtocolPid");
	}
	
	@Override
	public boolean execute() {
		int nodeNb;
		CheckpointNode current;
		Node dest;
		Message chkptMsg, bckpMsg;
		
		nodeNb = Network.size();
		chkptMsg = new Message(0, Message.CHKPT_SELF, -32);
		bckpMsg = new Message(0, Message.CHKPT_BCKP, 99);
		if (nodeNb < 1) {
			System.err.println("Network size is not positive");
			System.exit(1);
		}
		
		/* 
		 * Pour chaque noeud, on lance l'avancement de l'état applicatif 
		 * et les sauvegardes régulières
		 */
		for (int i = 0; i < nodeNb; i++) {
			dest = Network.get(i);
			current = (CheckpointNode) dest.getProtocol(checkpointPid);
			current.setTransportLayer(i);
			current.send(chkptMsg, i);
			current.save();
		}
		
		System.out.println("Init completed");
		
		return false;
	}

}
