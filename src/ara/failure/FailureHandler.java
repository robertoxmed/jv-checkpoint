package ara.failure;

/**
 * Un gestionnaire de fautes franches
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public interface FailureHandler {
	/**
	 * Prévient de la nouvelle suspicion d'un noeud
	 * 
	 * @param nodeId
	 * 		le noeud suspecté
	 */
	public void onDown(int nodeId);
	
	/**
	 * Prévient du redémarrage d'un noeud auparavant suspecté
	 * 
	 * @param nodeId
	 * 		le noeud redémarré
	 */
	public void onUp(int nodeId);
}
