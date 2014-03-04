package netenv;

import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;
import java.io.*;

public class NetEnv extends NetVarGroup implements Runnable {
	
	public static boolean m_bStackOnNextWrite = false;
	
	final byte VAR_UPDA = 0;
	final byte VAR_CREA = 1;
	final byte DESTROY = 2;
	
	final byte NDE_MOVE = 3;
	
	final byte GRP_CREA = 4;
	
	final byte NTE_REREG = 6;
	final byte NTE_CLOSE = 7;
	final byte NTE_CLOSE_CONF = 8;
	
	final byte NTF_RUN = 9;
	final byte NTF_CREA = 10;
	
	Network m_Network;
	
	Socket m_Socket;
	
	InputStream m_IS;
	OutputStream m_OS;
	
	private Vector<String> m_vAllowedFunctions = new Vector<String>();
	private Vector<Buffer> m_vOutputQueue = new Vector<Buffer>();
	
	NetEnv(Socket socket, Network network) throws IOException
	{
		super(network, "root");
		m_Network = network;
		
		m_Socket = socket;
		
		m_IS = m_Socket.getInputStream();
		m_OS = m_Socket.getOutputStream();
		
		new Printer();
		new Thread(this).start();
	}
		
	protected NetEnv(Network network, Socket socket) throws IOException
	{
		super(network, "root");
		m_Network = network;
		
		m_Socket = socket;
		
		m_IS = m_Socket.getInputStream();
		m_OS = m_Socket.getOutputStream();

		new Printer();
		new Thread(this).start();
	}
	
	public Network getNetwork()
	{
		return m_Network;
	}
	
	public void synchronize()
	{
		for(NetNode child : m_vChildren.values())
		{
			child.synchronize(this, NetSyncType.ADD, null);
		}
	}
	
	public void addAllowableFunction(String... names)
	{
		for(String func : names)
		{
			if(!m_vAllowedFunctions.contains(func))
				m_vAllowedFunctions.add(func);
		}
	}
	
	@Override
	void findNetEnvs(HashMap<NetEnv, NetAddress> addressByNetEnv, NetAddress add)
	{
		System.out.println("Found " + this + " " + add);
		
		addressByNetEnv.put(this, new NetAddress(add));
		super.findNetEnvs(addressByNetEnv, add);
	}
	
