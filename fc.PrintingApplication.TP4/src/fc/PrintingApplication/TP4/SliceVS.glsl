// version 420
uniform mat4 u_mvpMatrix;
in vec3 in_Position;

uniform mat4 u_inv_mvpMatrix;
uniform float width;
uniform float height;

void main()
{
	gl_Position = u_mvpMatrix * vec4(in_Position, 1.0);
	
}
