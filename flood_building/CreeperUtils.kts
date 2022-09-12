package flood

import arc.*
import arc.graphics.*
import arc.math.*
import arc.math.geom.*
import arc.struct.*
import arc.util.*
import mindustry.Vars.*
import mindustry.content.*
import mindustry.entities.bullet.*
import mindustry.game.*
import mindustry.gen.*
import mindustry.ui.*
import mindustry.world.*
import mindustry.world.blocks.defense.*
import mindustry.world.blocks.environment.*
import mindustry.world.meta.*
import java.util.*
import flood.Dependency_Tile.*

class Tile {
    var data: Byte = 0
    var build: Building? = null
    var block: Block? = null
    var floor: Floor? = null
    var x: Float = 0f
    var y: Float = 0f
    var creep = 0f

    fun worldx(): Float {
        return x * tilesize
    }

    fun worldy(): Float {
        return y * tilesize
    }

    fun drawx(): Float {
        return block().offset + worldx()
    }

    fun drawy(): Float {
        return block().offset + worldy()
    }

    fun getX(): Float {
        return drawx()
    }

    fun getY(): Float {
        return drawy()
    }

    fun setBlock(type: Block, team: Team?) {
        setBlock(type, team, 0)
    }

    fun setBlock(type: Block) {
        setBlock(type, Team.derelict, 0)
    }
}

object CreeperUtils {
    const val updateInterval = 0.03f // Base update interval in seconds
    const val transferRate = 0.25f // Base transfer rate NOTE: keep below 0.25f
    const val creeperDamage = 0.2f // Base creeper damage
    const val creeperEvaporationUponDamagePercent = 0.98f // Creeper percentage that will remain upon damaging something
    const val creeperUnitDamage = 2f
    const val maxTileCreep = 10.5f
    const val creeperBlockDamageMultiplier = 0.75f

