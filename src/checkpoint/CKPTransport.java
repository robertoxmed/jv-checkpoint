package checkpoint;

import peersim.config.*;
import peersim.core.*;
import peersim.edsim.*;

/**
 * La couche de transport utilisée par {@link CheckpointNode}.
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 * 
 * @see	CheckpointNode
 */
public class CKPTransport implements Protocol{
	/** La latence minimale entre deux noeuds */
    private long min = 0;
    
    /** La latence maximale entre deux noeuds */
    private long max = 0;
    
    /**
	 * Constructeur initialisant une nouvelle couche protocolaire
	 * 
	 * @param prefix
	 * 		la chaîne préfixe permettant d'accéder à la 
	 * 		configuration de la couche protocolaire
	 */
    public CKPTransport (String prefix) {
    	min = Configuration.getInt(prefix + ".mindelay");
		max = Configuration.getInt(prefix + ".maxdelay");
		if (max < min) {
			System.out.println("The maximum latency cannot be smaller than the minimum latency");
			System.exit(1);
		}
    }
    
    /**
     * Envoi de message
     * 
     * @param src
     * 		le noeud source
     * @param dest
     * 		le noeud destinataire
     * @param msg
     * 		le message à transmettre
     * @param pid
     * 		le pid de la couche protocolaire qui réceptionnera le message
     */
    public void send(Node src, Node dest, Object msg, int pid) {
    	long delay = getLatency(src, dest);
    	EDSimulator.add(delay, msg, dest, pid);
    }
    
    /**
     * Récupère la latence entre deux noeuds. Le résultat est indéterministe.
     * 
     * @param src
     * 		le noeud source
     * @param dest
     * 		le noeud destinataire
     * @return
     * 		le temps de latence entre src et dest
     */
    public long getLatency(Node src, Node dest) {
    	return ((max - min + 1) == 1? min: min + CommonState.r.nextLong(max - min + 1));
    }
    
   @Override
   public Object clone() {
   	return this;
   }
}
