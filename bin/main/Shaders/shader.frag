#version 330 core

in vec3 fragNormal;
in vec3 fragPos;

uniform vec3 objectColor;

out vec4 FragColor;

void main() {
    vec3 lightDir = normalize(vec3(1.0, 1.0, 0.5));
    vec3 norm = normalize(fragNormal);

    float ambient = 0.3;

    float diff = max(dot(norm, lightDir), 0.0);

    vec3 result = (ambient + diff * 0.7) * objectColor;
    FragColor = vec4(result, 1.0);
}
