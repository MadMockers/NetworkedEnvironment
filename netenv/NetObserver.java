package netenv;

import java.util.Set;

public interface NetObserver {

	public void variableCreated(NetVar<?> var, NetAddress add, Set<Long> doNotBroadcast);
	public void variableDestroyed(NetAddress add, Set<Long> doNotBroadcast);
	public void valueUpdated(NetVar<?> var, NetAddress add);
	
	public void functionCreated(NetFunc func, NetAddress add, Set<Long> doNotBroadcast);
	public void functionDestroyed(NetAddress add, Set<Long> doNotBroadcast);
	public void functionRun(NetAddress add, Object... args);
	
	public void groupCreated(NetVarGroup grp, NetAddress add, Set<Long> doNotBroadcast);
	public void groupDestroyed(NetAddress add, Set<Long> doNotBroadcast);
	
	public Network getNetwork();
	
}
