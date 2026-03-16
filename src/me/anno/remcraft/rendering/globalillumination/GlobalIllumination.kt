package me.anno.remcraft.rendering.globalillumination

object GlobalIllumination {

    // todo perfect graphics?
    //  a) we can do block-tracing in real-time -> we can trace a ray to the sun if need-be
    //  b) complex:
    //  1st degree via shadow map, and explicit light-source->block-face transfer functions
    //  2nd degree via face->face transfer functions (w < 1, decay with distance, angle, ... max 7 radius)
    //  three or so iterations of 2nd degree; all is diffuse
    //  blur neighboring parallel faces: our rendering needs to know what face we're on, and what our neighbors are

    // todo this is very complex :/
    //  we need the following:
    //  - dynamic buffer of all faces
    //    - pos
    //    - side
    //    - transfer functions (offset, count?) -> to all other buffers = invalidation complex
    //  - 1st degree & 2nd degree compute shaders
    //  - using light data in rendering (each storing face index & neighbors)

    // todo lights can be explicitly modelled as a cpu-computed list
    //  light-level (flicker for flame) * transmission * faceList

    // todo according to sb on Reddit, we only need ~20 random faces per face, and 7 iterations for it to get stable
    // todo this is called Radiosity

}