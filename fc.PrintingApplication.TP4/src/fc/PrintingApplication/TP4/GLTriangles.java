package fc.PrintingApplication.TP4;

import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;

public class GLTriangles
{
	public int m_VBO;
	private FloatBuffer m_AttributesBuffer;
    private int size;
	
	public GLTriangles(ArrayList<Triangle> faces) // must be 3 vertices
	{
		m_VBO = glGenBuffers();

        m_AttributesBuffer = BufferUtils.createFloatBuffer(faces.size() * 9);
        size = faces.size() * 3;

        for(Triangle t: faces){
            m_AttributesBuffer.put(t.a.x); m_AttributesBuffer.put(t.a.y); m_AttributesBuffer.put(t.a.z);
            m_AttributesBuffer.put(t.b.x); m_AttributesBuffer.put(t.b.y); m_AttributesBuffer.put(t.b.z);
            m_AttributesBuffer.put(t.c.x); m_AttributesBuffer.put(t.c.y); m_AttributesBuffer.put(t.c.z);
        }
                
		glBindBuffer(GL_ARRAY_BUFFER, m_VBO);
		glBufferData(GL_ARRAY_BUFFER, (FloatBuffer)m_AttributesBuffer.flip(), GL_STATIC_DRAW);
		glEnableVertexAttribArray(0);
	}
	
	public void render()
	{
        glBindBuffer(GL_ARRAY_BUFFER, m_VBO);
            glEnableVertexAttribArray(0);
                glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0L);
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, size);
            glDisableVertexAttribArray(0);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
    }
	
	public void dispose()
	{
		GL15.glDeleteBuffers(m_VBO);
	}
}
