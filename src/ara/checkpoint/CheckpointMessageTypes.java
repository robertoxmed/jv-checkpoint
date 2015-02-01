package ara.checkpoint;

/**
 * Les types de messages utilisés par le gestionnaire de sauvegardes
 * 
 * @author Roberto Medina
 * @author Denis Jeanneau
 */
public abstract class CheckpointMessageTypes {	
	/** Type de message: rappel de sauvegarde */
	public final static int CHKPT_BCKP = 2;
	
	/** Type de message: rollback */
	public final static int CHKPT_RLBK = 3;
	
	/** Type de message: crash */
	public final static int CHKPT_FAIL = 4;
}
