package ara.failure;


/**
 * Un détecteur de défaillance
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public interface FailureDetector {
	/**
	 * Indique si le noeud d'indice donné est suspecté ou non
	 * 
	 * @param nodeId
	 * 		l'indice d'un noeud
	 * @return
	 * 		true si le noeud désigné par nodeId est correct, false si il est suspecté
	 */
	public boolean isUp(int nodeId);
	
	/**
	 * Ajoute un écouteur pour gérer les suspicions de noeuds
	 * 
	 * @param handler
	 * 		l'écouteur à ajouter
	 */
	public void addFailureHandler(FailureHandler handler);
	
	/**
	 * Redémarre le détecteur après un crash
	 */
	public void restart();
}
