// version 420

uniform sampler2D curr_depth_tex;
uniform float width;
uniform float height;
uniform bool is_first_layer;

out vec4 out_FragColor;
uniform mat4 u_inv_mvpMatrix;

// https://stackoverflow.com/questions/38938498/how-do-i-convert-gl-fragcoord-to-a-world-space-point-in-a-fragment-shader
float getWorldDepth(float z_w){
	vec4 ndcPos;
	vec4 viewport = vec4(0, 0, width, height);
	ndcPos.xy = ((2.0 * gl_FragCoord.xy) - (2.0 * viewport.xy)) / (viewport.zw) - 1;
	ndcPos.z = (2.0 * z_w - gl_DepthRange.near - gl_DepthRange.far) /
		(gl_DepthRange.far - gl_DepthRange.near);
	
	ndcPos.w = 1.0;
	vec4 clipPos = ndcPos / gl_FragCoord.w;
	vec4 eyePos = u_inv_mvpMatrix * clipPos;
	return eyePos.z;
}

void main()
{
	vec2 frag_coord = vec2(gl_FragCoord.x / width, gl_FragCoord.y / height);
	float curr_depth = getWorldDepth(texture(curr_depth_tex, frag_coord).r);
	float frag_depth = getWorldDepth(gl_FragCoord.z);
	
	if(!is_first_layer && frag_depth >= curr_depth - 0.00001)
		discard;
    
	vec4 color = vec4(0.9, 0.0, 0.0, 1.0);
	
	if(!gl_FrontFacing)
		color = vec4(0.0, 0, 0.9, 1.0);	 
	
	out_FragColor = color;
}
