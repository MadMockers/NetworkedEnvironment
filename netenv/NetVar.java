package netenv;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

public class NetVar<E> extends NetNode {
	
	private final String m_sName;
	private E m_Value;
	private NetVarValType m_eValueType;
	
	private Network m_Network;
	
	private final boolean m_bReadOnly;
	private final NetVarType m_eVarType;
	
	private boolean m_bHasChanged;
	
	private Vector<NetListener> m_vNetListeners = new Vector<NetListener>();
	
	public NetVar(Network network, String name, NetVarType varType)
	{
		super(name, NetNodeType.VARIABLE);
//		if(!(network.getNetworkType().ordinal() == varType.ordinal() || varType == NetVarType.SERVER_CLIENT))
//		{
//			NetworkTypeException exc = new NetworkTypeException("Cannot create a " + varType.name() + " variable on a " + network.getNetworkType().name() + " network environment.");
//			throw exc;
//		}
		
		m_iId = network.getNextKey();
		
		m_Network = network;
		m_eVarType = varType;
		m_eValueType = NetVarValType.NULL;
		
		m_sName = name;
		m_Value = null;
		
		if(varType == NetVarType.CLIENT_ONLY && m_Network.getNetworkType() == NetworkType.CLIENT)
		{
			m_bReadOnly = false;
		} else if(varType == NetVarType.SERVER_ONLY && m_Network.getNetworkType() == NetworkType.SERVER)
		{
			m_bReadOnly = false;
		} else if(varType == NetVarType.SERVER_CLIENT)
		{
			m_bReadOnly = false;
		} else {
			m_bReadOnly = true;
		}
	}
	
	public NetVar(Network network, String name, NetVarType varType, E value)
	{
		super(name, NetNodeType.VARIABLE);
//		if(!(network.getNetworkType().ordinal() == varType.ordinal() || varType == NetVarType.SERVER_CLIENT))
//		{
//			NetworkTypeException exc = new NetworkTypeException("Cannot create a " + varType.name() + " variable on a " + network.getNetworkType().name() + " network environment.");
//			throw exc;
//		}
		
		m_iId = network.getNextKey();
		
		m_Network = network;
		m_eVarType = varType;
		
		m_sName = name;
		m_Value = value;
		identifyValueType();
		
		if(varType == NetVarType.CLIENT_ONLY && m_Network.getNetworkType() == NetworkType.CLIENT)
		{
			m_bReadOnly = false;
		} else if(varType == NetVarType.SERVER_ONLY && m_Network.getNetworkType() == NetworkType.SERVER)
		{
			m_bReadOnly = false;
		} else if(varType == NetVarType.SERVER_CLIENT)
		{
			m_bReadOnly = false;
		} else {
			m_bReadOnly = true;
		}
	}
	
	NetVar(Network network, String name, NetVarType varType, E value, long id)
	{
		super(name, NetNodeType.VARIABLE);
//		if(!(network.getNetworkType().ordinal() == varType.ordinal() || varType == NetVarType.SERVER_CLIENT))
//		{
//			NetworkTypeException exc = new NetworkTypeException("Cannot create a " + varType.name() + " variable on a " + network.getNetworkType().name() + " network environment.");
//			throw exc;
//		}
		
		m_iId = id;
		
		m_Network = network;
		m_eVarType = varType;
		
		m_sName = name;
		m_Value = value;
		identifyValueType();
		
		if(varType == NetVarType.CLIENT_ONLY && m_Network.getNetworkType() == NetworkType.CLIENT)
		{
			m_bReadOnly = false;
		} else if(varType == NetVarType.SERVER_ONLY && m_Network.getNetworkType() == NetworkType.SERVER)
		{
			m_bReadOnly = false;
		} else if(varType == NetVarType.SERVER_CLIENT)
		{
			m_bReadOnly = false;
		} else {
			m_bReadOnly = true;
		}
	}
	
	public void addNetListener(NetListener list)
	{
		m_vNetListeners.add(list);
	}
	
	public void dropNetListener(NetListener list)
	{
		m_vNetListeners.remove(list);
	}
	
	private void identifyValueType()
	{
		if(m_Value == null)
		{
			m_eValueType = NetVarValType.NULL;
		}
		else if(m_eValueType == NetVarValType.NULL || m_eValueType == null)
		{
			if(m_Value instanceof Integer)
				m_eValueType = NetVarValType.INT;
			else if(m_Value instanceof Float)
				m_eValueType = NetVarValType.FLOAT;
			else if(m_Value instanceof Long)
				m_eValueType = NetVarValType.LONG;
			else if(m_Value instanceof String)
				m_eValueType = NetVarValType.STRING;
			else if(m_Value instanceof Boolean)
				m_eValueType = NetVarValType.BOOLEAN;
//			else if(m_Value instanceof Enum<?>)
//				m_eValueType = NetVarValType.ENUM;
			else
				m_eValueType = NetVarValType.OBJECT;
		}
	}
	
	public boolean testId(long varId)
	{
		return m_iId == varId;
	}
	
