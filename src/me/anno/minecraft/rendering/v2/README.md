# UniqueMeshRenderer

This rendering method tries to minimize the number of draw calls by joining the mesh into one consecutive buffer.
It also saves on memory compared to V1 by using a custom mesh format with integer data instead of floats.