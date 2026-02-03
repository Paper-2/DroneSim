
#version 330 core

layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;

uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform mat4 modelMatrix;

out vec3 fragNormal;
out vec3 fragPos;

void main() {
    vec4 worldPos = modelMatrix * vec4(aPos, 1.0);
    fragPos = worldPos.xyz;
    fragNormal = mat3(transpose(inverse(modelMatrix))) * aNormal;
    gl_Position = projectionMatrix * viewMatrix * worldPos;
}
