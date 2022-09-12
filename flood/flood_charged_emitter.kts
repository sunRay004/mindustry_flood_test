package flood

import arc.math.geom.*
import arc.util.*
import mindustry.content.*
import mindustry.gen.*
import mindustry.world.*

class ChargedEmitter(build: Building) : Position {
    var type: ChargedEmitterType
    var build: Building?
    var counter = 0
    var buildup = 0f
    var overflow = 0f
    var emitting = false
    var sb: StringBuilder = StringBuilder()
    fun update(): Boolean {
        if (build == null || build?.health <= 1f) return false
        if (build?.health < build?.maxHealth && overflow > 0) {
            overflow--
            build?.heal(build!!.maxHealth)
            Call.effect(Fx.healBlock, build!!.x, build!!.y, build.block.size, CreeperUtils.creeperTeam.color)
        }
        if (emitting) {
            if (++counter >= type.interval) {
                counter = 0
                build.tile.getLinkedTiles { t -> t.creep += type.amt }
            }
            if (--buildup <= 0) {
                emitting = false
                overflow = Math.min(type.chargeCap, overflow + build.tile.creep / 100)
                build.tile.getLinkedTiles { t -> t.creep = Math.min(t.creep, CreeperUtils.maxTileCreep) }
            }
        } else if (type.chargePulse.let { buildup += it; buildup } > type.chargeCap) {
            emitting = true
        }
        return true
    }

    fun fixedUpdate() {
        sb.setLength(0)
        if (overflow > 0) {
            sb.append(
                Strings.format(
                    "[green]@[] - [stat]@%",
                    if (type.upgradable()) "\ue804" else "\ue813",
                    (overflow * 100 / type.chargeCap).toInt()
                )
            )
        }
        if (emitting) {
            Call.effect(Fx.launch, build!!.x, build!!.y, build!!.block!!.size, CreeperUtils.creeperTeam.color)
        } else {
            if (sb.length() > 0) sb.append("\n")
            sb.append(Strings.format("[red]⚠[] - [stat] @%", (buildup * 100 / type.chargeCap).toInt()))
        }
        if (sb.length() > 0) {
            Call.label(sb.toString(), 1f, build!!.x, build!!.y)
        }
        if (type.upgradable() && type.chargeCap > 0 && build != null && build.tile != null && overflow >= type.chargeCap) {
            val next = type.next
            if (next != null) {
                type = next
                build.tile.setNet(next.block, CreeperUtils.creeperTeam, 0)
                build = build.tile.build
            }
        }
    }

    init {
        this.build = build
        type = chargedEmitterTypes.get(build.block)
    }

    @get:Override
    val x: Float
        get() = build.x

    @get:Override
    val y: Float
        get() = build.y

    internal enum class ChargedEmitterType(
        interval: Int,
        val amt: Int,
        level: Int,
        chargePulse: Float,
        chargeCap: Int,
        block: Block
    ) {
        bastion(2, 9, 1, 0.4f, 600, Blocks.coreBastion), citadel(1, 10, 2, 0.7f, 1800, Blocks.coreCitadel), acropolis(
            1,
            10,
            3,
            0.99f,
            2000,
            Blocks.coreAcropolis
        );

        val level: Int
        val interval: Int
        val chargeCap: Int
        val chargePulse: Float
        val block: Block

        init {
            this.block = block
            this.level = level
            this.interval = interval
            this.chargeCap = chargeCap
            this.chargePulse = chargePulse
        }

        fun upgradable(): Boolean {
            return level < values().size
        }

        val next: ChargedEmitterType?
            get() {
                for (t in values()) {
                    if (t.level == level + 1) {
                        return t
                    }
                }
                return null
            }
    }

    companion object {
        @JvmField
        var chargedEmitterTypes: HashMap<Block, ChargedEmitterType> = HashMap()
        @JvmStatic
        fun init() {
            chargedEmitterTypes.put(Blocks.coreBastion, ChargedEmitterType.bastion)
            chargedEmitterTypes.put(Blocks.coreCitadel, ChargedEmitterType.citadel)
            chargedEmitterTypes.put(Blocks.coreAcropolis, ChargedEmitterType.acropolis)
        }
    }
}

