package fc.Serialization.Buffer;

import java.util.ArrayList;

public class BufferLocation
{
	public int m_StartPosition; // byte units
	public int m_EndPosition; // byte units
	public ArrayList<Integer> m_References = new ArrayList<>();
	
	public void addReference(int position)
	{
		m_References.add(position);
	}
}
