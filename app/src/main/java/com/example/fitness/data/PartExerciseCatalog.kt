package com.example.fitness.data

/**
 * Single source of truth for "body part -> grouped exercise name list".
 *
 * Both:
 * - ui/TrainingPlanScreen (選擇訓練部位)
 * - coach/ui/CoachPlanCreateScreen (教練中心 -> Create Plan)
 *
 * must use this catalog so the lists stay in sync.
 */
object PartExerciseCatalog {

    /**
     * Key is a part id used across the app: chest/back/legs/arms/shoulders/abs
     * Value is a list of (groupTitle -> exerciseNameList)
     */
    fun groupsForPart(part: String): List<Pair<String, List<String>>> {
        return when (part) {
            "abs" -> ExerciseLibrary.AbdominalGroups.map { grp -> grp.name to grp.exercises.map { it.name } }
            "chest" -> chestGroups
            "back" -> backGroups
            "legs" -> legsGroups
            "arms" -> armsGroups
            "shoulders" -> shouldersGroups
            else -> emptyList()
        }
    }

    /**
     * 根据练习名称获取对应的身体部位
     */
    fun getPartForExercise(exerciseName: String): String? {
        val allParts = listOf("chest", "back", "legs", "arms", "shoulders", "abs")
        return allParts.firstOrNull { part ->
            groupsForPart(part).any { (_, exercises) ->
                exercises.any { it.equals(exerciseName, ignoreCase = true) }
            }
        }
    }

    // IMPORTANT: Keep exercise names identical to what the UI expects and what gets stored.

    private val chestGroups: List<Pair<String, List<String>>> = listOf(
        "槓鈴/臥推" to listOf("槓鈴臥推 (Bench Press)", "窄握臥推 (窄握，偏三頭)"),
        "上斜" to listOf("上斜槓鈴臥推 (上胸)"),
        "下斜" to listOf("下斜槓鈴臥推 (下胸)"),
        "器械 - Cable" to listOf(
            "Cable 夾胸 (Cable Crossover)",
            "Cable 上胸夾胸 (Low to High)",
            "Cable 下胸夾胸 (High to Low)",
            "單手 Cable 夾胸 (改善左右胸不平衡)"
        ),
        "器械 - Machine" to listOf(
            "胸推機 (Chest Press)",
            "夾胸機 (Pec Deck Fly)",
            "上斜胸推機",
            "下斜胸推機",
            "俯臥推胸機 (Hammer Strength Type)"
        )
    )

    private val backGroups: List<Pair<String, List<String>>> = listOf(
        "背闊肌 (Latissimus)" to listOf(
            "引體向上（Pull-up）",
            "反手引體向上（Chin-up）",
            "雙槓垂拉（在雙槓上做反向划船）",
            "自重划船（Inverted Row）",
            "啞鈴單手划船（One-arm Dumbbell Row）",
            "槓鈴划船（Barbell Row）",
            "T-bar 划船（T-bar Row）",
            "槓鈴 Yates Row（反握）",
            "啞鈴上拉（Dumbbell Pullover — 胸背一起帶）",
            "Cable 下拉（Lat Pulldown）",
            "窄握下拉（Close Grip Pulldown）",
            "反握下拉（Reverse Grip Pulldown）",
            "Hammer Strength 背推拉機（Lat Row Machine）"
        ),
        "上背 (Rhomboids / Mid Traps)" to listOf(
            "自重倒划船（Inverted Row）",
            "超人式（Isometric Hold）",
            "雙手啞鈴划船（Dumbbell Row）",
            "槓鈴俯身划船（Pendlay Row）",
            "啞鈴後三角側平舉（Rear Delt Fly — 也會帶到上背）",
            "啞鈴反向飛鳥（Reverse Fly）",
            "坐姿划船（Seated Row） - 寬握/窄握/V-bar",
            "Cable 臉拉（Face Pull）",
            "Pec Deck 後飛鳥設定（Posterior Fly）"
        ),
        "下背 (Erector Spinae)" to listOf(
            "超人式（Superman）",
            "Bird Dog",
            "反向後伸（Back Extension）",
            "傳統硬舉（Deadlift）",
            "羅馬尼亞硬舉（RDL）",
            "Good Morning（啞鈴或槓鈴）",
            "壺鈴擺盪（Kettlebell Swing）"
        ),
        "斜方肌 (Trapezius)" to listOf(
            "聳肩（Shrug）",
            "啞鈴聳肩",
            "槓鈴聳肩",
            "Cable 聳肩",
            "坐姿划船（高角度）",
            "直立拉（Upright Row）"
        )
    )