    /*
    public static BulletType sporeType = new ArtilleryBulletType(3f, 20, "shell") {{
        hitEffect = Fx.flakExplosion;
        knockback = 0.8f;
        lifetime = 80f;
        width = height = 11f;
        collidesTiles = false;
        splashDamageRadius = 25f * 0.75f;
        splashDamage = 33f;
    }};
     */
    var sporeType: BulletType = Bullets.placeholder
    var sporeMaxRangeMultiplier = 30f
    var sporeAmount = 20f
    var sporeRadius = 5f
    var sporeSpeedMultiplier = 0.15f
    var sporeHealthMultiplier = 10f
    var sporeTargetOffset = 256f
    var unitShieldDamageMultiplier = 1.5f
    var buildShieldDamageMultiplier = 1.5f
    var shieldBoostProtectionMultiplier = 0.5f
    var shieldCreeperDropAmount = 7f
    var shieldCreeperDropRadius = 4f
    var nullifierRange: Float = 16f * tilesize
    var radarBeamDamage = 300f // damage the radar creeper beam deals to units
    var creepTowerDeposit = 0.3f // amount of creep deposited by the creep tower per tick
    var creepTowerRange = 300f // just slightly bigger than ripple's range
    var nullifyDamage = 1500f // Damage that needs to be applied for the core to be suspended
    var nullifyTimeout = 180f // The amount of ticks a core remains suspended (resets upon enough damage applied)
    var nullificationPeriod =
        10f // How many seconds all cores have to be nullified (suspended) in order for the game to end
    var tutorialID = 0
    private const val nullifiedCount = 0
    private const var pulseOffset = 0
    var creeperTeam: Team = Team.blue
    var creeperBlocks: HashMap<Int, Block> = HashMap<Int, Block>()
    var creeperLevels: HashMap<Block, Integer> = HashMap()
    var creeperEmitters: Seq<Emitter> = Seq()
    var chargedEmitters: Seq<ChargedEmitter> = Seq()
    var creeperableTiles: Seq<Tile> = Seq()
    var shields: Seq<ForceProjector.ForceBuild> = Seq()
    var runner: Timer.Task? = null
    var fixedRunner: Timer.Task? = null
    val tutContinue = arrayOf(arrayOf("[#49e87c]\uE829 Continue[]"))
    val tutFinal = arrayOf(arrayOf("[#49e87c]\uE829 Finish[]"))
    val tutStart =
        arrayOf(arrayOf("[#49e87c]\uE875 Take the tutorial[]"), arrayOf("[#e85e49]⚠ Skip (not recommended)[]"))
    val tutEntries = arrayOf(
        "[accent]\uE875[] 引导 1/6",
        "In [#e056f0]\uE83B the flood[] there are [scarlet]no units[] to defeat.\nInstead, your goal is to suspend all [accent]emitters[], which are simply [accent]enemy cores, launchpads and accelerators.[]",
        "[accent]\uE875[] 引导 2/6",
        "[scarlet]⚠ beware![]\n[accent]Emitters[] spawn [#e056f0]\uE83B the flood[], which when in proximity to friendly buildings or units, damages them.",
        "[accent]\uE875[] 引导 3/6",
        "[scarlet]⚠ beware![]\n[accent]Charged Emitters[] spawn [#e056f0]\uE83B the flood[] much faster, but they are only active for small periods.",
        "[accent]\uE875[] 引导 4/6",
        "You can [accent]suspend emitters[] by constantly dealing damage to them, and destroy [accent]charged emitters[] to remove them.",
        "[accent]\uE875[] 引导 5/6",
        "If [accent]emitters[] are sufficiently suspended, you can [accent]nullify them[] by building an \uF871 [accent]Impact Reactor[] near them and activating it.",
        "[accent]\uE875[] 引导 6/6",
        "If [accent]emitters[] are surrounded by the maximum creep, they will begin [stat]upgrading[]. You can stop the upgrade by suspending them.",
        "[white]\uF872[]",
        "[accent]Spore Launchers[]\n[accent]Thorium Reactors[] shoot long distance artillery that on impact, releases [accent]a huge amount of flood[], you can defend against this with segments \uF80E.",
        "[white]\uF682[]",
        "[accent]Flood Projector[]\n[accent]Shockwave Towers[] rapidly deposit flood at any nearby buildings, forcing a [accent]different approach[] than turret spam.\nRange is slightly larger than Ripples.",
        "[white]\uF6AD[]",
        "[accent]Flood Radar[]\n[accent]Radars[] focus on the closest unit, and after a short time of charging, [accent]shoot[] at that unit, forcing a [accent]different approach[] than unit spam.\nRange is slightly larger than Ripples.",
        "[white]\uF898[]",
        "[accent]Flood Shield[]\n[accent]Force Projectors[] and [accent]unit shields[] actively absorb [#e056f0]the flood[], but [accent]explode[] when they are full.",
        "[white]\uF7FA[]",
        "[accent]Flood Creep[]\n[accent]Spider-Type units[] explode when in contact of friendly buildings and release tons of [#e056f0]the flood[].",
        "[white]\uF7F5[]",
        "[accent]Horizons[] are immune to the flood but [orange]do not deal any damage[]. Use them to carry [accent]resources[] over the flood. They are not immune to emitters and spore launchers."
    )

    fun getTrafficlightColor(value: Double): String {
        return "#" + Integer.toHexString(java.awt.Color.HSBtoRGB(value.toFloat() / 3f, 1f, 1f)).substring(2)
    }

    fun targetSpore(): FloatArray {
        var ret: FloatArray? = null
        var iterations = 0
        while (ret == null && iterations < 10000 && Groups.player.size() > 0) {
            iterations++
            val player: Player = Groups.player.index(Mathf.random(0, Groups.player.size() - 1))
            if (player.unit() == null || player.x.toInt() == 0 && player.y.toInt() == 0) continue
            val unit: Unit = player.unit()
            ret = floatArrayOf(
                unit.x + Mathf.random(-sporeTargetOffset, sporeTargetOffset),
                unit.y + Mathf.random(-sporeTargetOffset, sporeTargetOffset)
            )
            val retTile: mindustry.world.Tile! = world.tileWorld(ret[0], ret[1])

            // target creeperableTiles only
            if (creeperableTiles.contains(retTile)) {
                return ret
            }
        }
        return ret ?: floatArrayOf(0f, 0f)
    }

