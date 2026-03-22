struct HashMap {
    int capacity;
    int dataPtr;
};
int HashUtilMix(int x) {
    uint h = uint(x * -1640531527);
    return int(h ^ (h >> 16));
}
int HashMapGetKey(HashMap hashMap, int pos) {
    return HashMapData[hashMap.dataPtr + pos];
}
int HashMapGetValue(HashMap hashMap, int pos) {
    return HashMapData[hashMap.dataPtr + hashMap.capacity + pos];
}
int HashMapGet(HashMap hashMap, int key) {
    int mask = hashMap.capacity - 1;
    int pos = HashUtilMix(key) & mask;
    int current = HashMapGetKey(hashMap, pos);
    if (current != 0) {
        if (current == key) {
            return pos
        }

        while (true) {
            pos = (pos + 1) & mask;
            current = HashMapGetKey(hashMap, pos);
            if (current == 0) return -1;
            if (current == key) return HashMapGetValue(hashMap, pos);
        }
    }

    return -1;
}