	@Override
	public void variableCreated(NetVar<?> var, NetAddress address, Set<Long> doNotBroadcast)
	{
		if(!var.isAuthorizedBroadcaster(getId()) || (doNotBroadcast != null && doNotBroadcast.contains(getId()))) return;
		
		Buffer message = new Buffer();
		message.writeByte(VAR_CREA);
		message.writeString(var.getName());
		message.writeEnum(var.getVarType());
		
		long[] aAddress = address.getAddress();
		message.writeInt(aAddress.length);
		for(long i : aAddress)
		{
			message.writeLong(i);
		}
		
		try {
			var.writeValue(message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		//message.writeBuffer(m_OS);
		writeBuffer(message);
	}

	@Override
	public void variableDestroyed(NetAddress address, Set<Long> doNotBroadcast)
	{
		NetNode child = getNode(address);
		NetNode parent = getNode(address.getParentAddress());
		if(!child.isAuthorizedBroadcaster(getId()) || (doNotBroadcast != null && doNotBroadcast.contains(getId()))) return;
		
		child.quietDropParent((NetVarGroup) parent);
		
		Buffer message = new Buffer();
		message.writeByte(DESTROY);
		
		long[] aAddress = address.getAddress();
		message.writeInt(aAddress.length);
		for(long i : aAddress)
		{
			message.writeLong(i);
		}

		//message.writeBuffer(m_OS);
		writeBuffer(message);
	}
	
	public void valueUpdated(NetVar<?> var, NetAddress address)
	{
		if(!getNode(address).isAuthorizedBroadcaster(getId())) return;
		
		Buffer message = new Buffer();
		message.writeByte(VAR_UPDA);
		
		long[] aAddress = address.getAddress();
		message.writeInt(aAddress.length);
		for(long i : aAddress)
		{
			message.writeLong(i);
		}

		try {
			var.writeValue(message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		//message.writeBuffer(m_OS);
		writeBuffer(message);
	}

	@Override
	public void functionCreated(NetFunc func, NetAddress address, Set<Long> doNotBroadcast)
	{
		if(!func.isAuthorizedBroadcaster(getId()) || (doNotBroadcast != null && doNotBroadcast.contains(getId()))) return;
		
		Buffer message = new Buffer();
		message.writeByte(NTF_CREA);
		message.writeString(func.getName());
		
		long[] aAddress = address.getAddress();
		message.writeInt(aAddress.length);
		for(long i : aAddress)
		{
			message.writeLong(i);
		}

		//message.writeBuffer(m_OS);
		writeBuffer(message);
	}

	@Override
	public void functionDestroyed(NetAddress address, Set<Long> doNotBroadcast)
	{
		if(!getNode(address).isAuthorizedBroadcaster(getId()) || (doNotBroadcast != null && doNotBroadcast.contains(getId()))) return;
		
		Buffer message = new Buffer();
		message.writeByte(DESTROY);
		
		long[] aAddress = address.getAddress();
		message.writeInt(aAddress.length);
		for(long i : aAddress)
		{
			message.writeLong(i);
		}

		//message.writeBuffer(m_OS);
		writeBuffer(message);
	}
	
	public void functionRun(NetAddress address, Object... args)
	{		
		Buffer message = new Buffer();
		message.writeByte(NTF_RUN);
		
		long[] aAddress = address.getAddress();
		message.writeInt(aAddress.length);
		for(long i : aAddress)
		{
			message.writeLong(i);
		}
		
		message.writeInt(args.length);
		for(Object obj : args)
		{
			m_Network.writeObject(obj, message);
		}

		//message.writeBuffer(m_OS);
		writeBuffer(message);
	}

	@Override
	public void groupCreated(NetVarGroup grp, NetAddress address, Set<Long> doNotBroadcast)
	{		
		if(!grp.isAuthorizedBroadcaster(getId()) || (doNotBroadcast != null && doNotBroadcast.contains(getId()))) return;
		
		Buffer message = new Buffer();
		message.writeByte(GRP_CREA);
		
		message.writeString(grp.getName());
		
		long[] aAddress = address.getAddress();
		message.writeInt(aAddress.length);
		for(long i : aAddress)
		{
			message.writeLong(i);
		}

		//message.writeBuffer(m_OS);
		writeBuffer(message);
	}

	@Override
	public void groupDestroyed(NetAddress address, Set<Long> doNotBroadcast)
	{
		if(!getNode(address).isAuthorizedBroadcaster(getId()) || (doNotBroadcast != null && doNotBroadcast.contains(getId()))) return;
		
		Buffer message = new Buffer();
		message.writeByte(DESTROY);
		
		long[] aAddress = address.getAddress();
		message.writeInt(aAddress.length);
		for(long i : aAddress)
		{
			message.writeLong(i);
		}

		//message.writeBuffer(m_OS);
		writeBuffer(message);
	}
	
	public void moveNetNode(NetAddress from, NetAddress to)
	{
		Buffer message = new Buffer();
		message.writeByte(NDE_MOVE);

		{
			long[] aAddress = from.getAddress();
			message.writeInt(aAddress.length);
			for(long i : aAddress)
			{
				message.writeLong(i);
			}
		}
		{
			long[] aAddress = to.getAddress();
			message.writeInt(aAddress.length);
			for(long i : aAddress)
			{
				message.writeLong(i);
			}
		}
		
		writeBuffer(message);
	}
	
	private void writeBuffer(Buffer buff)
	{
		if(m_bStackOnNextWrite)
		{
			m_bStackOnNextWrite = false;
			new IllegalStateException("Printing Stack..").printStackTrace();
		}
		m_vOutputQueue.add(buff);
	}
	
	public void close()
	{
		for(Enumeration<NetNode> e = m_vChildren.elements(); e.hasMoreElements(); )
		{
			quietDropChild(e.nextElement(), true);
		}
		
		Buffer message = new Buffer();
		message.writeByte(NTE_CLOSE);

		//message.writeBuffer(m_OS);
		writeBuffer(message);
	}
	
	public NetAddress readNetAddress(Buffer buffer)
	{
		NetAddress address = new NetAddress();
		
		int length = buffer.readInt();
		for(int i = 0;i < length;i++)
		{
			address.addChild(buffer.readLong());
		}
		
		return address;
	}
	
	boolean m_bError = false;
	boolean m_bClose = false;
	
	@SuppressWarnings("unchecked")
	public void run()
	{
		while(!m_bError && !m_bClose)
		{
			try {
				Buffer buffer = Buffer.readBuffer(m_IS);
				
				if(buffer == null)
				{
					close();
					continue;
				}
				
				byte indicator = (byte) buffer.readByte();
				
				switch(indicator)
				{
				case VAR_UPDA:
					{
						NetAddress address = new NetAddress();
						
						int length = buffer.readInt();

						for(int i = 0;i < length;i++)
						{
							address.addChild(buffer.readLong());
						}
						
						try {
							NetNode node = getNode(address);
														
							if(node == null)
							{
								continue;
							}
							
							if(!((getNetwork().getNetworkType() == NetworkType.SERVER && ((NetVar<?>) node).getVarType() == NetVarType.SERVER_ONLY) ||
								 (getNetwork().getNetworkType() == NetworkType.CLIENT && ((NetVar<?>) node).getVarType() == NetVarType.CLIENT_ONLY)))
							{
								if(node.isAuthorizedBroadcaster(getId())) ((NetVar<?>) node).updateValue(buffer);
							}
						} catch (IOException e) {
							System.out.println("Error Reading Updated Var " + address + ": " + e.getMessage());
							e.printStackTrace();
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
							System.out.println("Class sent was undefined.");
						}
					}
					break;
					
				case VAR_CREA:
					{
						// [HACK] protection to stop clients from spamming vars (causing to run out of memory)
						if(getNetwork().getNetworkType() == NetworkType.SERVER)
							continue;
						
						String varName = buffer.readString();
						NetVarType varType = (NetVarType) buffer.readEnum(NetVarType.class);
						
						NetAddress address = new NetAddress();
						
						int length = buffer.readInt();
						for(int i = 0;i < length;i++)
						{
							address.addChild(buffer.readLong());
						}
						
						Object obj = NetVar.readValue(buffer, m_Network);
						
						NetVar<?> var = new NetVar(m_Network, varName, varType, obj, address.getLocalId());
						
						NetNode node = getNode(address.getParentAddress());
						
						node.quietAttachChild(var, true);
					}
					break;
					
				case DESTROY:
					{
						// [HACK] protection to stop clients from maliciously separating parts of the graph
						if(getNetwork().getNetworkType() == NetworkType.SERVER)
							continue;
						NetAddress address = readNetAddress(buffer);
						
						NetNode child = getNode(address);
						NetNode parent = getNode(address.getParentAddress());
						
						if(child == null || parent == null)
							continue;
						
						if(child.isAuthorizedBroadcaster(getId())) parent.quietDropChild(child, true);
					}
					break;
					
				case NDE_MOVE:
					{
						// [HACK] protection to stop clients from maliciously separating parts of the graph
						if(getNetwork().getNetworkType() == NetworkType.SERVER)
							continue;
						
						NetAddress old = readNetAddress(buffer);
						NetAddress neu = readNetAddress(buffer);
						
						NetNode node = getNode(old);
						
						NetVarGroup oldParent = (NetVarGroup) getNode(old.getParentAddress());
						NetVarGroup newParent = (NetVarGroup) getNode(neu.getParentAddress());
						
						oldParent.quietDropChild(node, false);
						newParent.quietAttachChild(node, false);

						oldParent.notifyListenerMoveFrom(node);
						newParent.notifyListenerMoveTo(node);
					}
					
				case NTE_REREG:
					{
//						int varId = buffer.readInt();
//						
//						NetVar<?> var = m_vNetVars.get(varId);
//						
//						unregisterNetVar(varId);
//						
//						registerNetVar(var);
					}
					break;
					
				case GRP_CREA:
					{
						if(getNetwork().getNetworkType() == NetworkType.SERVER)
							continue;
						
						String name = buffer.readString();
						System.out.println(name);
						
						NetAddress address = readNetAddress(buffer);
						NetVarGroup grp = new NetVarGroup(getNetwork(), name, address.getLocalId());
						
						((NetVarGroup) getNode(address.getParentAddress())).quietAttachChild(grp, true);
					}
					break;
					
				case NTF_CREA:
					{
						String funcName = buffer.readString();
						if(getNetwork().getNetworkType() == NetworkType.SERVER && !m_vAllowedFunctions.contains(funcName))
						{
							System.out.println("Client attempted to create forbidden function name: " + funcName);
							continue;
						}
						
						NetAddress address = readNetAddress(buffer);
						
						NetFunc func = new NetFunc(getNetwork(), funcName, address.getLocalId());
						
						NetNode parent = getNode(address.getParentAddress());
						
						parent.quietAttachChild(func, true);
					}
					break;
					
				case NTF_RUN:
					{
						NetFunc func = (NetFunc) getNode(readNetAddress(buffer));
						
						int argsCount = buffer.readInt();
						
						Object[] args = new Object[argsCount];
						
						for(int i = 0;i < argsCount;i++)
						{
							try {
								args[i] = m_Network.readObject(buffer);
							} catch (IOException e) {
								System.out.println("Error Reading Function \"" + func.getName() + "\"'s Argument (" + i + "): ");
								e.printStackTrace();
								args[i] = null;
							} catch (ClassNotFoundException e) {
								e.printStackTrace();
								System.out.println("Class sent was undefined.");
								args[i] = null;
							}
						}

						if(func != null && func.isAuthorizedBroadcaster(getId()))
							func.run(args);
					}
					break;
					
				case NTE_CLOSE:
					{
						for(Enumeration<NetNode> e = m_vChildren.elements(); e.hasMoreElements(); )
						{
							quietDropChild(e.nextElement(), true);
						}
						
						m_bClose = true;
						
						Buffer message = new Buffer();
						message.writeByte(NTE_CLOSE_CONF);
						writeBuffer(message);
					}
					break;
					
				case NTE_CLOSE_CONF:
					{
						m_OS.close();
						m_IS.close();
						m_bClose = true;
					}
					break;
				}
						
			}
			catch(IOException e)
			{
				e.printStackTrace();
				m_bError = true;
			}
			catch(Exception e)
			{
				// Likely an uncaught error from a function called with 'netfunc'
				e.printStackTrace();
			}
		}
	}
	
	private class Printer implements Runnable
	{
		Printer()
		{
			System.out.println("Starting print thread...");
			new Thread(this).start();
		}
		
		public void run()
		{
			while(!m_bClose)
			{
				if(m_vOutputQueue.size() != 0)
				{
					try
					{
						m_vOutputQueue.remove(0).writeBuffer(m_OS);
					}
					catch (IOException e)
					{
						e.printStackTrace();
						m_bClose = true;
						continue;
					}
				}
				else
				{


					try
					{
						Thread.sleep(10);
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}
}
