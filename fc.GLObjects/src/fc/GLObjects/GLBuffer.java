// Copyright (c) 2016,2017 Frédéric Claux, Université de Limoges. Tous droits réservés.

package fc.GLObjects;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL31;

public class GLBuffer 
{
	private static int BUFFER_TARGET = GL31.GL_TEXTURE_BUFFER;
	
	protected int m_Id;
	protected long m_Size;
	
	public GLBuffer(long size)
	{
		m_Id = GL15.glGenBuffers();
		GLError.check("Could not generate buffer ID");
		resize(size);
	}
	
	public void resize(long size)
	{
		m_Size = size;
		GL15.glBindBuffer(BUFFER_TARGET, m_Id);
		GL15.glBufferData(BUFFER_TARGET, size, GL15.GL_STATIC_DRAW); // STATIC_DRAW uses a LOT less memory than DYNAMIC_DRAW {FC:2018-01-18}
		GL15.glBindBuffer(BUFFER_TARGET, 0);
	}
	
	public int getId()
	{
		return m_Id;
	}
	
	public long getSizeInBytes()
	{
		return m_Size;
	}
	
	public void dispose()
	{
		GL15.glBindBuffer(BUFFER_TARGET, 0);
		GL15.glDeleteBuffers(m_Id);
		m_Id = -1;
	}
}