	public String getName()
	{
		return m_sName;
	}
	
	public NetVarType getVarType()
	{
		return m_eVarType;
	}
	
	public E getValue()
	{
		return m_Value;
	}
	
	public boolean isUpdated()
	{
		return m_bHasChanged;
	}
	
	public void setValue(E value)
	{
		if(!m_bReadOnly)
		{
			E oldValue = m_Value;
			m_Value = value;
			identifyValueType();
			
			update();
			m_bHasChanged = true;
			notifyNetListeners(oldValue);
		} else {
//			NetworkTypeException exc = new NetworkTypeException("Variable is read-only on a " + m_Network.getNetworkType().name() + " network environment.");
//			throw exc;
			throw new IllegalStateException("Variable '" + getName() + "' is read only!");
			//System.out.println("[NetEnv] Variable is read only!");
		}
	}
	
	void writeValue(Buffer buffer) throws IOException
	{
		buffer.writeByte(m_eValueType.ordinal());
		switch(m_eValueType)
		{
		case NULL:
			break;
			
		case INT:
			buffer.writeInt((Integer) m_Value);
			break;
			
		case FLOAT:
			buffer.writeFloat((Float) m_Value);
			break;
			
		case LONG:
			buffer.writeLong((Long) m_Value);
			break;
			
		case STRING:
			buffer.writeString((String) m_Value);
			break;
			
		case BOOLEAN:
			if(((Boolean) m_Value).booleanValue() == true)
				buffer.writeByte(1);
			else
				buffer.writeByte(0);
			break;
			
//		case ENUM:
//			buffer.writeInt(((Enum<?>) m_Value).ordinal());
//			break;
			
		case OBJECT:
			//SerializableObject obj = new SerializableObject(m_Value);
			m_Network.writeObject(m_Value, buffer);
			break;
		}
	}
	
	@SuppressWarnings("unchecked")
	void updateValue(Object value)
	{
		E oldValue = m_Value;
		m_Value = (E) value;
		m_bHasChanged = true;
		notifyNetListeners(oldValue);
	}
	
	private void notifyNetListeners(E oldValue)
	{
		for(NetListener list : m_vNetListeners)
		{
			list.valueChanged(this, oldValue);
		}
	}
	
	void updateValue(Buffer buffer) throws IOException, ClassNotFoundException
	{
		NetVarValType type = NetVarValType.values()[buffer.readByte()];
		if(type != m_eValueType && type != NetVarValType.NULL && m_eValueType != NetVarValType.NULL)
		{
			throw new IllegalStateException("[NetEnv] Error!! The transmitted variable type does not match the type of the local variable. " + type + " != " + m_eValueType);
		}
		
		switch(type)
		{
		case NULL:
			m_Value = null;
			break;
			
		case INT:
			updateValue((Integer) buffer.readInt());
			break;
			
		case FLOAT:
			updateValue((Float) buffer.readFloat());
			break;
			
		case LONG:
			updateValue((Long) buffer.readLong());
			break;
			
		case STRING:
			updateValue(buffer.readString());
			break;
			
		case BOOLEAN:
			updateValue((Boolean) (buffer.readByte() == 1));
			break;
			
//		case ENUM:
//			updateValue(intToEnum(buffer.readInt()));
			
		case OBJECT:
			updateValue(m_Network.readObject(buffer));
			break;
		}
	}
	
	public Enum<?> intToEnum(int ordinal)
	{
		System.out.println("Int To Enum: " + ordinal);
		if(!(m_Value instanceof Enum<?>) && m_Value != null)
		{
			throw new IllegalStateException("Value is not an Enum: " + m_Value.getClass().getName());
		}
		
		try {
			Method method = m_Value.getClass().getMethod("values");
			try {
				Object[] values = (Object[]) method.invoke(m_Value);
				
				return (Enum<?>) values[((Enum<?>) m_Value).ordinal()];
			} catch (IllegalArgumentException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IllegalAccessException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (InvocationTargetException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (SecurityException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoSuchMethodException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}
	
	static Object readValue(Buffer buffer, Network net) throws IOException, ClassNotFoundException
	{
		NetVarValType type = NetVarValType.values()[buffer.readByte()];
		
		switch(type)
		{
		case NULL:
			return null;
			
		case INT:
			return (Integer) buffer.readInt();
			
		case FLOAT:
			return (Float) buffer.readFloat();
			
		case LONG:
			return (Long) buffer.readLong();
			
		case STRING:
			return buffer.readString();
			
		case BOOLEAN:
			return (Boolean) (buffer.readByte() == 1);
			
		case OBJECT:
			return net.readObject(buffer);
		}
		return null;
	}
	
	public void update()
	{		
		for(NetVarGroup parent : m_vParents)
		{
			NetAddress na = new NetAddress(getId());
			na.setValue(this);
			parent.valueUpdated(this, na);
		}
	}
	
	public String toString()
	{
		return "[NetVar: Super: " + super.toString() + "; Value: " + getValue() + "]";
	}

	public boolean isReadOnly()
	{
		return m_bReadOnly;
	}
}
