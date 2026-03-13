package fc.Serialization.Buffer;

import java.util.ArrayList;

public class ArraySerializationBuffer extends SerializationBuffer
{
	private int m_Position; // in byte units
	public int m_MaxPosition; // in byte units
	public ArrayList<Integer> m_Values;
	
	public ArraySerializationBuffer(Object object)
	{
		super(object);
	}

	@Override
	protected void createBuffer()
	{
		m_Position = 0;
		m_MaxPosition = 0;
		m_Values = new ArrayList<>();
	}
	
	@Override
	protected int getBufferSizeInBytes()
	{
		return m_Values.size()*4;
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
		int val = m_Values.get(m_Position / 4);
		m_Position += 4;
		m_MaxPosition = Math.max(m_Position, m_MaxPosition);
		return val;
	}
	
	@Override
	protected void putInt(int value)
	{
		int arrayIndex = m_Position / 4;
		if (arrayIndex < m_Values.size())
			m_Values.set(arrayIndex, value);
		else if (arrayIndex == m_Values.size())
			m_Values.add(value);
		m_Position += 4;
		m_MaxPosition = Math.max(m_Position, m_MaxPosition);
	}

	@Override
	protected void map(int startPositionInBytes, int endPositionInBytes, boolean mapForReading, boolean mapForWriting)
	{
		m_Position = startPositionInBytes;
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
