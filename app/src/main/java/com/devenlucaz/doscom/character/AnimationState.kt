package com.devenlucaz.doscom.character

data class AnimationState(
    // Limbs (degrees, pivot at attachment)
    var leftArmAngle: Float = 0f,
    var rightArmAngle: Float = 0f,
    var leftLegAngle: Float = 0f,
    var rightLegAngle: Float = 0f,
    
    // Body
    var bodyOffsetY: Float = 0f,
    var bodyOffsetX: Float = 0f,
    var bodyRotation: Float = 0f, // Z-axis roll
    var bodyRotationY: Float = 0f, // Y-axis yaw
    var scaleX: Float = 1f,
    
    // Expression
    var eyesClosed: Boolean = false,
    var eyesHalf: Boolean = false,
    var eyesWide: Boolean = false,
    var pupilOffsetX: Float = 0f,
    var pupilOffsetY: Float = 0f,
    var mouthExpression: Int = 0,
    var mouthOpen: Boolean = false,
    var blushVisible: Boolean = false,
    var tongueOut: Boolean = false,
    
    // Special
    var antennaGlow: Float = 1f,
    var scale: Float = 1f,
    
    // Props
    var activeProp: PropType = PropType.NONE,
    
    // 3D Model
    var animationName: String = "Idle_A"
)
