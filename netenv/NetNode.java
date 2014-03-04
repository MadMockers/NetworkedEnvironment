package netenv;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public abstract class NetNode {

	protected NetNodeType m_eNodeType;
	
	protected Vector<NetVarGroup> m_vParents = new Vector<NetVarGroup>();
	
	protected long m_iId;
	
	private String m_sName;
	
	private boolean m_bPrivate;
	private Set<Long> m_vAuthBroadcasters = new HashSet<Long>();
	
	public NetNode(String name, NetNodeType nodeType)
	{
		m_sName = name;
		m_eNodeType = nodeType;
	}
	
	public NetNodeType getType()
	{
		return m_eNodeType;
	}
	
	public long getId()
	{
		return m_iId;
	}

	public void attachParent(NetVarGroup parent)
	{
		attachParent(parent, null);
	}
	
	public void dropParent(NetVarGroup parent)
	{
		dropParent(parent, null);
	}

	public void attachParent(NetVarGroup parent, Set<Long> doNotBroadcast)
	{
		m_vParents.add(parent);
		if(!parent.contains(this)) parent.attachChild(this);
		
		synchronize(parent, NetSyncType.ADD, doNotBroadcast);
	}
	
	public void dropParent(NetVarGroup parent, Set<Long> doNotBroadcast)
	{
//		m_vParents.remove(parent);
//		if(parent.contains(this)) parent.dropChild(this);
		
		synchronize(parent, NetSyncType.REMOVE, doNotBroadcast);
		m_vParents.remove(parent);
	}
	
	void quietAttachParent(NetVarGroup parent)
	{
		m_vParents.add(parent);
		if(!parent.contains(this)) parent.quietAttachChild(this, true);
	}
	
	void quietDropParent(NetVarGroup parent)
	{
		m_vParents.remove(parent);
		if(parent.contains(this)) parent.quietDropChild(this, true);
	}
	
	public void moveToParent(NetVarGroup oldParent, NetVarGroup newParent)
	{
		/** my plan:
		 * propagate up to find all NetEnvs above 'oldParent' and get their IDs. Do the same for the new parent.
		 * A 'intersection' of these two sets will contain the list of NetEnvs that will receive the 'MOVE' command.
		 * 
		 * propagation will likely be recursion, with NetEnv class adding itself to the HashSet that is being passed up.
		 * Every NetNode will call 'findNetEnvs' (or something similar) on all of their parents.
		 * THOUGHT: Cyclic parents
		 * To prevent cyclic parents (probably already an issue in this library at this point in time anyway), another
		 * HashSet will contain all the NetNode IDs that have already been recursed through. ... fuck that nvm. Just don't
		 * make cyclics in the graph lol.
		 * 
		 * If the old or new parents are using the authorized broadcast feature, then the recursion is not required because
		 * any NetEnv that can be affected is already in the m_vAuthBroadcasters Set of each parent node. Use these to 'intersection'
		 * instead to find out NetEnvs that will get the 'MOVE' command.
		 * 
		 * attach and dropParent will be overloaded with an 'ignore' HashSet. If a NetEnv is to be updated, but is in
		 * that 'ignore' HashSet, they simply won't send the update.
		 * 
		 * A new 'move' function will be added to NetNode, that will propagate up to NetEnv. It will be passed a Set
		 * (the same Set used as the 'ignore' previously). Only NetEnvs that are included in this Set will issue the move
		 * command.
		 * THOUGHT: We need both the starting and ending address of the node. Perhaps instead at the start we
		 * send up a HashMap that references NetAddresses by NetEnv's for each parent. Do the same previously, but for the
		 * moving, simply call 'move' on the NetEnv, which will have a native network function to move it on it's peers.
		 * 
		 * Requirements:
		 * 2 recursions up each parent to get NetEnvs. I think that's about it.
		 * 
		 * WHY:
		 * Initially, when a node is moved to another parent (traditionally), both under the same NetEnv, the old instance
		 * of the NetNode was lost on the peer. Using this move, the NetNode instance on the peer will be retained.
		 */

		HashMap<NetEnv, NetAddress> addressesOld = new HashMap<NetEnv, NetAddress>();
		HashMap<NetEnv, NetAddress> addressesNew = new HashMap<NetEnv, NetAddress>();
		
		oldParent.findNetEnvs(addressesOld, new NetAddress(getId(), this));
		newParent.findNetEnvs(addressesNew, new NetAddress(getId(), this));
		
		// somewhat surprisingly, Set doesn't have union/intersect functionality :(
		Set<Long> inter = new HashSet<Long>();
		for(NetEnv env : addressesOld.keySet())
		{
			if(addressesNew.keySet().contains(env))
			{
				inter.add(env.m_iId);
				env.moveNetNode(addressesOld.get(env), addressesNew.get(env));
			}
		}
		
		dropParent(oldParent, inter);
		attachParent(newParent, inter);
	}
	
	void findNetEnvs(HashMap<NetEnv, NetAddress> addressByNetEnv, NetAddress add)
	{
		System.out.println(this);
		if(!canBroadcast(add.getValue()))
		{
			return;
		}
		add.addParent(getId());
		for(NetVarGroup grp : m_vParents)
		{
			grp.findNetEnvs(addressByNetEnv, new NetAddress(add, true));
		}
	}

	public void setPrivate(boolean priv)
	{
		m_bPrivate = priv;
	}
	
	public boolean isPrivate()
	{
		return m_bPrivate;
	}
	
	public void addAuthorizedBroadcaster(long id)
	{
		setPrivate(true);
		m_vAuthBroadcasters.add(id);
	}
	
	public void removeAuthorizedBroadcaster(long id)
	{
		m_vAuthBroadcasters.remove(id);
		setPrivate(m_vAuthBroadcasters.size() != 0);
	}
	
	public boolean isAuthorizedBroadcaster(long id)
	{
		return m_vAuthBroadcasters.contains(id) || !m_bPrivate;
	}
	
	public boolean canBroadcast(NetNode node)
	{
		if(!node.isPrivate()) return true;
		
		for(Long netId : m_vAuthBroadcasters)
		{
			if(node.m_vAuthBroadcasters.contains(netId))
			{
				return true;
			}
		}
		return false;
	}
	
	public void synchronize(NetVarGroup parent, NetSyncType syncType, Set<Long> doNotBroadcast)
	{
		Vector<NetAddress> groups = new Vector<NetAddress>();
		Vector<NetAddress> nodes = new Vector<NetAddress>();
		
		NetAddress self = new NetAddress(getId());
		self.setValue(this);
		self.finalize();
		
		if(getType() == NetNodeType.GROUP)
		{
			groups.add(self);
		}
		else
		{
			nodes.add(self);
		}
		
		getChildren(groups, nodes);
		if(syncType == NetSyncType.ADD)
		{
			for(NetAddress grp : groups)
			{
				parent.groupCreated((NetVarGroup) grp.getValue(), grp, doNotBroadcast);
			}
			
			for(NetAddress node : nodes)
			{
				if(node.getValue().getType() == NetNodeType.VARIABLE) parent.variableCreated((NetVar<?>) node.getValue(), node, doNotBroadcast);
				if(node.getValue().getType() == NetNodeType.FUNCTION) parent.functionCreated((NetFunc) node.getValue(), node, doNotBroadcast);
			}
		}
		else if(syncType == NetSyncType.REMOVE)
		{
			if(getType() == NetNodeType.GROUP) parent.groupDestroyed(self, doNotBroadcast);
			else if(getType() == NetNodeType.VARIABLE) parent.variableDestroyed(self, doNotBroadcast);
			else if(getType() == NetNodeType.FUNCTION) parent.functionDestroyed(self, doNotBroadcast);
//			for(NetAddress grp : groups)
//			{
//				parent.groupDestroyed(grp);
//			}
//			
//			for(NetAddress node : nodes)
//			{
//				if(node.getValue().getType() == NetNodeType.VARIABLE) parent.variableDestroyed(node);
//				if(node.getValue().getType() == NetNodeType.FUNCTION) parent.functionDestroyed(node);
//			}
		}
	}
	
	public void attachChild(NetNode child)
	{
		
	}
	
	public void dropChild(String name)
	{
		dropChild(getChild(name));
	}
	
	public void dropChild(NetNode child)
	{
		
	}
	
	public void quietAttachChild(NetNode child, boolean notify)
	{
		
	}
	
	public void quietDropChild(NetNode child, boolean notify)
	{
		
	}

	public void getChildren(Vector<NetAddress> grps, Vector<NetAddress> vars)
	{
		getChildren(grps, vars, new NetAddress(m_iId));
	}
	
	public void getChildren(Vector<NetAddress> grps, Vector<NetAddress> vars, NetAddress address)
	{
		
	}
	
	public NetNode getChild(long id)
	{
		return null;
	}
	
	public NetNode getChild(String name)
	{
		return null;
	}
	
	//abstract public void attachParent(NetVarGroup parent);
	//abstract public void dropParent(NetVarGroup parent);
	
	public NetAddress getAddress(NetAddress address, NetNode node)
	{
		if(this == node) 
		{
			return address;
		}
		return null;
	}

	public String getName() {
		return m_sName;
	}
	
	public String toString()
	{
		return "[NetNode; id: " + getId() + "; name: " + getName() + "; type: " + getType() + "]";
	}
	
	public int hashCode()
	{
		return new Long(m_iId).hashCode();
	}
	
	public boolean equals(NetNode node)
	{
		return m_iId == node.m_iId;
	}
}
