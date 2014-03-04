package netenv;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

public class NetVarGroup extends NetNode implements NetObserver {

	Network m_Network;

	Hashtable<Long, NetNode> m_vChildren = new Hashtable<Long, NetNode>();
	Hashtable<String, NetNode> m_vChildrenByName = new Hashtable<String, NetNode>();
	
	Vector<NetChildListener> m_vListeners = new Vector<NetChildListener>();
	
	NetVar<Integer> m_iNumChildren;
	
	public NetVarGroup(Network network, String name)
	{
		super(name, NetNodeType.GROUP);
		m_iId = network.getNextKey();
		m_Network = network;
		
		quietAttachChild(m_iNumChildren = new NetVar<Integer>(network, "numChild", NetVarType.values()[network.getNetworkType().ordinal()], 1), true);
	}
	
	NetVarGroup(Network network, String name, long id)
	{
		super(name, NetNodeType.GROUP);
		m_iId = id;
		m_Network = network;
		
		// quietAttachChild(m_iNumChildren = new NetVar<Integer>(network, "numChild", NetVarType.values()[network.getNetworkType().ordinal()], 1));
	}
	
	public void attachChild(NetNode child)
	{
		m_vChildren.put(child.getId(), child);
		m_vChildrenByName.put(child.getName(), child);
		if(!child.m_vParents.contains(this))
			child.attachParent(this);
		
		if(!m_iNumChildren.isReadOnly())
			m_iNumChildren.setValue(m_vChildren.size());
		
		notifyListenerAttach(child.getId());
	}
	
	public void dropChild(NetNode child)
	{
		//m_vChildren.remove(child.getId());
		//m_vChildren.remove(child.getName().hashCode());
		child.dropParent(this);
		
		m_iNumChildren.setValue(m_vChildren.size() - 1);
		
		notifyListenerDrop(child.getId());
		
		m_vChildren.remove(child.getId());
		m_vChildrenByName.remove(child.getName());
	}
	
	public void quietAttachChild(NetNode child, boolean notify)
	{
		m_vChildren.put(child.getId(), child);
		//System.out.println("Adding Node: " + child.getName() + " with hashcode " + child.getName().hashCode() + " to parent " + this);
		m_vChildrenByName.put(child.getName(), child);
		if(!child.m_vParents.contains(this)) child.quietAttachParent(this);
		
		if(m_iNumChildren == null && child.getType() == NetNodeType.VARIABLE)
		{
			if(child.getName().equals("numChild"))
				m_iNumChildren = (NetVar<Integer>) child;
		}
		
		if(notify)
			notifyListenerAttach(child.getId());
	}
	
	public void quietDropChild(NetNode child, boolean notify)
	{
		m_vChildren.remove(child.getId());
		m_vChildrenByName.remove(child.getName());
		child.quietDropParent(this);
		
		if(notify)
			notifyListenerDrop(child.getId());
	}
	
	public void addNetListener(NetChildListener list)
	{
		m_vListeners.add(list);
	}
	
	public void removeNetListener(NetChildListener list)
	{
		m_vListeners.remove(list);
	}
	
	private void notifyListenerAttach(long id)
	{
		Enumeration<NetChildListener> lists = m_vListeners.elements();
		while(lists.hasMoreElements())
		{
			NetChildListener list = lists.nextElement();
			list.childAttached(id);
		}
	}
	
	private void notifyListenerDrop(long id)
	{
		Enumeration<NetChildListener> lists = m_vListeners.elements();
		while(lists.hasMoreElements())
		{
			NetChildListener list = lists.nextElement();
			list.childDropped(id);
		}
	}
	
	void notifyListenerMoveFrom(NetNode n)
	{
		Enumeration<NetChildListener> lists = m_vListeners.elements();
		while(lists.hasMoreElements())
		{
			NetChildListener list = lists.nextElement();
			list.childMovedFrom(n);
		}
	}
	
	void notifyListenerMoveTo(NetNode n)
	{
		Enumeration<NetChildListener> lists = m_vListeners.elements();
		while(lists.hasMoreElements())
		{
			NetChildListener list = lists.nextElement();
			list.childMovedTo(n);
		}
	}
	
	public Network getNetwork() {
		return m_Network;
	}

	public boolean contains(NetNode child)
	{
		return m_vChildren.contains(child);
	}
	
	public void getChildren(Vector<NetAddress> grps, Vector<NetAddress> other, NetAddress address)
	{
		for(NetNode child : m_vChildren.values())
		{
			NetAddress childAdd = new NetAddress(address);
			childAdd.addChild(child.getId());
			
			if(child.getType() != NetNodeType.GROUP){
				childAdd.setValue(child);
				childAdd.finalize();
				other.add(childAdd);
			}
			else
			{
				childAdd.setValue(child);
				childAdd.finalize();
				grps.add(childAdd);
				((NetVarGroup) child).getChildren(grps, other, new NetAddress(childAdd));
			}
		}
	}
	
