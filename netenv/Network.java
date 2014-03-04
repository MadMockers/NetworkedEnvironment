package netenv;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Hashtable;

public class Network implements Runnable 
{
	private Hashtable<Integer, NetIOInstruction> m_vNetInstructions;
		
	private int PORT = 12834;
	
	private long m_iKeyCount;
	private NetworkType m_eNetType;
	
	private String m_sHost;
	
	private boolean m_bClose;
	
	public Network(NetworkType netType, String host)
	{
		m_eNetType = netType;
		m_iKeyCount = 0;
		m_bClose = false;
		m_sHost = host;
		
		m_vNetInstructions = new Hashtable<Integer, NetIOInstruction>();
		
		if(netType == NetworkType.SERVER)
		{
			if(!host.equals("localhost")) System.out.println("[NETWORK WARNING] SERVER Network Host should be 'localhost'.");
			new Thread(this).start();
		}
		else
		{
			updateServerTime();
		}
	}
	
	public NetworkType getNetworkType()
	{
		return m_eNetType;
	}
	
	public long getNextKey()
	{
		if(m_eNetType == NetworkType.SERVER)
		{
			return m_iKeyCount++;
		}
		else
		{
			long key = -1;
			
			InetAddress address;
			try {
				address = InetAddress.getByName(m_sHost);
				Socket socket = new Socket(address, PORT);
				
				socket.getOutputStream().write(1);
				
				Buffer buffer = Buffer.readBuffer(socket.getInputStream());
				
				key = buffer.readLong();
			} catch (UnknownHostException e) {
				System.out.println("[NETWORK ERROR] Unable to find host: " + m_sHost);
				e.printStackTrace();
			} catch (IOException e) {
				System.out.println("[NETWORK ERROR] Error reading new key.");
				e.printStackTrace();
			}
			
			return key;
		}
	}
	
	public void registerIOInstruction(String name, NetIOInstruction inst)
	{
		if(m_vNetInstructions.get(name.hashCode()) == null)
			m_vNetInstructions.put(name.hashCode(), inst);
	}
	
	public void writeObject(Object obj, Buffer buffer)
	{
		NetIOInstruction inst = m_vNetInstructions.get(obj.getClass().getName().hashCode());
		if(inst == null)
		{
			buffer.writeByte(1);
			Serializable param = (Serializable) obj;
			try {
				new ObjectOutputStream(buffer.getOutputStream()).writeObject(param);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			buffer.writeByte(0);
			buffer.writeInt(obj.getClass().getName().hashCode());
			inst.write(buffer, obj);
		}
	}
	
	public Object readObject(Buffer buffer) throws IOException, ClassNotFoundException
	{
		byte ind = (byte) buffer.readByte();

		if(!(ind == 0 || ind == 1))
			throw new IllegalStateException("Indicator is a value other than 0 or 1");
		
		if(ind == 0)
		{
			int nameHash = buffer.readInt();
			NetIOInstruction inst = m_vNetInstructions.get(nameHash);
			if(inst == null)
			{
				throw new IllegalStateException("Expecting object with a NetIOInstruction.");
			}
			
			return inst.read(buffer);
		}
		else
		{
			ObjectInputStream ois = new ObjectInputStream(buffer.getInputStream());
			return ois.readObject();
		}
	}
	
	private long m_lLastSTimeUpdate = 0L;
	private long m_lServerTimeOffset = 0L;
	private long m_lLatency = 0L;
	
	public long getServerTime()
	{
		if(m_lLastSTimeUpdate + 10000L < System.currentTimeMillis())
			updateServerTime();
		return System.currentTimeMillis() + m_lServerTimeOffset;
	}
	
	public long getLatency()
	{

		if(m_lLastSTimeUpdate + 5000L < System.currentTimeMillis())
			updateServerTime();
		return m_lLatency;
	}
	
	private void updateServerTime()
	{
		InetAddress address;
		try {
			address = InetAddress.getByName(m_sHost);
			Socket socket = new Socket(address, PORT);
			
			long sentAt = System.currentTimeMillis();
			socket.getOutputStream().write(2);
			
			Buffer buffer = Buffer.readBuffer(socket.getInputStream());
			long tripTime = System.currentTimeMillis() - sentAt;
			m_lLatency = (long) (tripTime / 2.0);
			
			long serverTime = (long) buffer.readLong();
			m_lServerTimeOffset = serverTime - System.currentTimeMillis();
			m_lLastSTimeUpdate = System.currentTimeMillis();
			
		} catch (UnknownHostException e) {
			System.out.println("[NETWORK ERROR] Unable to find host: " + m_sHost);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("[NETWORK ERROR] Error reading server time.");
			e.printStackTrace();
		}
	}
	
	public NetEnv getNetworkedEnvironment(Socket socket) throws IOException
	{
		if(m_eNetType == NetworkType.SERVER) return new NetEnv(socket, this);
		else return null;
	}
	
	public NetEnv getNetworkedEnvironment(String host, int port) throws IOException
	{
		if(!host.equals(m_sHost)) return null;
		InetAddress address = InetAddress.getByName(host);
		Socket socket = new Socket(address, port);
		return new NetEnv(socket, this);
	}
	
	public void close()
	{
		m_bClose = true;
	}
	
	public void run()
	{
		ServerSocket serverSocket = null;
		
		try {
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e) {
			System.out.println("[NETWORK ERROR] Network of type 'SERVER' unable to listen for ID requests.");
			e.printStackTrace();
		}
		
		while(!m_bClose)
		{
			try {
				Socket socket = serverSocket.accept();
				
				new NetworkHandler(socket);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private class NetworkHandler implements Runnable
	{
		Socket m_Socket;
		OutputStream m_OS;
		InputStream m_IS;
		
		NetworkHandler(Socket socket) throws IOException
		{
			m_Socket = socket;
			m_OS = socket.getOutputStream();
			m_IS = socket.getInputStream();
			
			new Thread(this).start();
		}
		
		public void run()
		{
			try
			{
				int in = m_IS.read();
				
				switch(in)
				{
				case 1:
					// Get next Key
					{
						Buffer buffer = new Buffer();
						buffer.writeLong(getNextKey());
						buffer.writeBuffer(m_OS);
					}
					break;
					
				case 2:
					// Respond with server time
					{
						Buffer buffer = new Buffer();
						buffer.writeLong(System.currentTimeMillis());
						buffer.writeBuffer(m_OS);
					}
					break;
				}
				
				m_Socket.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
	}
}
