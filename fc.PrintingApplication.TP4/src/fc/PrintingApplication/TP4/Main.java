package fc.PrintingApplication.TP4;

import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL20.glBindAttribLocation;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.owens.oobjloader.builder.Build;
import com.owens.oobjloader.builder.Face;
import com.owens.oobjloader.builder.FaceVertex;
import com.owens.oobjloader.builder.VertexGeometric;
import com.owens.oobjloader.parser.Parse;

import fc.GLObjects.GLProgram;
import fc.GLObjects.GLShaderMatrixParameter;
import fc.Math.AABB;
import fc.Math.Matrix;
import fc.Math.Vec3f;

public class Main
{
	static public String model_name = "CuteOcto";// Cubewithhole, CuteOcto, giraffe, moai, yoda, cuber_v1, tour
													// wrench, makerbot, 5mm_Calibration_Steps, 20mm_calibration_cube,
													//benchy
	static public AABB model_aabb = new AABB();
	static public int WIDTH;
	static public int HEIGHT;
	static public float step = 0.2f;
	static public float px_size = 0.05f;
	public static float nozzle_diameter = 0.4f;
	public static boolean with_contours = true; //
	public static boolean with_smoothing = false; // smothing is bugging
	public static boolean generate_layers_images = true; // turn it on to see layer's depth & color (frontfacing => red or backfacing => blue) 	
	public static void main(String[] args) throws IOException
	{

		glfwInit();
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		long window = glfwCreateWindow(100, 100, "Dummy", NULL, NULL);
		glfwMakeContextCurrent(window);
		GL.createCapabilities();

		ArrayList<Triangle> model_faces = new ArrayList<Triangle>();
		
		parseObjFile("3d_models\\"+ model_name +".obj", model_aabb, model_faces);
		System.out.println("Model name: "+ model_name);
		System.out.println("Faces size: "+ model_faces.size());
		
		enlargeTenPercentXY(model_aabb);
		System.out.println("Model aabb => ");
		System.out.println("	Min:" + "x: "+model_aabb.getMin().x + " y: "+ model_aabb.getMin().y + " z: "+ model_aabb.getMin().z);
		System.out.println("	Max:" + "x: "+model_aabb.getMax().x + " y: "+ model_aabb.getMax().y + " z: "+ model_aabb.getMax().z);
		
		updateWidthHeight(model_aabb);
		System.out.println("Pixel size: "+ px_size +"mm");
		System.out.println("Nozzle diameter: "+ nozzle_diameter +"mm");
		System.out.println("Image width: "+ WIDTH + " height: " + HEIGHT);

		DepthPeeling(model_faces);
	}

