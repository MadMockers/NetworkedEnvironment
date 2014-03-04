package netenv;

import java.awt.datatransfer.Transferable;
import java.io.*;
import java.util.ArrayList;
import java.util.Vector;

public class Buffer
{
	private static long m_lTotalOut = 0L;
	private static long m_lTotalIn = 0L;
	
	ArrayList<Byte> buffer;
	
	int writePosition = 0;
	int readPosition = 0;
	
	public Buffer()
	{
		buffer = new ArrayList<Byte>(64);
	}
	
	public Buffer(byte[] b)
	{
		buffer = new ArrayList<Byte>(b.length);
		
		for(byte a : b)
		{
			writeByte(a);
		}
	}
	
	public void clearBuffer() {
		buffer.clear();
		writePosition = 0;
		readPosition = 0;
	}
	
//	public void addToBufferSize(int size) {
//		byte[] buff = new byte[buffer.size() + size];
//		for(int i = 0;i < buffer.size();i++) {
//			buff[i] = buffer[i];
//		}
//		buffer = buff;
//	}
	
	public int size()
	{
		return (int) writePosition;
	}
	
	public void writeByte(byte b) {
		//addToBufferSize(1);
		//buffer[writePosition++] = b;
		buffer.add(b);
	}
	
	public void writeByte(int iB) {
		writeByte((byte) iB);
	}
	
	public int readByte() {
		if(readPosition == buffer.size())
		{
			return -1;
		}
		else
		{
			int b = buffer.get(readPosition++);
			if(b < 0)
				b += 0x100;
			
			return b;
		}
	}
	
	public void writeInt(int i) {
		writeByte((byte) (i & 255));
		i >>>= 8;
		writeByte((byte) (i & 255));
		i >>>= 8;
		writeByte((byte) (i & 255));
		i >>>= 8;
		writeByte((byte) (i & 255));
		i >>>= 8;
	}
	
	public int readInt() {
        int i = (readByte() & 255);
        i |= ((readByte() & 255) << 8);
        i |= ((readByte() & 255) << 16);
        i |= ((readByte() & 255) << 24);
        return i;
	}
	
	public void writeLong(long i) {
		writeByte((byte) (i & 255));
		i >>>= 8;
		writeByte((byte) (i & 255));
		i >>>= 8;
		writeByte((byte) (i & 255));
		i >>>= 8;
		writeByte((byte) (i & 255));
		i >>>= 8;
		writeByte((byte) (i & 255));
		i >>>= 8;
		writeByte((byte) (i & 255));
		i >>>= 8;
		writeByte((byte) (i & 255));
		i >>>= 8;
		writeByte((byte) (i & 255));
		i >>>= 8;
	}
	
	public long readLong() {
        long i = (readByte() & 255);
        i |= ((readByte() & 255) << 8);
        i |= ((readByte() & 255) << 16);
        i |= ((readByte() & 255) << 24);
        i |= ((readByte() & 255) << 32);
        i |= ((readByte() & 255) << 40);
        i |= ((readByte() & 255) << 48);
        i |= ((readByte() & 255) << 56);
        return i;
	}
	
	public void writeShort(short s) {
		writeByte((byte) (s & 255));
		s >>>= 8;
		writeByte((byte) (s & 255));
		s >>>= 8;
	}
	
	public void writeShort(int s) {
		writeShort((short) s);
	}
	
	public short readShort() {
        short s = (short) (readByte() & 255);
        s |= ((readByte() & 255) << 8);
        return s;
	}
	
	public void writeDouble(Double d) {
		String sD = Double.toHexString(d);
		writeString(sD);
	}
	
	public double readDouble() {
		return Double.valueOf(readString());
	}
	
	public void writeFloat(Float f)
	{
		String sF = Float.toHexString(f);
		writeString(sF);
	}
	
	public float readFloat()
	{
		return Float.valueOf(readString());
	}
	
	public void writeBoolean(boolean bool) {
		if(bool) writeByte(0);
		else writeByte(1);
	}
	
	public boolean readBoolean() {
		byte b = (byte) readByte();
		return b == 0;
	}
	
	public void writeString(String s) {
		if(s == null) writeInt(0);
		else {
			char[] chars = s.toCharArray();
			writeInt(chars.length);
			for(int i = 0;i < chars.length;i++) {
				writeByte((byte) chars[i]);
			}
		}
	}
	
	public String readString() {
		String s = "";
		int length = readInt();
		for(int i = 0;i < length;i++) {
			s += String.valueOf((char) readByte());
		}
		return s;
	}
	
