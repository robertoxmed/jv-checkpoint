package checkpoint;

import peersim.core.*;
import peersim.config.*;
import peersim.edsim.EDSimulator;

public class Controller implements Control{

	private int checkpointPid;

	public Controller (String prefix) {
		this.checkpointPid = Configuration.getPid(prefix + ".checkpointProtocolPid");
	}

	@Override
	public boolean execute() {
		if (CommonState.getIntTime() > 0) {
			Node dest;
			Message failMsg;
			int victim = CommonState.r.nextInt(Network.size());
			dest = Network.get(victim);
			failMsg = new Message(Network.size(), Message.CHKPT_FAIL, -99);

			EDSimulator.add(0, failMsg, dest, checkpointPid);
		}
		return false;
	}

}
