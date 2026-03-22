int HashChunkId(uvec3 chunkId) {
    return int(chunkId.x ^ (chunkId.y << 11) ^ (chunkId.z << 22));
}
bool blockTracing(
    vec3 localStart, vec3 dir,
    out float hitDistance,
    out ivec4 hitFace,
    out int hitBlockId,
) {
    if (abs(dir.x) < 1e-7) dir.x = 1e-7;
    if (abs(dir.y) < 1e-7) dir.y = 1e-7;
    if (abs(dir.z) < 1e-7) dir.z = 1e-7;
    vec3 dirSign = sign(dir);
    ivec3 blockPos = ivec3(floor(localStart));
    vec3 dist3 = (dirSign*.5+.5 + vec3(blockPos) - localStart) / dir;
    vec3 invUStep = dirSign / dir;
    float nextDist, dist = 0.0;
    ivec3 faceCandidates = ivec3(
        dir.x < 0.0 ? ${BlockSide.NX.ordinal} else ${BlockSide.PX.ordinal},
        dir.y < 0.0 ? ${BlockSide.NY.ordinal} else ${BlockSide.PY.ordinal},
        dir.z < 0.0 ? ${BlockSide.NZ.ordinal} else ${BlockSide.PZ.ordinal}
    );

    vec3 dtf3 = localStart / dir;
    float dtf1 = min(dtf3.x, min(dtf3.y, dtf3.z));
    int faceId = dtf3.z == dtf1 ? faceCandidates.z
               : dtf3.y == dtf1 ? faceCandidates.y
               :      faceCandidates.x;

    uvec3 chunkId = ivec3(0xffffffff);
    int chunkData = -1;

    hitBlockId = 0;
    hitFace = ivec4(0);
    hitDistance = -1.0;

    ivec3 dirSignI = ivec3(dirSign);
    for (int i=0; i<maxSteps; i++) {
        nextDist = min(dist3.x, min(dist3.y, dist3.z));
        bool continueTracing = true;
        float skippingDist = 0.0;

        // todo check block is here:
        uvec3 newChunkId = uvec3(blockPos) >> ivec3($bitsX,$bitsY,$bitsZ);
        if (newChunkId != chunkId) {
            // if chunkId has changed, change chunk
            chunkData = HashMapGet(chunkHashMap, HashChunkId(newChunkId));
            chunkId = newChunkId;
        }

        if (chunkData != -1) {
            hitBlockId = GetBlockInChunk(chunkData, blockPos & ivec3($maskX,$maskY,$maskZ));
            continueTracing = hitBlockId == 0;
        }

        if (skippingDist >= 1.0) {
            // todo if a chunk is missing, skip it quickly...
           blockPos = floor(localStart + dir * (dist + skippingDist));
           dist3 = (dirSign*.5+.5 + blockPos - localStart)/dir;
           nextDist = min(dist3.x, min(dist3.y, dist3.z));
           dist = nextDist;
       } else {
           if (nextDist == dist3.x) {
               blockPos.x += dirSignI.x;
               dist3.x += invUStep.x;
               faceId = faceCandidates.x;
           } else if (nextDist == dist3.y) {
               blockPos.y += dirSignI.y;
               dist3.y += invUStep.y;
               faceId = faceCandidates.y;
           } else {
               blockPos.z += dirSignI.z;
               dist3.z += invUStep.z;
               faceId = faceCandidates.z;
           }
           dist = nextDist;
       }
    }
    if (hitBlock == 0) return false;

    vec3 localPos = localStart + dir * dist;

    hitDistance = dist;
    hitFace = ivec4(blockPos, faceId);

    return true;
}