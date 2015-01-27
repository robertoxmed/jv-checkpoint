package checkpoint;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


import peersim.edsim.*;
import peersim.core.*;
import peersim.config.*;

public class CheckpointNode implements EDProtocol {
	
	private int transportPid;
	
	private CKPTransport transport;
	
	private int mypid;
	private long state;
	private long[] receiveTab;
	private long[] sendTab;
	private List<Checkpoint> backups;
	private int nodeId;
	private String prefix;
	private boolean applicativeBlocked;
	private int rollbackAck;
	
	public CheckpointNode (String prefix) {
		this.prefix = prefix;
		this.setReceiveTab(new long[Network.size()]);
		this.setSendTab(new long[Network.size()]);
		this.setState(0);
		this.setBackups(new LinkedList<Checkpoint>());
		this.transportPid = Configuration.getPid(prefix + ".transport");
		this.mypid = Configuration.getPid(prefix + ".myself");
		this.transport = null;
		this.applicativeBlocked = false;
		this.rollbackAck = 0;
		
		for (int i = 0; i < Network.size(); i++) {
			this.receiveTab[i] = 0;
			this.sendTab[i] = 0;
		}
	}
	
	@Override
	public void processEvent(Node node, int pid, Object event) {
		this.receive((Message)event);
	}
	
    public void send(checkpoint.Message chkptMsg, Node dest) {
    	this.sendTab[(int) dest.getID()]++;
    	this.transport.send(getMyNode(), dest, chkptMsg, this.mypid);
    }
    /* 				Etape 1 : 
     * Incrémenter state
     * Tirage aléatoire pour envoyer un msg applicatif
     * Tirage pour savoir si on fait un broadcast
     * Prochain self
     */ 
    private void nextStepSelf () {
    	setState(getState() + 1);
    	
    	System.out.println("[" + CommonState.getTime() + "] " + this + ">  state : " + getState());
		
    	// Tirage applicatif
    	double rand = CommonState.r.nextDouble();
    	if (rand <= 0.5 && !applicativeBlocked) {
    		int destId = CommonState.r.nextInt(Network.size());
    		Message chkptMsg = new Message(nodeId, Message.CHKPT_APP, this.getState());
        	System.out.println("[" + CommonState.getTime() + "] " + this + "> Send to : " + destId
        			+ " value : " + chkptMsg.getContent()
        			+ " canal (" + sendTab[destId]+ ")" );
    		send(chkptMsg, Network.get(destId));
    	}
    	
    	// Tirage pour le broadcast
    	rand = CommonState.r.nextDouble();
    	if (rand <= 0.005 && !applicativeBlocked) {
    		for (int i = 0; i < Network.size(); i++) {
        		Message chkptMsg = new Message(nodeId, Message.CHKPT_APP, this.getState());
    	    	System.out.println("[" + CommonState.getTime() + "] " + this + "> Send to : " + i
    	    			+ " value : " + chkptMsg.getContent()
    	    			+ " canal (" + sendTab[i]+ ")" );
        		send(chkptMsg, Network.get(i));
    		}
    	}
    	
    	// Prochain self
    	Message selfMsg = new Message(nodeId, Message.CHKPT_SELF, -32);
    	int delay = CommonState.r.nextInt(3) + 3;
    	EDSimulator.add(delay, selfMsg, Network.get(this.nodeId), mypid);
    }
    
    private void nextStepBackup () {

    	save();
    	System.out.println("[" + CommonState.getTime() + "] " + this + ">  State saved! ");
    	
    	// Prochain save
    	Message saveMsg = new Message(nodeId, Message.CHKPT_BCKP, 99);
    	int delay = CommonState.r.nextInt(131) + 45;
    	EDSimulator.add(delay, saveMsg, Network.get(this.nodeId), mypid);	
    }
	
    private void rollback () {
    	this.rollbackAck = 0;
    	System.out.println("[" + CommonState.getTime() + "] " + this + ": Rollingback bitches");
    	for(int i = 0; i < Network.size(); i++) {
    		if (nodeId != i) {
    			Message rollMsg = new Message(nodeId, Message.CHKPT_RLBK, sendTab[i]);
    			send(rollMsg, Network.get(i));
    		}
    	}
    }
    
