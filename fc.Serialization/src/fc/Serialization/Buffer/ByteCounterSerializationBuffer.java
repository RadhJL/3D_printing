package fc.Serialization.Buffer;

import java.nio.ByteBuffer;

public class ByteCounterSerializationBuffer extends SerializationBuffer
{
	private int m_Position; // in byte units
	public int m_MaxPosition; // in byte units
	
	public ByteCounterSerializationBuffer(Object object)
	{
		super(object);
	}

	@Override
	protected void createBuffer()
	{
		m_Position = 0;
		m_MaxPosition = 0;
		// Nothing to do, we just count bytes in this implementation
	}
	
	@Override
	protected int getBufferSizeInBytes()
	{
		return 0;
	}
	
	@Override
	protected int getBufferPosition() // in byte units
	{
		return m_Position;
	}
	
	@Override
	protected void setBufferPosition(int position) // in byte units
	{
		m_Position = position;
		m_MaxPosition = Math.max(m_Position, m_MaxPosition);
	}
	
	@Override
	protected int getInt()
	{
		m_Position += 4;
		m_MaxPosition = Math.max(m_Position, m_MaxPosition);
		return 0;
	}
	
	@Override
	protected void putInt(int value)
	{
		m_Position += 4;
		m_MaxPosition = Math.max(m_Position, m_MaxPosition);
	}
	
	@Override
	public void serializeFloatBuffer(ByteBuffer buffer)
	{
		int numBytesUntilEnd = buffer.limit() - buffer.position();
		m_Position += numBytesUntilEnd;
		m_MaxPosition = Math.max(m_Position, m_MaxPosition);
	}
	
	@Override
	public void deserializeFloatBuffer(ByteBuffer buffer, int numFloats)
	{
		m_Position -= numFloats*4;
	}

	@Override
	protected void map(int startPositionInBytes, int endPositionInBytes, boolean mapForReading, boolean mapForWriting)
	{
		// Nothing to do
	}
	
	@Override
	protected void unmap(boolean forWriting)
	{
		// Nothing to do
	}
	
	@Override
	public void skip(int numberOfVec4ToSkip)
	{
		m_Position += numberOfVec4ToSkip*16; // *16 because m_Buffer here is a BYTEBuffer, not IntBuffer
		m_MaxPosition = Math.max(m_Position, m_MaxPosition);
	}
}
