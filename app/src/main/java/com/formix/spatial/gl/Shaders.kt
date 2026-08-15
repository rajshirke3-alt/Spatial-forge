package com.formix.spatial.gl

object Shaders {

    const val VERTEX_SHADER = """
        #version 300 es
        uniform mat4 uMVP;
        uniform mat4 uModel;
        uniform mat3 uNormalMatrix;

        layout(location = 0) in vec3 aPosition;
        layout(location = 1) in vec3 aNormal;

        out vec3 vWorldNormal;
        out vec3 vWorldPos;

        void main() {
            vec4 worldPos = uModel * vec4(aPosition, 1.0);
            vWorldPos = worldPos.xyz;
            vWorldNormal = normalize(uNormalMatrix * aNormal);
            gl_Position = uMVP * vec4(aPosition, 1.0);
        }
    """

    const val FRAGMENT_SHADER = """
        #version 300 es
        precision mediump float;

        in vec3 vWorldNormal;
        in vec3 vWorldPos;
        out vec4 fragColor;

        uniform vec4 uColor;
        uniform vec3 uLightDir;      // directional "sun" light, pointing FROM the light
        uniform vec3 uCameraPos;
        uniform float uSelected;     // 1.0 if this object is selected, else 0.0

        void main() {
            vec3 N = normalize(vWorldNormal);
            vec3 L = normalize(-uLightDir);
            float diff = max(dot(N, L), 0.0);

            vec3 V = normalize(uCameraPos - vWorldPos);
            vec3 H = normalize(L + V);
            float spec = pow(max(dot(N, H), 0.0), 24.0) * 0.15;

            float ambient = 0.32;
            vec3 base = uColor.rgb * (ambient + diff * 0.68) + vec3(spec);

            // Rim highlight for the selected object
            float rim = pow(1.0 - max(dot(N, V), 0.0), 2.5) * uSelected;
            base += vec3(0.31, 0.82, 0.77) * rim * 0.9;

            fragColor = vec4(base, uColor.a);
        }
    """
}
