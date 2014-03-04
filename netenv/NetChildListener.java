package netenv;

public interface NetChildListener
{	
	public void childAttached(long id);
	public void childDropped(long id);
	
	public void childMovedFrom(NetNode node);
	public void childMovedTo(NetNode node);
}