	public void writeIntegerArray(int[] ai) {
		writeInt(ai.length);
		for(int i = 0;i < ai.length;i++) {
			writeInt(ai[i]);
		}
	}
	
	public int[] readIntegerArray() {
		int[] ai = new int[readInt()];
		for(int i = 0;i < ai.length;i++) {
			ai[i] = readInt();
		}
		return ai;
	}
	
	public void writeIntegerVector(Vector<Integer> vector) {
		writeInt(vector.size());
		for(int i = 0;i < vector.size();i++) {
			writeInt(vector.get(i));
		}
	}
	
	public Vector<Integer> readIntegerVector() {
		Vector<Integer> vector = new Vector<Integer>();
		int length = readInt();
		for(int i = 0;i < length;i++) {
			vector.add(readInt());
		}
		return vector;
	}
	
	public void writeStringVector(Vector<String> vector) {
		writeInt(vector.size());
		for(int i = 0;i < vector.size();i++) {
			writeString(vector.get(i));
		}
	}
	
	public Vector<String> readStringVector() {
		Vector<String> vector = new Vector<String>();
		int length = readInt();
		for(int i = 0;i < length;i++) {
			vector.add(readString());
		}
		return vector;
	}
	
	public void writeStringArray(String as[]) {
		writeInt(as.length);
		for(int i = 0;i < as.length;i++) {
			writeString(as[i]);
		}
	}
	
	public String[] readStringArray() {
		String[] as = new String[readInt()];
		for(int i = 0;i < as.length;i++) {
			as[i] = readString();
		}
		return as;
	}
	
	public Object readEnum (Class<? extends Enum> e) {
		int value = readInt();
		return e.getEnumConstants()[value];
	}
	
	public void writeEnum(Enum<?> e) {
		writeInt(e.ordinal());
	}
	
	public byte[] getBuffer() {
		byte[] aBuff = new byte[buffer.size()];
		
		for(int i = 0;i < buffer.size();i++)
		{
			aBuff[i] = buffer.get(i);
		}
		
		return aBuff;
	}
	
	// [DEBUG]
	static int count = 0;
	
	public void prepareBuffer() {
		ArrayList<Byte> buff = new ArrayList<Byte>(buffer.size() + 8);

		
		Buffer size = new Buffer();
		size.writeInt(buffer.size());
		size.writeInt(count++);
		
		byte[] bSize = size.getBuffer();
		
		for(int i = 0;i < 8;i++) {
			buff.add(bSize[i]);
		}
		
		//byte[] buff = new byte[buffer.size() + 8];
		for(int i = 0;i < buffer.size();i++) {
			buff.add(buffer.get(i));
		}
		
		buffer = buff;
	}

	public void writeBuffer(OutputStream os) throws IOException
	{
		Buffer buffer = new Buffer(getBuffer());
		
		buffer.prepareBuffer();
		//Tools.dbgMsg("Sending: " + (count - 1) + " - " + Tools.byteArrayToHexString(buffer.getBuffer()));

		byte[] a = buffer.getBuffer();
		os.write(a);
		os.flush();
		
		m_lTotalOut += a.length;
	}
	
	public static Buffer readBuffer(InputStream is) throws IOException
	{
		
		byte[] size = new byte[8];
		
		for(int i = 0;i < 8;i++)
		{
			int in = is.read();
			if(in == -1)
			{
				return null;
			}
			
			size[i] = (byte) in;
		}
		
		Buffer bSize = new Buffer(size);
		int len = bSize.readInt();
		
//		if(len > 1000)
//		{
//			Tools.dbgMsg("Oversize :( - " + len);
//			System.exit(0);
//		}
		
		byte[] data = new byte[len];
		
		for(int i = 0;i < len;i++) {
			data[i] = (byte) is.read();
		}

		//Tools.dbgMsg("Received: " + bSize.readInt() + ":" + len + " - " + Tools.byteArrayToHexString(size) + Tools.byteArrayToHexString(data));
		
		Buffer buffer = new Buffer(data);
		
		m_lTotalIn += len + 8;
		
		return buffer;
	}
	
	public OutputStream getOutputStream()
	{
		return new OutputStream() {

			@Override
			public void write(int arg0) throws IOException {
				writeByte(arg0);
			}
			
		};
	}
	
	public InputStream getInputStream()
	{
		return new InputStream() {

			@Override
			public int read() throws IOException {
				return readByte();
			}
			
		};
	}
	
	public static long[] getTotalUsage()
	{
		return new long[] {m_lTotalIn, m_lTotalOut};
	}
	
	public String toString()
	{
		return "[Buffer; size: " + buffer.size() + "; writePosition: " + writePosition + "; readPos: " + readPosition + "]";
	}
}
