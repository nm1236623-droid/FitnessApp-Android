package com.example.fitness.data

// Reusable exercise library organized by body part and subgroup

data class Exercise(
    val name: String,
    val defaultSets: Int = 8,
    val defaultReps: Int? = null
)

data class ExerciseGroup(
    val id: String,
    val name: String,
    val exercises: List<Exercise>
)

object ExerciseLibrary {
    // Abdomen groups: 上腹 (upper) / 下腹 (lower)
    val AbdominalUpper = ExerciseGroup(
        id = "abs_upper",
        name = "上腹訓練 (主攻腹肌上半部)",
        exercises = listOf(
            Exercise("仰臥起坐 Sit-Up"),
            Exercise("卷腹 Crunch"),
            Exercise("十字卷腹 Cross Crunch"),
            Exercise("膝蓋碰手卷腹 Knee-to-Hand Crunch"),
            Exercise("上腹彈震 Crunch Pulse"),
            Exercise("負重卷腹 Weighted Crunch"),
            Exercise("Cable Crunch（拉繩捲腹）"),
            Exercise("Decline Sit-Up（下斜仰臥起坐）")
        )
    )

    val AbdominalLower = ExerciseGroup(
        id = "abs_lower",
        name = "下腹訓練 (主攻腹肌下半部)",
        exercises = listOf(
            Exercise("抬腿 Leg Raise"),
            Exercise("仰臥屈膝抬腿 Knee Raise"),
            Exercise("反向捲腹 Reverse Crunch"),
            Exercise("仰臥交替腳踢 Flutter Kicks"),
            Exercise("懸垂舉腿 Hanging Leg Raise"),
            Exercise("懸垂屈膝 Knee Raise"),
            Exercise("Cable Leg Raise")
        )
    )

    val AbdominalGroups = listOf(AbdominalUpper, AbdominalLower)

    // --- Strength/Powerlifting templates for Coach Center ---

    /** Push (bench-focused) */
    val StrengthChest = ExerciseGroup(
        id = "strength_chest",
        name = "胸推/臥推",
        exercises = listOf(
            Exercise("槓鈴臥推 Barbell Bench Press", defaultSets = 5, defaultReps = 5),
            Exercise("上斜臥推 Incline Bench Press", defaultSets = 4, defaultReps = 6),
            Exercise("啞鈴臥推 Dumbbell Bench Press", defaultSets = 4, defaultReps = 8),
            Exercise("窄握臥推 Close-Grip Bench Press", defaultSets = 4, defaultReps = 6)
        )
    )

    /** Pull (deadlift/back-focused) */
    val StrengthBack = ExerciseGroup(
        id = "strength_back",
        name = "背拉/硬舉",
        exercises = listOf(
            Exercise("傳統硬舉 Conventional Deadlift", defaultSets = 5, defaultReps = 3),
            Exercise("羅馬尼亞硬舉 Romanian Deadlift", defaultSets = 4, defaultReps = 6),
            Exercise("槓鈴划船 Barbell Row", defaultSets = 4, defaultReps = 6),
            Exercise("引體向上 Pull-Up", defaultSets = 4, defaultReps = 6)
        )
    )

    /** Legs (squat-focused) */
    val StrengthLegs = ExerciseGroup(
        id = "strength_legs",
        name = "腿蹲/深蹲",
        exercises = listOf(
            Exercise("深蹲 Back Squat", defaultSets = 5, defaultReps = 5),
            Exercise("前蹲 Front Squat", defaultSets = 4, defaultReps = 5),
            Exercise("腿推 Leg Press", defaultSets = 4, defaultReps = 8),
            Exercise("分腿蹲 Bulgarian Split Squat", defaultSets = 3, defaultReps = 8)
        )
    )

    /** Shoulders/overhead */
    val StrengthShoulders = ExerciseGroup(
        id = "strength_shoulders",
        name = "肩推",
        exercises = listOf(
            Exercise("站姿肩推 Overhead Press", defaultSets = 5, defaultReps = 5),
            Exercise("推舉 Push Press", defaultSets = 4, defaultReps = 3),
            Exercise("啞鈴肩推 DB Shoulder Press", defaultSets = 4, defaultReps = 8)
        )
    )

    /** Assistance */
    val StrengthAccessories = ExerciseGroup(
        id = "strength_accessory",
        name = "輔助",
        exercises = listOf(
            Exercise("三頭下壓 Triceps Pushdown", defaultSets = 3, defaultReps = 12),
            Exercise("二頭彎舉 Bicep Curl", defaultSets = 3, defaultReps = 12),
            Exercise("核心撐體 Plank", defaultSets = 3, defaultReps = 60)
        )
    )

    val StrengthGroups: List<ExerciseGroup> = listOf(
        StrengthChest,
        StrengthBack,
        StrengthLegs,
        StrengthShoulders,
        StrengthAccessories,
    )

    // Helper to convert an Exercise into ExerciseEntry (training plan item)
    fun toEntry(ex: Exercise): ExerciseEntry = ExerciseEntry(name = ex.name, reps = ex.defaultReps, sets = ex.defaultSets)

    fun getGroupById(id: String): ExerciseGroup? = (AbdominalGroups + StrengthGroups).firstOrNull { it.id == id }
}