	//_____________________________________________________________________________________________________________________________________________________________________
	public static void DepthPeeling(ArrayList<Triangle> model_faces){

		System.out.println("Begin Depth Peeling");
		if(with_contours){
			System.out.println("Generate Contours: ON");
			if(with_smoothing){
				System.out.println("Smoothing contours: ON");
			}else{
				System.out.println("Smoothing contours: OFF");
			}
		}else{
			System.out.println("Generate Contours: OFF");
		}
		
		int nb_layers = -1;
		GLTriangles triangles = new GLTriangles(model_faces);
		
		//--------------------------------------------------------------------------------------------------------------------------------------------------
	    // Compute number of layers
		
		GLRenderTarget stencil_tex = new GLRenderTarget(WIDTH, HEIGHT, GL30.GL_RGBA32I, GL30.GL_RGBA_INTEGER, GL11.GL_INT);
		GLProgram stencil_shader = new GLProgram()
		{
			@Override
			protected void preLinkStep()
			{
				glBindAttribLocation(m_ProgramId, 0, "in_Position");
			}
		};

		stencil_shader.init(new MyVShader(), new MyFShader());
		GLShaderMatrixParameter matParam = new GLShaderMatrixParameter("u_mvpMatrix");
		matParam.init(stencil_shader);

		GL11.glViewport(0, 0, WIDTH, HEIGHT);
		GL30.glDisable(GL30.GL_DEPTH_TEST);
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL30.glEnable(GL30.GL_STENCIL_TEST);
		stencil_tex.bind();
			glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL30.GL_STENCIL_BUFFER_BIT);
			stencil_shader.begin();
				GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 0);
				GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_INCR, GL11.GL_INCR);
				updateOrhtoMatrix(matParam, model_aabb);
				triangles.render();
				stencil_shader.end();
		stencil_tex.unbind();
	
		nb_layers = computeNbLayers(stencil_tex);
		System.out.println("Number of layers: "+ nb_layers);
		// saveStencilFromTexture(stencil_tex, WIDTH, HEIGHT, "depth_peeling", "stencil");
		
		//--------------------------------------------------------------------------------------------------------------------------------------------------
		// Extract layers
		
		GLRenderTarget curr_depth_tex = new GLRenderTarget(WIDTH, HEIGHT, GL30.GL_RGBA32I, GL30.GL_RGBA_INTEGER, GL11.GL_INT);
		ArrayList<GLRenderTarget> layers_depth_tex = new ArrayList<GLRenderTarget>();
		for(int layer_id = 0; layer_id < nb_layers; layer_id++)
			layers_depth_tex.add(new GLRenderTarget(WIDTH, HEIGHT, GL30.GL_RGBA32I, GL30.GL_RGBA_INTEGER, GL11.GL_INT));

		
		GLProgram layer_shader = new GLProgram()
		{
			@Override
			protected void preLinkStep()
			{
				glBindAttribLocation(m_ProgramId, 0, "in_Position");
			}
		};
		layer_shader.init(new LayerVS(), new LayerFS());
		matParam.init(layer_shader);
		
		GLShaderMatrixParameter inv_matParam = new GLShaderMatrixParameter("u_inv_mvpMatrix");
		inv_matParam.init(layer_shader);
		
		layer_shader.begin();
			int curr_depth_tex_loc = GL30.glGetUniformLocation(layer_shader.getId(), "curr_depth_tex");
			GL30.glUniform1i(curr_depth_tex_loc, 0);
			
			int width_loc = GL30.glGetUniformLocation(layer_shader.getId(), "width");
			GL30.glUniform1f(width_loc, (float)WIDTH);

			int height_loc = GL30.glGetUniformLocation(layer_shader.getId(), "height");
			GL30.glUniform1f(height_loc, (float)HEIGHT);

			int is_first_layer_loc = GL30.glGetUniformLocation(layer_shader.getId(), "is_first_layer");
			GL30.glUniform1i(is_first_layer_loc, 1);
		layer_shader.end();

		GL30.glDisable(GL30.GL_STENCIL_TEST);
		GL30.glEnable(GL30.GL_DEPTH_TEST);
		GL30.glDepthFunc(GL30.GL_LESS);
		GL11.glDisable(GL11.GL_CULL_FACE);
		for(int layer_id = 0; layer_id < nb_layers; layer_id++){
			System.out.println("layer: "+ (layer_id + 1) + " / "+  nb_layers);
			layers_depth_tex.get(layer_id).bind();
				glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
				GL30.glActiveTexture(GL30.GL_TEXTURE0);
				GL11.glBindTexture(GL11.GL_TEXTURE_2D, curr_depth_tex.getDepthTexId());
				layer_shader.begin();
					if(layer_id > 0)
						GL30.glUniform1i(is_first_layer_loc, 0);
					updateOrhtoMatrix(matParam, model_aabb);
					getinvOrhtoMatrix(inv_matParam, model_aabb);
					triangles.render();
				layer_shader.end();
			layers_depth_tex.get(layer_id).unbind();
			curr_depth_tex = layers_depth_tex.get(layer_id);
			if(generate_layers_images){
				saveImageFromTexture(layers_depth_tex.get(layer_id), WIDTH, HEIGHT, "depth_peeling", "layer"+ String.format("%02d" , layer_id + 1), false, false);
				saveDepthFromTexture(layers_depth_tex.get(layer_id), WIDTH, HEIGHT, "depth_peeling", "layer"+ String.format("%02d" , layer_id + 1)+"_depth");
			}
		}
	
	//--------------------------------------------------------------------------------------------------------------------------------------------------
	// Generate slices

		GLProgram slice_shader = new GLProgram()
		{
			@Override
			protected void preLinkStep()
			{
				glBindAttribLocation(m_ProgramId, 0, "in_Position");
			}
		};
		slice_shader.init(new SliceVS(), new SliceFS());
		matParam.init(slice_shader);
		inv_matParam.init(slice_shader);

		slice_shader.begin();
			int layer0_depth_tex_loc = GL30.glGetUniformLocation(slice_shader.getId(), "layer0_depth_tex");
			GL30.glUniform1i(layer0_depth_tex_loc, 0);
			
			int layer1_depth_tex_loc = GL30.glGetUniformLocation(slice_shader.getId(), "layer1_depth_tex");
			GL30.glUniform1i(layer1_depth_tex_loc, 1);
			
			width_loc = GL30.glGetUniformLocation(slice_shader.getId(), "width");
			GL30.glUniform1f(width_loc, (float)WIDTH);

			height_loc = GL30.glGetUniformLocation(slice_shader.getId(), "height");
			GL30.glUniform1f(height_loc, (float)HEIGHT);

			int layer0_front_loc = GL30.glGetUniformLocation(slice_shader.getId(), "layer0_front");
			int layer1_front_loc = GL30.glGetUniformLocation(slice_shader.getId(), "layer1_front");

			Vec3f min = model_aabb.getMin();
			Vec3f max = model_aabb.getMax();
			float zn = min.z;
			float zf = max.z;
			
			int zn_loc = GL30.glGetUniformLocation(slice_shader.getId(), "zn");
			GL30.glUniform1f(zn_loc, zn);
			
			int zf_loc = GL30.glGetUniformLocation(slice_shader.getId(), "zf");
			GL30.glUniform1f(zf_loc, zf);

			int max_dist_loc = GL30.glGetUniformLocation(slice_shader.getId(), "max_dist");
			GL30.glUniform1f(max_dist_loc, (float)(zf - zn));

		slice_shader.end();

		float x_min = model_aabb.getMin().x;
		float x_max = model_aabb.getMax().x;
		float y_min = model_aabb.getMin().y;
		float y_max = model_aabb.getMax().y;
		
		GL30.glDisable(GL30.GL_STENCIL_TEST);
		GL30.glEnable(GL30.GL_DEPTH_TEST);
		GL30.glDepthFunc(GL30.GL_LESS);
		GL11.glDisable(GL11.GL_CULL_FACE);
		int slice_id = 1;
		int nb_slice =   (int) ((Math.abs(model_aabb.getMax().z) - model_aabb.getMin().z)/step) + 1;
		for(float z = model_aabb.getMin().z; z <= model_aabb.getMax().z; z += step){
			System.out.println("Slice: "+ slice_id + " / " + nb_slice);
			GLRenderTarget slice_tex = new GLRenderTarget(WIDTH, HEIGHT, GL30.GL_RGBA32I, GL30.GL_RGBA_INTEGER, GL11.GL_INT);
			slice_tex.bind();
				glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			slice_tex.unbind();
			for(int layer_id = 0; layer_id < nb_layers - 1; layer_id++){
				slice_tex.bind();
					GL30.glActiveTexture(GL30.GL_TEXTURE0);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, layers_depth_tex.get(layer_id).getDepthTexId());
					GL30.glActiveTexture(GL30.GL_TEXTURE1);
					GL11.glBindTexture(GL11.GL_TEXTURE_2D, layers_depth_tex.get(layer_id + 1).getDepthTexId());
					slice_shader.begin();
						GL30.glUniform1i(layer0_front_loc, layer_id % 2 == 0 ? 0 : 1);
						GL30.glUniform1i(layer1_front_loc, ((layer_id + 1) % 2) == 0 ? 0 : 1);
						
						updateOrhtoMatrix(matParam, model_aabb);
						getinvOrhtoMatrix(inv_matParam, model_aabb);
						GLVec3fTriangle tri0 = new GLVec3fTriangle(new Vec3f[] {new Vec3f(x_min, y_min, z), new Vec3f(x_max, y_max, z), new Vec3f(x_min, y_max, z)});
						GLVec3fTriangle tri1 = new GLVec3fTriangle(new Vec3f[] {new Vec3f(x_min, y_min, z), new Vec3f(x_max, y_min, z), new Vec3f(x_max, y_max, z)});
						tri0.render();
						tri1.render();
					slice_shader.end();
				slice_tex.unbind();
			}
			saveImageFromTexture(slice_tex, WIDTH, HEIGHT, "depth_peeling", "tranche" +  String.format("%03d" , slice_id++), with_contours, with_smoothing);
			slice_tex.dispose();
		}
	}

	//_____________________________________________________________________________________________________________________________________________________________________
	// Functions to make code cleaner
	static public void enlargeTenPercentXY(AABB aabb){
		aabb.getMax().x = aabb.getMax().x + (float) Math.abs(aabb.getMax().x * (1.0f / 10.0));
		aabb.getMax().y = aabb.getMax().y + (float) Math.abs(aabb.getMax().y * (1.0f / 10.0));
		aabb.getMin().x = aabb.getMin().x - (float) Math.abs(aabb.getMin().x * (1.0f / 10.0));
		aabb.getMin().y = aabb.getMin().y - (float) Math.abs(aabb.getMin().y * (1.0f / 10.0));
	}

	static public int computeNbLayers(GLRenderTarget rt){
		int max_nb_layers = -1; 
		int[][] stencil = rt.readBackStencil(0, 0, WIDTH, HEIGHT);
		
		for (int y = 0; y < HEIGHT; y++)
			for (int x = 0; x < WIDTH; x++)
				if(stencil[y][x] > max_nb_layers)
					max_nb_layers = stencil[y][x];

		return max_nb_layers;
	}

	static public void updateWidthHeight(AABB aabb){
		HEIGHT = (int)(Math.ceil(aabb.getMax().y - aabb.getMin().y) / px_size);
		WIDTH = (int)(Math.ceil(aabb.getMax().x - aabb.getMin().x) / px_size);
	}

	static public void updateOrhtoMatrix(GLShaderMatrixParameter matParam, AABB aabb){
		Vec3f min = aabb.getMin();
		Vec3f max = aabb.getMax();
		float l = min.x;
		float r = max.x;
		float b = min.y;
		float t = max.y;
		float zn = min.z;
		float zf = max.z;
		matParam.set(Matrix.createOrtho(l, r, b, t, zn, zf));
	}
	
	static public void getinvOrhtoMatrix(GLShaderMatrixParameter matParam, AABB aabb){
		Vec3f min = aabb.getMin();
		Vec3f max = aabb.getMax();
		float l = min.x;
		float r = max.x;
		float b = min.y;
		float t = max.y;
		float zn = min.z;
		float zf = max.z;
		matParam.set(Matrix.createOrtho(l, r, b, t, zn, zf).invert());
	}

	//_____________________________________________________________________________________________________________________________________________________________________
	// Functions to saves different type of textures as image
	public static void saveImageFromTexture(GLRenderTarget texture, int width, int height, String folder_name, String  image_name, boolean with_contours, boolean with_smoothing){
		float[][][] pixels = texture.readBackAsFloat();
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		for (int y=0; y < height; y++)
			for (int x=0; x < width; x++){
				int r = (int)(pixels[y][x][0] * 255.0f);
				int g = (int)(pixels[y][x][1] * 255.0f);
				int b = (int)(pixels[y][x][2] * 255.0f);
				img.setRGB(x, y, (r<<16) | (g<<8) | b);
			}

		if(with_contours)
			ToolpathGeneration.createContours(img, nozzle_diameter, px_size, with_smoothing);

		File outputFile = new File(System.getProperty("user.dir")+"\\"+folder_name, image_name+".png");
		try{
			ImageIO.write(img, "png", outputFile);
		}
		catch (IOException e){
			System.out.println("Error, IOException caught: " + e.toString());
		}
	}

	public static void saveStencilFromTexture(GLRenderTarget texture, int width, int height, String folder_name, String  image_name){
		int[][] pixels = texture.readBackStencil(0, 0, width, height);
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		for (int y=0; y < pixels.length; y++)
			for (int x=0; x < pixels[0].length; x++){
				int r = (pixels[y][x] ) * 10;
				int g = (pixels[y][x] ) * 10;
				int b = (pixels[y][x] ) * 10;
				img.setRGB(x, y, (r<<16) | (g<<8) | b);
			}

		File outputFile = new File(System.getProperty("user.dir")+"\\"+folder_name, image_name+".png");
		try{
			ImageIO.write(img, "png", outputFile);
		}
		catch (IOException e){
			System.out.println("Error, IOException caught: " + e.toString());
		}
	}

	public static void saveDepthFromTexture(GLRenderTarget texture, int width, int height, String folder_name, String  image_name){
		float[][] pixels = texture.readBackDepth(0, 0, width, height);
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int y=0; y < height; y++)
			for (int x=0; x < width; x++){
				int r = (int)(pixels[y][x] * 255.0f);
					img.setRGB(x, y, (r<<16) | (r<<8) | r);
			}

		File outputFile = new File(System.getProperty("user.dir")+"\\"+folder_name, image_name+".png");
		try{
			ImageIO.write(img, "png", outputFile);
		}
		catch (IOException e){
			System.out.println("Error, IOException caught: " + e.toString());
		}
	}

	//_____________________________________________________________________________________________________________________________________________________________________
	static public void parseObjFile(String filename, AABB aabb, ArrayList<Triangle> faces)
	{
	    try
	    {
	        Build builder = new Build();
	        Parse obj = new Parse(builder, new File(filename).toURI().toURL());
	        
	        // Enumeration des sommets
	        
	        for (FaceVertex vertex : builder.faceVerticeList)
	        {
	        	float x = vertex.v.x;
		    	float y = vertex.v.y;
	        	float z = vertex.v.z;
	        	aabb.enlarge(new Vec3f(x, y, z));
	        	// ...
	        }

			float zmin = aabb.getMin().z;
			float zmax = aabb.getMax().z;
			float length = (float) Math.sqrt((zmax - zmin) * (zmax - zmin)) / 2.0f;
			float translate_z = 0;

			if(length != Math.abs(aabb.getMax().z) || length != Math.abs(aabb.getMin().z)){
				translate_z = -length - zmin;
				aabb.getMin().z = -length - 0.0001f; // for rounding float errors
				aabb.getMax().z =  length + 0.0001f;
			}
			// Enumeration des faces (souvent des triangles, mais peuvent comporter plus de sommets dans certains cas)
	        
	        for (Face face : builder.faces)
	        {
	        	// Parcours des triangles de cette face
	        	for (int i=1; i <= (face.vertices.size() - 2); i++)
	        	{
	        		int vertexIndex1 = face.vertices.get(0).index;
	        		int vertexIndex2 = face.vertices.get(i).index;
	        		int vertexIndex3 = face.vertices.get(i+1).index;
	        		
	        		VertexGeometric vertex1 = builder.faceVerticeList.get(vertexIndex1).v;
	        		VertexGeometric vertex2 = builder.faceVerticeList.get(vertexIndex2).v;
					VertexGeometric vertex3 = builder.faceVerticeList.get(vertexIndex3).v;
					Vec3f a = new Vec3f(vertex1.x, vertex1.y, vertex1.z + translate_z);
					Vec3f b = new Vec3f(vertex2.x, vertex2.y, vertex2.z + translate_z);
					Vec3f c = new Vec3f(vertex3.x, vertex3.y, vertex3.z + translate_z);
					Triangle tmp_t = new Triangle(a, b, c);
	        		faces.add(tmp_t);
	        		// .. .
	        	}
	        }
	    }
	    catch (java.io.FileNotFoundException e)
	    {
	    	System.out.println("FileNotFoundException loading file "+filename+", e=" + e);
	        e.printStackTrace();
	    }
	    catch (java.io.IOException e)
	    {
	    	System.out.println("IOException loading file "+filename+", e=" + e);
	        e.printStackTrace();
	    }
	}

	public static void checkGLErrorState()
	{
		int err = GL11.glGetError();
		if (err != 0)
			throw new IllegalStateException("OpenGL is in error state " + err);
	}
	
}
