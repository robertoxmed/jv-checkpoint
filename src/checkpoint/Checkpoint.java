package checkpoint;

public class Checkpoint {
	private long state;
	private long[] receiveTab;
	private long[] sendTab;
	
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

	public Checkpoint(long state, long[] receiveTab, long[] sendTab) {
		super();
		this.state = state;
		this.receiveTab = receiveTab;
		this.sendTab = sendTab;
	}
}
