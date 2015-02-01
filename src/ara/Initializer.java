package ara;

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
public class Initializer implements Control {
	/** Le pid de la couche protocolaire applicative */
	private int appLayerPid;

	/**
	 * Constructeur initialisant un nouveau module
	 * 
	 * @param prefix
	 * 		la chaîne préfixe permettant d'accéder à la 
	 * 		configuration du module
	 */
	public Initializer (String prefix) {
		appLayerPid = Configuration.getPid(prefix + ".appLayerPid");
	}

	@Override
	public boolean execute() {
		int nodeNb;
		EDProtocolImpl current;
		Node dest;

		nodeNb = Network.size();
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
			current = (EDProtocolImpl) dest.getProtocol(appLayerPid);
			current.init(i);
		}

		System.out.println("Init completed");

		return false;
	}

}
