package netenv;

public interface NetListener
{
	public void valueChanged(NetVar<?> var, Object oldValue);
}
