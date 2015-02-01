package ara.app;

/**
 * Les types de messages utilisés par la couche applicative
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public abstract class AppMessageTypes {
	/** Type de message: avancement de l'état */
	public final static int APP_SELF = 0;
	
	/** Type de message: message applicatif */
	public final static int APP_MSG = 1;
}
