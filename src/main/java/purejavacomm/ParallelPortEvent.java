package purejavacomm;

public class ParallelPortEvent {
	public static final int PAR_EV_ERROR = 1;
	public static final int PAR_EV_BUFFER = 2;
	int eventType;

	int getEventType() {
		return eventType;
	}

	boolean getNewValue() {
		return false;
	}

	boolean getOldValue() {
		return false;
	}

}
