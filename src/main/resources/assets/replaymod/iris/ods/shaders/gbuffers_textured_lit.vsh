#version 120

varying vec4 color;
varying vec2 texcoord;
varying vec2 lmcoord;

uniform int leftEye;
uniform int direction;

const float eyeDistance = 0.14;

void orient(vec4 position, int orientation) {
    float z;
    if (orientation == 0) { // LEFT
        z = position.z;
        position.z = position.x;
        position.x = -z;
    } else if (orientation == 1) { // RIGHT
        z = position.z;
        position.z = -position.x;
        position.x = z;
    } else if (orientation == 2) { // FRONT
        // No changes required
    } else if (orientation == 3) { // BACK
        position.x = -position.x;
        position.z = -position.z;
    } else if (orientation == 4) { // TOP
        z = position.z;
        position.z = -position.y;
        position.y = z;
    } else if (orientation == 5) { // BOTTOM
        z = position.z;
        position.z = position.y;
        position.y = -z;
    }
}

void orientInverse(vec4 position, int orientation) {
    if (orientation < 2) {
        orient(position, 1 - orientation); // LEFT and RIGHT flip
    } else if (orientation < 4) {
        orient(position, orientation); // FRONT and BACK are their own inverses
    } else {
        orient(position, (1 - (orientation - 4)) + 4); // TOP and BOTTOM flip
    }
}

void main() {
    // Transform to view space
    vec4 position = gl_ModelViewMatrix * gl_Vertex;

    // Undo the camera rotation, so we always apply our stereo effect looking in the same direction
    orientInverse(position, direction);

    // Distort for ODS
    //  O := The origin
    //  P := The current vertex/point
    //  T := Point of tangency with the tangent going through P
    // Distance between P and O
    float distPO = sqrt(position.x * position.x + position.z * position.z);
    float distTO = eyeDistance * 0.5;
    // Angle between PO and PT
    float angP = acos(distTO / distPO);
    // Angle between PO and TO (angle at the origin)
    float angO = 90.0 - angP;
    if (leftEye == 0) {
        angO = -angO;
    }
    // The angel of OP within the circle, that is between OP and O(0,1)
    float angOP = atan(position.x, position.z);
    // The angle of OT within the circle, that is between OT and O(0,1)
    float angOT = angO + angOP;
    // Calculate the vector between O and T and finally move the vertex by that vector
    position -= vec4(distTO * sin(angOT), 0, distTO * cos(angOT), 0);

    // Rotate back into the correct cubic view
    orient(position, direction);

    // Transform to screen space
    gl_Position = gl_ProjectionMatrix * position;

    // Misc.
    color = gl_Color;
    texcoord = (gl_TextureMatrix[0] * gl_MultiTexCoord0).xy;
    lmcoord = (gl_TextureMatrix[1] * gl_MultiTexCoord1).xy;
}