	public NetAddress getAddress(NetAddress address, NetNode node)
	{
		if(node == this)
		{
			return address;
		}
		else
		{
			for(NetNode tNode : m_vChildren.values())
			{
				NetAddress childAdd = new NetAddress(address);
				childAdd.addChild(tNode.getId());
				childAdd.finalize();
				
				NetAddress nodeAdd = tNode.getAddress(childAdd, node);
				if(nodeAdd != null) return nodeAdd;
			}
		}
		
		return null;
	}
	
	public NetAddress getAddress(NetNode node)
	{
		return getAddress(new NetAddress(getId()), node);
	}
	
	public NetNode getNode(NetAddress address)
	{
		long[] aAddress = address.getAddress();
		
		
		NetNode node = this;
		for(long nodeId : aAddress)
		{
			node = node.getChild(nodeId);
			if(node == null)
				break;
			
			if(!node.isAuthorizedBroadcaster(getId()))
			{
				node = null;
				break;
			}
		}
		
		return node;
	}
	
	public NetNode getChild(String name)
	{
		return m_vChildrenByName.get(name);
		//return getChildFromNameHash(name.hashCode());
	}
	
	public NetNode getChild(long id)
	{
		return m_vChildren.get(id);
	}
	
	public void printAllChildren()
	{
		System.out.println("---Children Of " + this + "---");
		for(NetNode node : m_vChildren.values())
		{
			System.out.println(node);
		}
		System.out.println("------------------------------------------");
	}
	
	public boolean hasAllChildren()
	{
		if(m_iNumChildren == null)
			return false;
		
		for(NetNode child : m_vChildren.values())
		{
			if(child instanceof NetVarGroup && !((NetVarGroup) child).hasAllChildren())
				return false;
		}

		for(NetNode node : m_vChildren.values())
		{
			if(node instanceof NetVarGroup)
				if(!((NetVarGroup) node).hasAllChildren())
					return false;
		}

		return m_iNumChildren.getValue() == m_vChildren.size();
	}
	
	public NetVarGroup getGroup(String name)
	{
		return (NetVarGroup) getChild(name);
	}
	
	public NetVar<?> getVar(String name)
	{
		return (NetVar<?>) getChild(name);
	}
	
	public NetFunc getFunc(String name)
	{
		return (NetFunc) getChild(name);
	}
	
//	public NetNode getChildFromNameHash(int nameHash)
//	{
//		return m_vChildrenByName.get(nameHash);
//	}
	
	public void valueUpdated(NetVar<?> var, NetAddress address) {
		if(!canBroadcast(address.getValue())) return;
		
		NetAddress self = new NetAddress(address, true);
		self.addParent(getId());
		self.finalize();
		
		for(NetVarGroup e : m_vParents)
		{
			e.valueUpdated(var, self);
		}
	}

	public void variableCreated(NetVar<?> var, NetAddress address, Set<Long> doNotBroadcast) {
		if(!canBroadcast(address.getValue())) return;
		
		NetAddress self = new NetAddress(address, true);
		self.addParent(getId());
		self.finalize();
		
		for(NetVarGroup e : m_vParents)
		{
			e.variableCreated(var, self, doNotBroadcast);
		}
	}

	public void variableDestroyed(NetAddress address, Set<Long> doNotBroadcast) {
		if(!canBroadcast(address.getValue())) return;
		
		NetAddress self = new NetAddress(address, true);
		self.addParent(getId());
		self.finalize();
		
		for(NetVarGroup e : m_vParents)
		{
			e.variableDestroyed(self, doNotBroadcast);
		}
	}

	public void functionCreated(NetFunc func, NetAddress address, Set<Long> doNotBroadcast) {
		if(!canBroadcast(address.getValue())) return;
		
		NetAddress self = new NetAddress(address, true);
		self.addParent(getId());
		self.finalize();
		
		for(NetVarGroup e : m_vParents)
		{
			e.functionCreated(func, self, doNotBroadcast);
		}
	}

	public void functionDestroyed(NetAddress address, Set<Long> doNotBroadcast) {
		if(!canBroadcast(address.getValue())) return;
		
		NetAddress self = new NetAddress(address, true);
		self.addParent(getId());
		self.finalize();
		
		for(NetVarGroup e : m_vParents)
		{
			e.functionDestroyed(self, doNotBroadcast);
		}
	}

	public void functionRun(NetAddress address, Object... args) {
		NetAddress self = new NetAddress(address, true);
		self.addParent(getId());
		self.finalize();
		
		for(NetVarGroup e : m_vParents)
		{
			e.functionRun(self, args);
		}
	}
	
	public void groupCreated(NetVarGroup grp, NetAddress address, Set<Long> doNotBroadcast) {
		if(!canBroadcast(address.getValue())) return;
		
		NetAddress self = new NetAddress(address, true);
		self.addParent(getId());
		self.finalize();
		
		for(NetVarGroup e : m_vParents)
		{
			e.groupCreated(grp, self, doNotBroadcast);
		}
	}

	public void groupDestroyed(NetAddress address, Set<Long> doNotBroadcast) {
		if(!canBroadcast(address.getValue())) return;
		
		NetAddress self = new NetAddress(address, true);
		self.addParent(getId());
		self.finalize();
		
		for(NetVarGroup e : m_vParents)
		{
			e.groupDestroyed(self, doNotBroadcast);
		}
	}
}
