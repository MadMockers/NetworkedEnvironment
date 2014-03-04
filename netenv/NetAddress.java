package netenv;

import java.util.LinkedList;
import java.util.Vector;

public class NetAddress {

	private NetNode m_Value;
	private LinkedList<Long> m_vAddress = new LinkedList<Long>();
	
	private boolean m_bFinal = false;
	
	public NetAddress()
	{
		
	}
	
	public NetAddress(long start)
	{
		m_vAddress.add(start);
	}
	
	public NetAddress(long start, NetNode value)
	{
		m_vAddress.add(start);
		m_Value = value;
	}
	
	@SuppressWarnings("unchecked")
	public NetAddress(NetAddress add)
	{
		this(add, false);
	}
	
	@SuppressWarnings("unchecked")
	public NetAddress(NetAddress add, boolean preserveValue)
	{
		m_vAddress = (LinkedList<Long>) add.m_vAddress.clone();
		if(preserveValue) m_Value = add.m_Value;
	}
	
	public void addChild(long id)
	{
		if(!m_bFinal) m_vAddress.addLast(id);
	}
	
	public void addParent(long id)
	{
		if(!m_bFinal) m_vAddress.addFirst(id);
	}
	
	public void setValue(NetNode value)
	{
		if(!m_bFinal) m_Value = value;
	}
	
	public NetNode getValue()
	{
		return m_Value;
	}
	
	public long getLocalId()
	{
		return m_vAddress.get(m_vAddress.size() - 1);
	}
	
	public long[] getAddress()
	{
		long[] address = new long[m_vAddress.size()];
		
		int count = 0;
		for(long i : m_vAddress)
		{
			address[count++] = i;
		}
		
		return address;
	}
	
	public NetAddress getParentAddress()
	{
		NetAddress address = new NetAddress();
		
		for(int i = 0;i < m_vAddress.size() - 1;i++)
		{
			address.addChild(m_vAddress.get(i));
		}
		
		return address;
	}
	
	public void finalize()
	{
		m_bFinal = true;
	}
	
	public String toString()
	{
		String str = "[NetAddress: ";
		for(long i : m_vAddress)
		{
			str += i + "->";
		}
		str += "Self]";
		return str;
	}
}
