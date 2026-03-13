package fc.PrintingApplication.TP4;

import java.util.ArrayList;
import java.awt.image.BufferedImage;
import fc.Math.Vec2i;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.*;


public class ToolpathGeneration {
	static Vec2i path_moves[] = {new Vec2i(0, -1), 
								new Vec2i(1, -1), new Vec2i(1, 0), 
								new Vec2i(1, 1), new Vec2i(0, 1),
								 new Vec2i(-1, 1), new Vec2i(-1, 0),
								new Vec2i(-1, -1)}; 
    
	public static void drawPaths(BufferedImage image, ArrayList<ArrayList<Vec2i>> paths){
		Graphics2D g = image.createGraphics();
		g.setColor(new Color(0, 0, 255));
		g.setStroke(new BasicStroke(1));
		for(ArrayList<Vec2i> path: paths){
			for(int i = 0; i < path.size() - 1 ; i++){
				Vec2i p0 = path.get(i);
				Vec2i p1 = path.get(i + 1);
				g.drawLine(p0.x, p0.y, p1.x, p1.y);
			}
		}
	}

	public static boolean[][] convertImageToArray(BufferedImage image, int width, int height){// image to material / non material array
		boolean[][] material = new boolean[height][width]; 
		for(int y = 0; y < height; y++){
			for(int x = 0; x < width; x++){
				if(((image.getRGB(x, y) & 0x00ff0000) >> 16) > 0)
					material[y][x] = true;
				else
					material[y][x] = false;
			}
		}
		return material;
	}

	public static boolean Erosion(boolean[][] material, int erosion_diameter){ 
		
		int rad = (int)(erosion_diameter / 2.0f) + 1;
		if(rad == 1){
			System.out.println("Nozzle diameter is too small to do discrete erosion");
			return false;
		}
		
		boolean[][] tmp_material = new boolean[material.length][material[0].length];
		for(int y = 0; y < material.length; y++){
			for(int x = 0; x < material[0].length; x++){
				tmp_material[y][x] = material[y][x];
			}	
		}
		
		boolean mat_exist = false;
		for(int y = 0; y < material.length; y++){
				for(int x = 0; x < material[0].length; x++){
						if(material[y][x]){
							outerloop:
							for(int r = 1; r < rad; r++){
								for(int i = 0; i < path_moves.length; i++){
									Vec2i px = new Vec2i(x, y).add(new Vec2i(path_moves[i].x * r, path_moves[i].y*r));
									if(isInside(px, material) && !tmp_material[px.y][px.x]){
										material[y][x] = false;
										break outerloop;
									}

								}
							}
						}
					if(material[y][x]){
						mat_exist = true;
					}
			}
		}

		return mat_exist;
	}	

	public static void smoothPathandDraw(int left, int right, ArrayList<Vec2i> path, BufferedImage image){
		//Line equation (y0 − y1)x + (x1 − x0)y + x0y1 − x1y0 = 0.
		//Ax +         By +           C = 0 
		if(right <= left)
			return;
		int x0 = path.get(left).x;
		int y0 = path.get(left).y;
		int x1 = path.get(right).x;
		int y1 = path.get(right).y;
		int A = y0 - y1;
		int B = x1 - x0;
		int C = x0 * y1 - x1 * y0;// not sure		
		float bottom_eq = (float)Math.sqrt(A * A + B * B);
		float d_max = -1.0f;
		int max_id = left + 1;
		for(int i = left + 1; i < right; i++){
			Vec2i point = path.get(i);
			float d = Math.abs(A * point.x + B * point.y + C) / bottom_eq;
			if(d > d_max){
				d_max = d;
				max_id = i;
			} 
		}

		if(d_max <= Math.sqrt(2)){
			Graphics2D g = image.createGraphics();
			g.setColor(new Color(0, 0, 255));
			g.setStroke(new BasicStroke(1));
			Vec2i p0 = path.get(left);
			Vec2i p1 = path.get(right);
			g.drawLine(p0.x, p0.y, p1.x, p1.y);

		}else{
			smoothPathandDraw(left, max_id, path, image);
			smoothPathandDraw(max_id, right, path, image);
		}
		
	}

	public static boolean haveEmptyNeighbor(Vec2i pixel, boolean material[][]){
		for(int i = 0; i < path_moves.length; i++){
			Vec2i neighbor = pixel.add(path_moves[i]);
			if(neighbor.x >= 0 && neighbor.x < material[0].length && neighbor.y >= 0 && neighbor.y < material.length &&
			!material[neighbor.y][neighbor.x])
				return true;
		}
		return false;
	} 

	static boolean isInside(Vec2i pixel, boolean[][] mat){
		int width = mat[0].length;
		int height = mat.length;
		if(pixel.x >= 0 && pixel.x < width && pixel.y >= 0 && pixel.y < height)
			return true;
		return false;
	}

	public static void createPathRecu(Vec2i current, Vec2i ancestor, boolean[][] material, boolean[][] visited, ArrayList<Vec2i> path){
		visited[current.y][current.x] = true;
		path.add(current);

		if(current.equals(ancestor) && path.size() > 1)
			return;
		
		for(int i = 0; i < path_moves.length; i++){
			Vec2i next = current.add(path_moves[i]);
			if(!isInside(next, material))
				continue;
			if(material[next.y][next.x] && !visited[next.y][next.x] && haveEmptyNeighbor(next, material)){
				createPathRecu(next, ancestor, material, visited, path);
				break;
			}
		}
	}

	public static void createContours(BufferedImage img, float nozzle_diameter, float px_size, boolean with_smoothing){
			
		int width = img.getWidth();
		int height = img.getHeight();
		
		boolean material[][] = convertImageToArray(img, width, height); 
		ArrayList<ArrayList<Vec2i>> paths = new ArrayList<ArrayList<Vec2i>>();
		int erosion_diameter = (int) Math.ceil(((nozzle_diameter) / 2.0f) / px_size);
		
		while(Erosion(material, erosion_diameter)){ 
			boolean contours [][] = new boolean[height][width];
			boolean flag = true;
			while(flag){
				flag = false;
				outerloop:
					for(int x = 0; x < width; x++){
						for(int y = 0; y < height; y++){
							if(material[y][x] && haveEmptyNeighbor(new Vec2i(x, y), material) && !contours[y][x]){
								ArrayList<Vec2i> path = new ArrayList<Vec2i>();
								// createPath(new Vec2i(x, y), new Vec2i(x, y), material, contours, new ArrayList<Move> (), path);	
								createPathRecu(new Vec2i(x, y), new Vec2i(x, y), material, contours, path);
								flag = true;
								paths.add(path);
								break outerloop;
							}
						}	
					}

			}
		}

	if(with_smoothing){
		for(int i = 0; i < paths.size(); i++)
			smoothPathandDraw(0, paths.get(i).size() - 1, paths.get(i), img);
	}else{
		drawPaths(img, paths);
	}
	
	}
}
