package checkpoint;

public class Message {
	
	public final static int CHKPT_SELF = 0;
	public final static int CHKPT_APP = 1;
	public final static int CHKPT_BCKP = 2;
	public final static int CHKPT_RLBK = 3;
	
	private int type;
	private long content;
	private int source;
	
	Message (int source, int type, long content) {
		this.setType(type);
		this.setContent(content);
		this.setSource(source);
	}

	public long getContent() {
		return content;
	}

	public void setContent(long content) {
		this.content = content;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getSource() {
		return source;
	}

	public void setSource(int source) {
		this.source = source;
	}
}
