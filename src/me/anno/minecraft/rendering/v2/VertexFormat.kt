package me.anno.minecraft.rendering.v2

import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode

object VertexFormat {

    val blockAttributes16Bit = bind(
        // more than that is unnecessary anyway
        Attribute("positions", AttributeType.SINT16, 3),
        Attribute("texIds", AttributeType.SINT16, 1)
    )

    val blockAttributes32Bit = bind(
        // more than that is unnecessary anyway
        Attribute("positions", AttributeType.SINT32, 3),
        Attribute("texIds", AttributeType.SINT32, 1)
    )

    val blockVertexData = MeshVertexData(
        listOf(
            ShaderStage(
                "positions", listOf(
                    Variable(GLSLType.V3I, "positions", VariableMode.ATTR),
                    Variable(GLSLType.V1I, "texIds", VariableMode.ATTR),
                    Variable(GLSLType.V3F, "localPosition", VariableMode.OUT),
                    Variable(GLSLType.V1I, "texId", VariableMode.OUT),
                ), "localPosition = vec3(positions);\n" +
                        "texId = texIds;\n"
            )
        ),
        listOf(
            ShaderStage(
                "nor", listOf(
                    Variable(GLSLType.V3F, "normal", VariableMode.OUT),
                    Variable(GLSLType.V4F, "tangent", VariableMode.OUT)
                ), "normal = vec3(0.0); tangent = vec4(0.0);\n"
            )
        ),
        MeshVertexData.DEFAULT.loadColors,
        MeshVertexData.DEFAULT.loadMotionVec,
        listOf(
            // calculate normals using cross product
            ShaderStage(
                "px-nor", listOf(
                    Variable(GLSLType.V3F, "finalPosition"),
                    Variable(GLSLType.V3F, "normal", VariableMode.OUT)
                ), "normal = normalize(cross(dFdx(finalPosition), dFdy(finalPosition)));\n"
            )
        ),
    )

    val detailsBlockVertexData = MeshVertexData(
        listOf(
            ShaderStage(
                "positions", listOf(
                    Variable(GLSLType.V3I, "positions", VariableMode.ATTR),
                    Variable(GLSLType.V1I, "texIds", VariableMode.ATTR),
                    Variable(GLSLType.V3F, "localPosition", VariableMode.OUT),
                    Variable(GLSLType.V1I, "texId", VariableMode.OUT),
                ), "localPosition = vec3(positions) * ${1.0 / 16.0};\n" +
                        "texId = texIds;\n"
            )
        ),
        blockVertexData.loadNorTan,
        MeshVertexData.DEFAULT.loadColors,
        MeshVertexData.DEFAULT.loadMotionVec,
        blockVertexData.onFragmentShader
    )

}