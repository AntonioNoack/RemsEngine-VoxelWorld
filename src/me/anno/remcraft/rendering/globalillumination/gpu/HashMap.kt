package me.anno.remcraft.rendering.globalillumination.gpu

data class HashMap(val capacity: Int, val entryOffset: Int)

val hashMap = """
        struct HashMap {
            int capacity;
            int entryOffset;
        };
        int HashUtilMix(int x) {
            uint h = uint(x * -1640531527);
            return int(h ^ (h >> 16));
        }
        int HashMapGetKey(HashMap hashMap, int pos) {
            return ChunkData[hashMap.entryOffset + pos];
        }
        int HashMapGetValue(HashMap hashMap, int pos) {
            return ChunkData[hashMap.entryOffset + hashMap.capacity + pos];
        }
        int HashMapGet(HashMap hashMap, int key) {
            int mask = hashMap.capacity - 1;
            int pos = HashUtilMix(key) & mask;
            while (true) {
                int currKey = HashMapGetKey(hashMap, pos);
                if (currKey == key) return HashMapGetValue(hashMap, pos);
                if (currKey == 0) return -1;
                pos = (pos + 1) & mask;
            }
        }
    """.trimIndent()
