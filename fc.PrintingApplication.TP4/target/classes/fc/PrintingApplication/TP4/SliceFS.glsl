// version 420

uniform sampler2D layer0_depth_tex;
uniform sampler2D layer1_depth_tex;
uniform bool layer0_front;
uniform bool layer1_front;
uniform float width;
uniform float height;
uniform mat4 u_inv_mvpMatrix;
uniform float zn;
uniform float zf;
uniform float max_dist;
out vec4 out_FragColor;

float getColorFromDistance(float dist){ 
	float slope = (255.0 - 64.0) / (max_dist);  
	float o = 64.0 + slope * (dist);
	float o_normalized = o / 255.0;
	return o_normalized;
}

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
	float layer0_depth = texture(layer0_depth_tex, frag_coord).r;
	float layer1_depth = texture(layer1_depth_tex, frag_coord).r;
	float frag_depth = gl_FragCoord.z;

	if( frag_depth >= layer0_depth && frag_depth <= layer1_depth){
		if(!layer0_front && layer1_front){
			float real_frag_depth = getWorldDepth(frag_depth);
			float real_layer0_depth = getWorldDepth(layer0_depth);
			float dist = sqrt((real_frag_depth - real_layer0_depth) * (real_frag_depth - real_layer0_depth));
			float r = getColorFromDistance(dist);
		 	out_FragColor = vec4(r, 0, 0, 1); 
		}
		else{
			discard;
		}
	}else{
		discard;
	}

}
