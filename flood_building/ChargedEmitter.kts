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