    fun sporeCollision(bullet: Bullet?, x: Float, y: Float) {
        val tile: mindustry.world.Tile! = world.tileWorld(x, y)
        if (invalidTile(tile)) return
        Call.effect(Fx.sapExplosion, x, y, sporeRadius, Color.blue)
        depositCreeper(tile, sporeRadius, sporeAmount)
    }

    fun init() {


        // old walls since conveyors no longer work :{
        creeperBlocks.put(0, Blocks.air)
        creeperBlocks.put(1, Blocks.scrapWall)
        creeperBlocks.put(2, Blocks.titaniumWall)
        creeperBlocks.put(3, Blocks.thoriumWall)
        creeperBlocks.put(4, Blocks.plastaniumWall)

        // new erekir walls
        creeperBlocks.put(5, Blocks.phaseWall)
        creeperBlocks.put(6, Blocks.surgeWall)
        creeperBlocks.put(7, Blocks.reinforcedSurgeWall)
        creeperBlocks.put(8, Blocks.berylliumWall)
        creeperBlocks.put(9, Blocks.tungstenWall)
        creeperBlocks.put(10, Blocks.carbideWall)

        // this is purely for damage multiplication
        creeperBlocks.put(12, Blocks.thoriumReactor)
        creeperBlocks.put(20, Blocks.coreShard)
        creeperBlocks.put(25, Blocks.coreFoundation)
        creeperBlocks.put(30, Blocks.coreNucleus)
        creeperBlocks.put(75, Blocks.coreBastion)
        creeperBlocks.put(76, Blocks.coreCitadel)
        creeperBlocks.put(77, Blocks.coreAcropolis)
        for (set in creeperBlocks.entrySet()) {
            val newFlags: Array<BlockFlag?> = arrayOfNulls<BlockFlag>(set.getValue().flags.size + 1)
            var i = 0
            for (flag in set.getValue().flags.array) {
                newFlags[i++] = flag
            }
            newFlags[i] = BlockFlag.generator
            set.getValue().flags = EnumSet.of(newFlags)
            creeperLevels.put(set.getValue(), set.getKey())
        }
        Emitter.init()
        ChargedEmitter.init()
        var menuID = 0
        var i = tutEntries.size
        while (--i >= 0) {
            val j = i
            val current = menuID
            menuID = Menus.registerMenu { player, selection ->
                if (selection === 1) return@registerMenu
                if (j == tutEntries.size / 2) return@registerMenu
                Call.menu(
                    player.con,
                    current,
                    tutEntries[2 * j],
                    tutEntries[2 * j + 1],
                    if (j == tutEntries.size / 2 - 1) tutFinal else tutContinue
                )
            }
        }
        tutorialID = menuID
        Events.on(EventType.PlayerJoin::class.java) { e ->
            if (e.player.getInfo().timesJoined > 1) return@on
            Call.menu(
                e.player.con,
                tutorialID,
                "[accent]Welcome![]",
                "Looks like it's your first time playing..",
                tutStart
            )
        }
        Events.on(EventType.GameOverEvent::class.java) { e ->
            if (runner != null) runner.cancel()
            if (fixedRunner != null) fixedRunner.cancel()
            creeperableTiles.clear()
            creeperEmitters.clear()
            chargedEmitters.clear()
            shields.clear()
        }
        Events.on(EventType.PlayEvent::class.java) { e ->
            creeperableTiles.clear()
            chargedEmitters.clear()
            creeperEmitters.clear()
            for (tile in world.tiles) {
                if (!tile.floor()
                        .isDeep() && tile.floor().placeableOn && (tile.breakable() || tile.block() === Blocks.air || tile.block() is TreeBlock)
                ) {
                    creeperableTiles.add(tile)
                }
            }
            for (build in Groups.build) {
                if (build.team !== creeperTeam) continue
                if (Emitter.emitterTypes.containsKey(build.block)) {
                    creeperEmitters.add(Emitter(build))
                } else if (ChargedEmitter.chargedEmitterTypes.containsKey(build.block)) {
                    chargedEmitters.add(ChargedEmitter(build))
                }
            }
            Log.info(creeperableTiles.size.toString(), " creeperable tiles")
            Log.info(creeperEmitters.size.toString(), " emitters")
            Log.info(chargedEmitters.size.toString(), " charged emitters")
            emitterDst = Array(world.width()) { IntArray(world.height()) }
            resetDistanceCache()
            runner = Timer.schedule({ obj: CreeperUtils? -> updateCreeper() }, 0, updateInterval)
            fixedRunner = Timer.schedule({ obj: CreeperUtils? -> fixedUpdate() }, 0, 1)
        }
        Events.on(EventType.BlockDestroyEvent::class.java) { e ->
            if (creeperBlocks.containsValue(e.tile.block())) {
                e.tile.creep = 0
            }
        }
        Timer.schedule({
            if (!state.isGame()) return@schedule
            Call.infoPopup(
                Strings.format(
                    "\uE88B [@] @/@ []emitters suspended\n\uE88B [@] @ []charged emitters remaining",
                    getTrafficlightColor(
                        Mathf.clamp(CreeperUtils.nullifiedCount / Math.max(1.0, creeperEmitters.size), 0f, 1f)
                    ), CreeperUtils.nullifiedCount, creeperEmitters.size,
                    if (chargedEmitters.size > 0) "red" else "green", chargedEmitters.size
                ), 10f, 20, 50, 20, 527, 0
            )
            // check for gameover
            if (CreeperUtils.nullifiedCount == creeperEmitters.size) {
                Timer.schedule({
                    if (CreeperUtils.nullifiedCount == creeperEmitters.size && chargedEmitters.size <= 0) {
                        // gameover
                        state.gameOver = true
                        Events.fire(GameOverEvent(state.rules.defaultTeam))
                    }
                }, nullificationPeriod)
            }
        }, 0, 10)
    }