/*
ERROR Class 'ChargedEmitter' is not abstract and does not implement abstract member public abstract fun getX(): Float defined in arc.math.geom.Position (flood_charged_emitter.kts:9:1)
ERROR 'public' property exposes its 'internal' type ChargedEmitterType (flood_charged_emitter.kts:10:9)
ERROR Operator call corresponds to a dot-qualified call 'build?.health.compareTo(1f)' which is not allowed on a nullable receiver 'build?.health'. (flood_charged_emitter.kts:18:44)
ERROR Operator call corresponds to a dot-qualified call 'build?.health.compareTo(build?.maxHealth)' which is not allowed on a nullable receiver 'build?.health'. (flood_charged_emitter.kts:19:27)
ERROR Type mismatch: inferred type is Float? but Float was expected (flood_charged_emitter.kts:19:29)
ERROR Smart cast to 'Building' is impossible, because 'build' is a mutable property that could have been changed by this time (flood_charged_emitter.kts:22:61)
ERROR Type mismatch: inferred type is Int but Float was expected (flood_charged_emitter.kts:22:61)
ERROR Unresolved reference: CreeperUtils (flood_charged_emitter.kts:22:79)
ERROR Smart cast to 'Building' is impossible, because 'build' is a mutable property that could have been changed by this time (flood_charged_emitter.kts:27:17)
ERROR Unresolved reference: creep (flood_charged_emitter.kts:27:52)
ERROR Unresolved reference: += (flood_charged_emitter.kts:27:58)
ERROR Type mismatch: inferred type is Int but Float was expected (flood_charged_emitter.kts:31:28)
ERROR Smart cast to 'Building' is impossible, because 'build' is a mutable property that could have been changed by this time (flood_charged_emitter.kts:31:64)
ERROR Unresolved reference: creep (flood_charged_emitter.kts:31:75)
ERROR Smart cast to 'Building' is impossible, because 'build' is a mutable property that could have been changed by this time (flood_charged_emitter.kts:32:17)
ERROR Unresolved reference: creep (flood_charged_emitter.kts:32:52)
ERROR Unresolved reference: creep (flood_charged_emitter.kts:32:71)
ERROR Unresolved reference: CreeperUtils (flood_charged_emitter.kts:32:78)
ERROR Type mismatch: inferred type is Int but Float was expected (flood_charged_emitter.kts:52:58)
ERROR Unresolved reference: CreeperUtils (flood_charged_emitter.kts:52:80)
ERROR Expression 'length' of type 'Int' cannot be invoked as a function. The function 'invoke()' is not found (flood_charged_emitter.kts:54:20)
ERROR Expression 'length' of type 'Int' cannot be invoked as a function. The function 'invoke()' is not found (flood_charged_emitter.kts:57:16)
ERROR Smart cast to 'Building' is impossible, because 'build' is a mutable property that could have been changed by this time (flood_charged_emitter.kts:60:73)
ERROR Smart cast to 'Building' is impossible, because 'build' is a mutable property that could have been changed by this time (flood_charged_emitter.kts:64:17)
ERROR Unresolved reference: CreeperUtils (flood_charged_emitter.kts:64:47)
ERROR Smart cast to 'Building' is impossible, because 'build' is a mutable property that could have been changed by this time (flood_charged_emitter.kts:65:25)
ERROR Type mismatch: inferred type is Flood_charged_emitter.ChargedEmitter.ChargedEmitterType? but Flood_charged_emitter.ChargedEmitter.ChargedEmitterType was expected (flood_charged_emitter.kts:72:16)
ERROR Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Building? (flood_charged_emitter.kts:77:22)
ERROR Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Building? (flood_charged_emitter.kts:81:22)
ERROR 'public' property exposes its 'internal' type argument ChargedEmitterType (flood_charged_emitter.kts:131:13)
[2022-09-12 | 14:47:05 | 警告] [ScriptAgent] 加载脚本 flood/flood_charged_emitter 失败: Class 'ChargedEmitter' is not abstract and does not implement abstract member public abstract fun getX(): Float defined in arc.math.geom.Position (flood_charged_emitter.kts:9:1)
 */
