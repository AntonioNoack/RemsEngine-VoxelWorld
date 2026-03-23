package me.anno.remcraft.rendering.globalillumination.gpu

import me.anno.mesh.vox.meshing.BlockSide
import me.anno.remcraft.world.Index.bitsX
import me.anno.remcraft.world.Index.bitsY
import me.anno.remcraft.world.Index.bitsZ
import me.anno.remcraft.world.Index.maskX
import me.anno.remcraft.world.Index.maskY
import me.anno.remcraft.world.Index.maskZ

fun hashChunkId(xi: Int, yi: Int, zi: Int): Int {
    val xd = xi.and(0x3ff)
    val yd = yi.and(0x3ff)
    val zd = zi.and(0x3ff)
    return 1 + xd + yd.shl(10) + zd.shl(20)
}

// todo implement skipping whole chunks
// todo implement half-transparent blocks:
//  pass-through is decided randomly (+ needs another bit :/)

val blockTracing = """
        int HashChunkId(ivec3 chunkId) {
            chunkId = chunkId & 0x3ff;
            return 1 + chunkId.x + (chunkId.y<<10) + (chunkId.z<<20);
        }
        int GetBlockInChunk(int chunkData, ivec3 blockPos) {
            blockPos = blockPos & ivec3($maskX,$maskY,$maskZ);
            int offset = blockPos.x + (blockPos.y << ${bitsX + bitsZ}) + (blockPos.z << $bitsX);
            int blockPattern = ChunkData[chunkData + (offset >> 4)];
            return (blockPattern >> ((offset & 15) * 2)) & 3;
        }
        int GetDataOffset(int chunkData) {
            if (chunkData == -1) return -1;
            int capacity = ChunkData[chunkData];
            return chunkData + capacity * 2 + 1;
        }
        float blockTracing(
            vec3 localStart, vec3 dir,
            out float hitDistance,
            out ivec4 hitFace,
            out int hitChunkData
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
                dir.x < 0.0 ? ${BlockSide.PX.ordinal} : ${BlockSide.NX.ordinal},
                dir.y < 0.0 ? ${BlockSide.PY.ordinal} : ${BlockSide.NY.ordinal},
                dir.z < 0.0 ? ${BlockSide.PZ.ordinal} : ${BlockSide.NZ.ordinal}
            );
            
            vec3 dtf3 = localStart / dir;
            float dtf1 = min(dtf3.x, min(dtf3.y, dtf3.z));
            int faceId = dtf3.z == dtf1 ? faceCandidates.z
                       : dtf3.y == dtf1 ? faceCandidates.y
                       :                  faceCandidates.x;
                       
            uvec3 chunkId = uvec3(0xffffffff);
            int chunkData0 = -1, chunkData = -1;
            
            hitFace = ivec4(0);
            hitDistance = -1.0;
            
            HashMap chunkHashMap = HashMap(ChunkData[chunkMap], chunkMap + 1);
            float opacity = 0.0;
            
            ivec3 dirSignI = ivec3(dirSign);
            for (int i=0; i<=maxSteps; i++) {
                if (i == maxSteps) return opacity;
                
                nextDist = min(dist3.x, min(dist3.y, dist3.z));
                bool continueTracing = true;
                float skippingDist = 0.0;
                
                // check block is here:
                ivec3 newChunkId = blockPos >> ivec3($bitsX,$bitsY,$bitsZ);
                if (newChunkId != chunkId) {
                    // if chunkId has changed, change chunk
                    chunkData0 = HashMapGet(chunkHashMap, HashChunkId(newChunkId));
                    chunkData = GetDataOffset(chunkData0);
                    chunkId = newChunkId;
                }
                
                if (chunkData != -1) {
                    int hitBlock = GetBlockInChunk(chunkData, blockPos);
                    // todo could be tinted!?
                    if (hitBlock == 1) opacity = mix(opacity, 0.99, 0.5); 
                    continueTracing = hitBlock != 2;
                }
                
                /*if (skippingDist >= 1.0) {
                    // todo if a chunk is missing, skip it quickly...
                   vec3 blockPosF = floor(localStart + dir * (dist + skippingDist));
                   blockPos = ivec3(blockPosF);
                   dist3 = (dirSign*.5+.5 + blockPosF - localStart)/dir;
                   nextDist = min(dist3.x, min(dist3.y, dist3.z));
                   dist = nextDist;
               } else*/ if (continueTracing) {
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
               } else break;
            }
            
            hitDistance = dist;
            hitFace = ivec4(blockPos, faceId);
            hitChunkData = chunkData0;
            
            return 1.0;
        }
    """.trimIndent()