    private val legsGroups: List<Pair<String, List<String>>> = listOf(
        "股四頭肌（大腿前側）" to listOf(
            "深蹲（Squat）",
            "前跨弓箭步（Forward Lunge）",
            "後跨弓箭步（Reverse Lunge）",
            "保加利亞分腿蹲（Bulgarian Split Squat）",
            "箱子登階（Step-up）",
            "靠牆靜蹲（Wall Sit）",
            "槓鈴深蹲（Barbell Back Squat）",
            "前蹲（Front Squat）",
            "上斜槓鈴深蹲（High-bar Squat）",
            "啞鈴深蹲（Dumbbell Squat）",
            "啞鈴分腿蹲/弓箭步",
            "45° 腿推機（Leg Press）",
            "腿部伸展（Leg Extension）",
            "哈克深蹲（Hack Squat）",
            "45° 史密斯深蹲（Smith Machine Squat）"
        ),
        "股二頭肌（大腿後側）" to listOf(
            "單腳早安（Single-leg Good Morning）",
            "仰臥腿屈（徒手版）",
            "硬舉（Deadlift）",
            "羅馬尼亞硬舉（RDL）",
            "直腿硬舉（Stiff Leg Deadlift）",
            "啞鈴早安（Good Morning）",
            "啞鈴後踢",
            "腿後側屈伸（Leg Curl）",
            "俯臥腿屈",
            "坐姿腿屈",
            "站姿腿屈"
        ),
        "臀部訓練（臀大肌/臀中肌）" to listOf(
            "臀推（Hip Thrust）",
            "臀橋（Glute Bridge）",
            "蚌式開合（Clam Shell）",
            "怪獸走路（Monster Walk）",
            "側抬腿（Side Leg Raise）",
            "槓鈴臀推（Barbell Hip Thrust）",
            "槓鈴臀橋",
            "啞鈴後踢（Kickback）",
            "啞鈴相撲深蹲（Sumo Squat）",
            "直腿硬舉（刺激臀腿）",
            "臀推機（Hip Thrust Machine）",
            "外展機（Hip Abductor）",
            "後踢機（Glute Kickback)"
        ),
        "內外側大腿（內收/外展）" to listOf(
            "外展機（Abductor）",
            "內收機（Adductor）",
            "Cable 外展 / 內收",
            "彈力帶側走（Lateral Band Walk）"
        ),
        "小腿（腓腸肌/比目魚肌）" to listOf(
            "站姿提踵（Standing Calf Raise）",
            "坐姿提踵（Seated Calf Raise）",
            "單腳提踵",
            "機械提踵（Calf Machine）",
            "Leg Press 上做提踵"
        )
    )

    private val armsGroups: List<Pair<String, List<String>>> = listOf(
        "二頭肌 (Biceps)" to listOf(
            "俯身靠牆屈臂拉（Bodyweight Bicep Curl）",
            "懸吊彎曲（Pull-up 反握可以帶二頭）",
            "站姿啞鈴彎舉（Dumbbell Curl）",
            "站姿槓鈴彎舉（Barbell Curl）",
            "槌式彎舉（Hammer Curl）",
            "交替啞鈴彎舉",
            "Zottman Curl（前段彎舉，後段旋轉手心向下放下）",
            "Cable 二頭肌彎舉（直立或坐姿）",
            "Preacher Curl 機器"
        ),
        "三頭肌 (Triceps)" to listOf(
            "窄距伏地挺身（Diamond Push-up）",
            "椅上三頭肌撐體（Bench Dip）",
            "臥姿啞鈴三頭肌伸展（Dumbbell Skull Crusher）",
            "槓鈴臥姿三頭肌伸展（Barbell Skull Crusher）",
            "單手啞鈴頭後伸展（Overhead Dumbbell Extension）",
            "啞鈴 Kickback",
            "Cable 下壓（Pushdown）",
            "繩索下壓（Rope Pushdown）",
            "Overhead Cable Extension",
            "機械三頭肌伸展"
        ),
        "前臂 (Forearm)" to listOf(
            "掌心向上/向下腕屈伸",
            "反手俯身懸吊握力",
            "手腕卷舉（Wrist Curl）",
            "反手手腕卷舉（Reverse Wrist Curl）",
            "槌式彎舉（Hammer Curl）",
            "Cable 手腕彎舉",
            "Cable 反手手腕彎舉"
        )
    )

    private val shouldersGroups: List<Pair<String, List<String>>> = listOf(
        "前三角肌（前肩）" to listOf(
            "伏地挺身（Push-up，重心前傾偏前肩）",
            "徒手肩推（Handstand Push-up，進階）",
            "啞鈴肩推（Dumbbell Overhead Press）",
            "槓鈴軍事推舉（Barbell Military Press）",
            "啞鈴前平舉（Front Raise）",
            "Cable 前平舉",
            "機械肩推（Shoulder Press Machine）"
        ),
        "側三角肌（中肩 / 側肩）" to listOf(
            "側平舉徒手（Hand support 側抬）",
            "彈力帶側平舉",
            "啞鈴側平舉（Lateral Raise）",
            "啞鈴俯身側平舉（Rear Lateral Raise）",
            "坐姿啞鈴側平舉（穩定核心）",
            "Cable 側平舉",
            "俯身 Cable 側平舉（Rear Lateral）"
        ),
        "後三角肌（後肩 / 上背側）" to listOf(
            "超人式（Superman，帶後肩+上背）",
            "桌下划船式（Inverted Row 變化，帶後肩）",
            "俯身後飛鳥（Bent-over Rear Delt Fly）",
            "坐姿後飛鳥（Seated Rear Delt Fly）",
            "Cable 反向飛鳥",
            "Pec Deck 反向飛鳥（Rear Delt Fly Machine）"
        )
    )
}

