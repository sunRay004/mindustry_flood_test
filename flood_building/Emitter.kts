package flood

import arc.graphics.*
import arc.math.geom.*
import arc.util.*
import mindustry.content.*
import mindustry.gen.*
import mindustry.world.*
import mindustry.world.blocks.storage.CoreBlock
import arc.*
import arc.math.*
import arc.struct.*
import mindustry.Vars.*
import mindustry.entities.bullet.*
import mindustry.game.*
import mindustry.ui.*
import mindustry.world.blocks.defense.*
import mindustry.world.blocks.environment.*
import mindustry.world.meta.*
import java.util.*
import flood.Dependency_Tile.*

class tile {
    var data: Byte = 0
    var build: Building? = null
    var block: Block? = null
    var floor: Floor? = null
    var x: Float = 0f
    var y: Float = 0f
    var creep = 0f
}

class Emitter(build: Building?) : Position {
    var build: Building?
    var type: EmitterType
    var nullified = false
    protected var counter = 0

    init {
        if (build == null) {
            creeperEmitters.remove(this)
        }
        this.build = build
        type = emitterTypes.get(build?.block)
    }

    // updates every interval in CreeperUtils
    fun update(): Boolean {
        if (build == null || build.health <= 1f || build !is CoreBlock.CoreBuild) return false
        nullified = build.nullifyTimeout > 0f
        if (!nullified and (counter >= type.interval)) {
            counter = 0
            build.tile.getLinkedTiles { t -> t.creep = Math.min(t.creep + type.amt, maxTileCreep) }
        }
        counter++
        return true
    }

    // updates every 1 second
    fun fixedUpdate() {
        if (nullified) {
            Call.label("[red]*[] SUSPENDED [red]*[]", 1f, build!!.x, build!!.y)
            Call.effect(Fx.placeBlock, build!!.x, build!!.y, build!!.block?.size, Color.yellow)
        }
        if (build != null && build?.tile != null && type.upgradeThreshold > 0 && build?.tile.creep > 20) {
            Call.label(
                Strings.format(
                    "[green]*[white] UPGRADING []@% *[]",
                    (build?.tile?.creep * 100 / type.upgradeThreshold) as Int
                ), 1f, build?.x, build?.y
            )
            if (build?.tile.creep > type.upgradeThreshold) {
                val next = type.next
                if (next != null) {
                    build?.tile?.setNet(next.block, creeperTeam, 0)
                    build = build?.tile?.build
                    type = next
                }
            }
        }
    }

    @get:Override
    val x: Float
        get() = build?.x

    @get:Override
    val y: Float
        get() = build?.y

    enum class EmitterType(val amt: Int, interval: Int, val level: Int, upgradeThreshold: Int, block: Block) {
        shard(3, 30, 1, 30, Blocks.coreShard), foundation(5, 20, 2, 3000, Blocks.coreFoundation), nucleus(
            7,
            15,
            3,
            -1,
            Blocks.coreNucleus
        );

        val interval: Int
        val upgradeThreshold: Int
        val block: Block

        init {
            this.block = block
            this.interval = interval
            this.upgradeThreshold = upgradeThreshold
        }

        val next: EmitterType?
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
        var emitterTypes: HashMap<Block, EmitterType> = HashMap()
        fun init() {
            emitterTypes.put(Blocks.coreShard, EmitterType.shard)
            emitterTypes.put(Blocks.coreFoundation, EmitterType.foundation)
            emitterTypes.put(Blocks.coreNucleus, EmitterType.nucleus)
        }
    }
}