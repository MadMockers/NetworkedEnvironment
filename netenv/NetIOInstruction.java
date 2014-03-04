/**
 * Elcire and all of its code is copyrighted Glen Chatfield 2010.
 */
package netenv;

/**
 * @author Glen Chatfield
 *
 */
public interface NetIOInstruction
{
	public Object read(Buffer buf);
	public void write(Buffer buf, Object obj);
}