    fun depositCreeper(tile: Tile, radius: Float, amount: Float) {
        Geometry.circle(tile.x, tile.y, radius.toInt()) { cx, cy ->
            val ct: Tile = world.tile(cx, cy)
            if (invalidTile(ct) || tile.block() is StaticWall || tile.floor() != null && !tile.floor().placeableOn || tile.floor()
                    .isDeep() || tile.block() is Cliff
            ) return@circle
            ct.creep = Math.min(ct.creep + amount, 10)
        }
    }

    fun fixedUpdate() {
        // dont update anything if game is paused
        if (!state.isPlaying() || state.serverPaused) return
        var newcount = 0
        for (emitter in creeperEmitters) {
            emitter.fixedUpdate()
            if (emitter.nullified) newcount++
        }
        chargedEmitters.forEach { obj: ChargedEmitter -> obj.fixedUpdate() }
        for (shield in shields) {
            if (shield == null || shield.dead || shield.health <= 0f || shield.healthLeft <= 0f) {
                shields.remove(shield)
                if (shield == null) continue
                Core.app.post(shield::kill)
                val percentage: Float = 1f - shield.healthLeft / (shield.block as ForceProjector).shieldHealth
                depositCreeper(shield.tile, shieldCreeperDropRadius, shieldCreeperDropAmount * percentage)
                continue
            }
            val percentage: Double = shield.healthLeft / (shield.block as ForceProjector).shieldHealth
            Call.label(
                "[" + getTrafficlightColor(percentage) + "]" + (percentage * 100).toInt() + "%" + if (shield.phaseHeat > 0.1f) " [#f4ba6e]\uE86B +" + ((1f - shieldBoostProtectionMultiplier) * 100f).toInt() + "%" else "",
                1f,
                shield.x,
                shield.y
            )
        }
        CreeperUtils.nullifiedCount = newcount
    }