    // Algorithme de Juang-Venkatesan
    private void receiveRollback (int source, long content) {
    	/* Répéter N-1 fois // N le nb de sites
		 * 		Pour tout site j ≠ i
		 *			envoyer (<ROLLBACK, i, SENT ij (S i )>)
		 * 		Répéter N-1 fois
		 *			recevoir(<ROLLBACK, j, v>)
		 *			Si (RCVD ji (S i ) > v) // il y a des messages orphelins
		 *				Trouver (S' i ) le plus tardif tel que RCVD ji (S' i ) ≤ v
		 *				S i = S' i
		 *		FinR
		 * FinR
    	 */
    	applicativeBlocked = true;
    	System.out.println("[" + CommonState.getTime() + "] " +this + "> "
				+ "Received rollback message from " + source + " value " + content);
		if (receiveTab[source] > content) {
			while (receiveTab[source] > content) {
				restore();
			}
			System.out.println("[" + CommonState.getTime() + "] " +this + "> "
					+ " final rollback state: " + getState());
			// Broadcast rollback
			rollback();
		} else {
			Message rollAck = new Message(nodeId, Message.CHKPT_RLBK_ACK, 0);
			this.send(rollAck, Network.get(source));
		}
    }
    
    private void receiveRlbkAck () {
    	this.rollbackAck++;
    	if (this.rollbackAck == Network.size() - 1) {
    		System.out.println("[" + CommonState.getTime() + "] " +this + "> "
					+ " Relaunch application");
    		broadcastAppl();
    	}
    }
    
    private void broadcastAppl () {
    	this.applicativeBlocked = false;
		System.out.println("[" + CommonState.getTime() + "] " +this + "> Applicative blocked = false");
    	for(int i = 0; i < Network.size(); i++) {
    		if (nodeId != i) {
    			Message rollMsg = new Message(nodeId, Message.CHKPT_RLBK_DONE, 0);
    			send(rollMsg, Network.get(i));
    		}
    	}
    }
    
    
	private void receive(Message event) {
		switch (event.getType()) {
		case Message.CHKPT_SELF:
			nextStepSelf();
			break;
		case Message.CHKPT_APP:
			this.receiveTab[event.getSource()]++;
			System.out.println("[" + CommonState.getTime() + "] " +this + "> Received applicative " + event.getContent()
					+ " from : " + event.getSource()
					+ " canal (" + receiveTab[event.getSource()]+ ")" );
			break;
		case Message.CHKPT_BCKP:
			nextStepBackup();
			break;
		case Message.CHKPT_RLBK:
			this.rollbackAck = 0;
			receiveRollback(event.getSource(), event.getContent());
			break;
		case Message.CHKPT_FAIL:
			System.out.println("[" + CommonState.getTime() + "] " +this + "> Failing... rollback.");
			applicativeBlocked = true;
			restore();
			rollback();
			break;
		case Message.CHKPT_RLBK_ACK:
			receiveRlbkAck();
			break;
		case Message.CHKPT_RLBK_DONE:
			this.applicativeBlocked = false;
			break;
		}
		
	}

	public Object clone () {
		CheckpointNode dolly = new CheckpointNode(this.prefix);
		return dolly;
	}
	
    public void setTransportLayer(int nodeId) {
    	this.nodeId = nodeId;
    	this.transport = (CKPTransport) Network.get(this.nodeId).getProtocol(this.transportPid);
    }
	
    // Gestion des checkpoints
    public void save () {
    	long[] rTab = Arrays.copyOf(receiveTab, Network.size());
    	long[] sTab = Arrays.copyOf(sendTab, Network.size());
    	
    	Checkpoint chkpt = new Checkpoint(state, rTab, sTab);
    	backups.add(chkpt);
    }
    
    public void restore () {
    	Checkpoint chkpt = backups.get(backups.size() - 1);
    	backups.remove(backups.size() - 1);
    	setState(chkpt.getState());
    	setReceiveTab(chkpt.getReceiveTab());
    	setSendTab(chkpt.getSendTab());
    }
    
    // Retourne le noeud courant
    private Node getMyNode() {
    	return Network.get(this.nodeId);
    }

    public String toString() {
    	return "Node "+ this.nodeId;
    }

	public long getState() {
		return state;
	}

	public void setState(long state) {
		this.state = state;
	}

	public long[] getReceiveTab() {
		return receiveTab;
	}

	public void setReceiveTab(long[] receiveTab) {
		this.receiveTab = receiveTab;
	}

	public long[] getSendTab() {
		return sendTab;
	}

	public void setSendTab(long[] sendTab) {
		this.sendTab = sendTab;
	}

	public List<Checkpoint> getBackups() {
		return backups;
	}

	public void setBackups(List<Checkpoint> backups) {
		this.backups = backups;
	}

}
