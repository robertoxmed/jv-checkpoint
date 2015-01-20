package checkpoint;

import peersim.core.*;
import peersim.config.*;

public class Initializer implements peersim.core.Control {
	
	private int checkpointPid;
	
	public Initializer (String prefix) {
		this.checkpointPid = Configuration.getPid(prefix + ".checkpointProtocolPid");
	}
	
	@Override
	public boolean execute() {
		int nodeNb;
		CheckpointNode emitter, current;
		Node dest;
		Message chkptMsg, bckpMsg;
		
		nodeNb = Network.size();
		chkptMsg = new Message(0, Message.CHKPT_SELF, -32);
		bckpMsg = new Message(0, Message.CHKPT_BCKP, 99);
		if (nodeNb < 1) {
			System.err.println("Network size is not positive");
			System.exit(1);
		}
		
		emitter = (CheckpointNode)Network.get(0).getProtocol(this.checkpointPid);
		emitter.setTransportLayer(0);
		
		for (int i = 0; i < nodeNb; i++) {
			dest = Network.get(i);
			current = (CheckpointNode)dest.getProtocol(this.checkpointPid);
			current.setTransportLayer(i);
			emitter.send(chkptMsg, dest);
			emitter.send(bckpMsg, dest);
		}
		
		System.out.println("Init completed");
		
		return false;
	}

}
