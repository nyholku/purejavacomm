package purejavacomm;

public interface CommPortOwnershipListener {
	public static final int PORT_OWNED = 1;
	public static final int PORT_OWNERSHIP_REQUESTED = 3;
	public static final int PORT_UNOWNED = 2;

	public void ownershipChange(int type);
}
