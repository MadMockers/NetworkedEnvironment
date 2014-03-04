package netenv;

public class NetFunc extends NetNode
{
	
	public static NetFunc declareFunction(Network network, String name)
	{
		return new NetFunc(network, name, network.getNextKey());
	}
	
	ArgRunnable m_Func;
	
	public NetFunc(Network network, String name, ArgRunnable func)
	{
		super(name, NetNodeType.FUNCTION);
		m_iId = network.getNextKey();
		m_Func = func;
	}
	
	NetFunc(Network network, String name, long id)
	{
		super(name, NetNodeType.FUNCTION);
		m_iId = id;
	}
	
	public void defineFunc(ArgRunnable func)
	{
		m_Func = func;
	}
	
	public void runFunc(Object... args)
	{
		NetAddress self = new NetAddress(getId());
		self.setValue(this);
		self.finalize();
		
		for(NetVarGroup e : m_vParents)
		{
			e.functionRun(self, args);
		}
	}
	
	void run(Object... args)
	{
		if(m_Func != null)
			m_Func.run(args);
	}
}