    fun updateCreeper() {
        // dont update anything if game is paused
        if (!state.isPlaying() || state.serverPaused) return

        // update emitters
        for (emitter in creeperEmitters) {
            if (!emitter.update()) {
                creeperEmitters.remove(emitter)
            }
        }
        for (emitter in chargedEmitters) {
            if (!emitter.update()) {
                chargedEmitters.remove(emitter)
                resetDistanceCache()
            }
        }

        // no emitters so game over
        if (creeperEmitters.size === 0
            || CreeperUtils.closestEmitter(world.tile(0, 0)) == null
        ) {
            return
        }

        // update creeper flow
        if (++CreeperUtils.pulseOffset == 64) CreeperUtils.pulseOffset = 0
        for (tile in creeperableTiles) {
            if (tile == null) {
                creeperableTiles.remove(null as Tile?)
                continue
            }

            // spread creep and apply damage
            transferCreeper(tile)
            applyDamage(tile)
            if ((closestEmitterDist(tile) - CreeperUtils.pulseOffset) % 64 == 0) {
                drawCreeper(tile)
            }
        }
    }

    var emitterDst = Array(0) { IntArray(0) }
    fun resetDistanceCache() {
        for (i in emitterDst.indices) { // Don't use enhanced for as that allocates
            for (j in emitterDst[i].indices) {
                val tile: Unit = world.tile(i, j)
                var dst = -1
                val ce = CreeperUtils.closestEmitter(tile)
                val cce = closestChargedEmitter(tile)
                if (ce == null) {
                    if (cce != null) dst = cce.dst(tile) as Int
                } else {
                    dst = ce.dst(tile) as Int
                    if (cce != null) dst = Math.min(ce.dst(tile), cce.dst(tile))
                }
                emitterDst[i][j] = dst
            }
        }
    }

    fun closestEmitterDist(tile: Tile): Int {
        return emitterDst[tile.x][tile.y]
    }

    fun closestEmitter(tile: Tile): Emitter {
        return Geometry.findClosest(tile.getX(), tile.getY(), creeperEmitters)
    }

    fun closestChargedEmitter(tile: Tile): ChargedEmitter {
        return Geometry.findClosest(tile.getX(), tile.getY(), chargedEmitters)
    }

    fun drawCreeper(tile: Tile) {
        Core.app.post {
            if (tile.creep < 1f) {
                return@post
            }
            val currentLvl: Int = creeperLevels.getOrDefault(tile.block(), 11)
            if ((tile.build == null || tile.block().alwaysReplace || tile.build.team === creeperTeam) && currentLvl <= 10 && (currentLvl < tile.creep as Int || currentLvl > tile.creep as Int + 0.1f)) {
                tile.setNet(creeperBlocks.get(Mathf.clamp(tile.creep as Int, 0, 10)), creeperTeam, Mathf.random(0, 3))
            }
        }
    }

    fun applyDamage(tile: Tile) {
        if (tile.build != null && tile.build.team !== creeperTeam && tile.creep > 1f) {
            Core.app.post {
                if (tile.build == null) return@post
                if (Mathf.chance(0.02)) {
                    Call.effect(Fx.bubble, tile.build.x, tile.build.y, 0, creeperTeam.color)
                }
                tile.build.damage(creeperDamage * tile.creep)
                tile.creep *= creeperEvaporationUponDamagePercent
            }
        }
    }

    fun invalidTile(tile: Tile?): Boolean {
        return tile == null
    }

    fun transferCreeper(source: Tile) {
        if (source.build == null || source.creep < 1f) return
        var total = 0f
        for (i in source.build.id until source.build.id + 4) {
            val target: Tile = source.nearby(i % 4)
            if (cannotTransfer(source, target)) continue

            // creeper delta, cannot transfer more than 1/4 source creep or less than 0.001f. Target creep cannot exceed max creep
            val delta: Float = Mathf.clamp(
                (source.creep - target.creep) * transferRate,
                0,
                Math.min(source.creep * transferRate, maxTileCreep - target.creep)
            )
            if (delta > 0.001f) {
                target.creep += delta
                total += delta
            }
        }
        if (total > 0.001f) {
            source.creep -= total
        }
    }

    fun cannotTransfer(source: Tile?, target: Tile?): Boolean {
        if ((source == null || target == null || target.creep >= maxTileCreep || source.creep <= target.creep || target.block() is StaticWall
                    || target.block() is Cliff) || target.floor() != null && (!target.floor().placeableOn || target.floor()
                .isDeep())
        ) {
            return true
        }
        if (source.build != null && source.build.team !== creeperTeam) {
            applyDamage(source)
            return true
        }
        return false
    }
}