package com.devenlucaz.doscom.character

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CompanionRenderer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    var state = AnimationState()
        set(value) {
            field = value
            update3DModelState()
        }

    var zzzParticles = listOf<com.devenlucaz.doscom.animation.ZzzParticle>()
    var antennaColor: Int = Color.WHITE

    val sceneView: SceneView = SceneView(context, attrs)
    var modelNode: ModelNode = ModelNode(engine = sceneView.engine)

    init {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        // Ensure transparent background for floating overlay
        sceneView.setBackgroundColor(Color.TRANSPARENT)
        
        addView(sceneView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        
        CoroutineScope(Dispatchers.Main).launch {
            val modelInstance = sceneView.modelLoader.loadModelInstance("Alien.glb")
            modelInstance?.let {
                modelNode.modelInstance = it
                sceneView.addChildNode(modelNode)
                applyColors()
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDetachedFromWindow()
    }

    private fun applyColors() {
        val modelInstance = modelNode.modelInstance ?: return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Loop through all material instances to tint them
                modelInstance.materialInstances.forEach { material ->
                    val name = material.name.lowercase()
                    if (name.contains("eye")) {
                        material.setParameter("baseColorFactor", 0.0f, 1.0f, 1.0f, 1.0f) // Cyan
                        material.setParameter("emissiveFactor", 0.0f, 1.0f, 1.0f) // Cyan glow
                    } else if (name.contains("body") || name.contains("skin")) {
                        material.setParameter("baseColorFactor", 0.039f, 0.055f, 0.102f, 1.0f) // #0A0E1A approx
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun update3DModelState() {
        // Map 2D states to 3D transforms
        // Make the base size slightly larger
        val targetScale = state.scale * 1.5f 
        val flipX = if (state.scaleX < 0) -1f else 1f
        
        // We only scale X and Y in 2D, but for 3D we apply uniform or flipped scale
        modelNode.scale = Position(x = targetScale * flipX, y = targetScale, z = targetScale)
        
        // Body rotation (mapping 2D rotation to 3D Y or Z axis)
        // 2D rotation typically rotates around Z, but for a 3D character facing forward,
        // it might make sense to slightly tilt or rotate based on the action
        modelNode.rotation = Rotation(x = 0f, y = state.bodyRotation, z = 0f)
    }
}
