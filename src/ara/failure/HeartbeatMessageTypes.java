package ara.failure;

/**
 * Les types de messages utilisé par le détecteur de défaillances
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public abstract class HeartbeatMessageTypes {
	/** Type de message: heartbeat */
	public final static int HBEAT = 0;
	
	/** Type de message: vérification du délai heartbeat */
	public final static int HBEAT_CHECK = 1;
	
	/** Type de message: envoi d'un nouveau heartbeat */
	public final static int HBEAT_SELF = 2;
	
	/** Type de message: crash */
	public final static int HBEAT_KILL = 98;
	
	/** Type de message: réveil */
	public final static int HBEAT_WKUP = 99;
}
