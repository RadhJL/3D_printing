// version 420

uniform mat4 u_mvpMatrix;
in vec3 in_Position;

void main()
{
	gl_Position = u_mvpMatrix * vec4(in_Position, 1);
	
}
