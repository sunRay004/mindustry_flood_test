package flood

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.creeper.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

class ImpactReactor {
    fun updateTile() {
        if (lastFx > (2f - warmup) * 25) {
            lastFx = 0
            if (targetEmitter == null) {
                val core: Emitter = CreeperUtils.closestEmitter(tile)
                if (core != null && within(core, nullifierRange)) {
                    targetEmitter = core
                }
            }
            if (targetEmitter != null && targetEmitter.build != null) {
                Geometry.iterateLine(0f, x, y, targetEmitter.getX(), targetEmitter.getY(), 1f - warmup) { x, y ->
                    Timer.schedule(
                        { Call.effect(Fx.missileTrailShort, x, y, warmup * 3f, Pal.accent) },
                        dst(x, y) / tilesize / 2
                    )
                }
                Call.soundAt(Sounds.dullExplosion, x, y, 1, 1)
                Call.effect(Fx.dynamicSpikes, x, y, warmup * 3f, team.color)
            }
        } else {
            lastFx += 1
        }
        if (efficiency >= 0.9999f && power.status >= 0.99f) {
            val prevOut: Boolean = getPowerProduction() <= consPower.requestedPower(this)
            warmup = Mathf.lerpDelta(warmup, 1f, warmupSpeed * timeScale)
            if (Mathf.equal(warmup, 1f, 0.001f)) {
                warmup = 1f
            }
            if (finFx > (1.1f - warmup) * 50) {
                finFx = 0
                if (targetEmitter != null) {
                    if (Mathf.chance(warmup * 0.1f)) {
                        targetEmitter.build.tile.getLinkedTiles { t ->
                            Call.effect(
                                Fx.mineHuge,
                                t.getX(),
                                t.getY(),
                                warmup,
                                Pal.health
                            )
                        }
                        Call.effect(Fx.smokeCloud, x + Mathf.range(0, 36), y + Mathf.range(0, 36), 1f, Pal.gray)
                        Call.soundAt(
                            if (Mathf.chance(0.7f)) Sounds.flame2 else Sounds.flame,
                            x,
                            y,
                            0.8f,
                            Mathf.range(0.8f, 1.5f)
                        )
                    }
                }
            } else {
                finFx += 1
            }
            if (targetEmitter != null && Mathf.equal(warmup, 1f, 0.01f)) {
                Call.effect(Fx.massiveExplosion, x, y, 2f, Pal.accentBack)
                creeperEmitters.remove(targetEmitter)
                Call.effect(Fx.shockwave, x, y, 16f, Pal.accent)
                Call.soundAt(Sounds.corexplode, x, y, 1.2f, 1f)
                val build: Building = targetEmitter.build
                val block: Block = build.block
                val target: Tile = build.tile
                build.kill()
                if (state.rules.coreCapture) {
                    target.setNet(block, team(), 0)
                    Call.effect(Fx.placeBlock, target.getX(), target.getY(), block.size, team().color)
                }
                targetEmitter = null
                Core.app.post(this::kill)
            }
            if (!prevOut && getPowerProduction() > consPower.requestedPower(this)) {
                Events.fire(Trigger.impactPower)
            }
            if (timer(timerUse, itemDuration / timeScale)) {
                consume()
            }
        } else {
            warmup = Mathf.lerpDelta(warmup, 0f, 0.01f)
        }
        totalProgress += warmup * Time.delta
        productionEfficiency = Mathf.pow(warmup, 5f)
    }
}

/*
ERROR Unresolved reference: creeper (flood_impact_reactor.kts:10:18)
ERROR Unresolved reference: lastFx (flood_impact_reactor.kts:23:13)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:23:28)
ERROR Unresolved reference: lastFx (flood_impact_reactor.kts:24:13)
ERROR Unresolved reference: targetEmitter (flood_impact_reactor.kts:25:17)
ERROR Unresolved reference: Emitter (flood_impact_reactor.kts:26:27)
ERROR Unresolved reference: CreeperUtils (flood_impact_reactor.kts:26:37)
ERROR Unresolved reference: tile (flood_impact_reactor.kts:26:65)
ERROR Unresolved reference: within (flood_impact_reactor.kts:27:37)
ERROR Unresolved reference: nullifierRange (flood_impact_reactor.kts:27:50)
ERROR Unresolved reference: targetEmitter (flood_impact_reactor.kts:28:21)
ERROR Unresolved reference: targetEmitter (flood_impact_reactor.kts:31:17)
ERROR Unresolved reference: targetEmitter (flood_impact_reactor.kts:31:42)
ERROR Unresolved reference: x (flood_impact_reactor.kts:32:42)
ERROR Unresolved reference: y (flood_impact_reactor.kts:32:45)
ERROR Unresolved reference: targetEmitter (flood_impact_reactor.kts:32:48)
ERROR Unresolved reference: targetEmitter (flood_impact_reactor.kts:32:70)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:32:97)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:34:67)
ERROR Unresolved reference: dst (flood_impact_reactor.kts:35:25)
ERROR Unresolved reference: dullExplosion (flood_impact_reactor.kts:38:37)
ERROR Unresolved reference: x (flood_impact_reactor.kts:38:52)
ERROR Unresolved reference: y (flood_impact_reactor.kts:38:55)
ERROR The integer literal does not conform to the expected type Float (flood_impact_reactor.kts:38:58)
ERROR The integer literal does not conform to the expected type Float (flood_impact_reactor.kts:38:61)
ERROR Unresolved reference: x (flood_impact_reactor.kts:39:47)
ERROR Unresolved reference: y (flood_impact_reactor.kts:39:50)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:39:53)
ERROR Unresolved reference: team (flood_impact_reactor.kts:39:66)
ERROR Unresolved reference: lastFx (flood_impact_reactor.kts:42:13)
ERROR Unresolved reference: += (flood_impact_reactor.kts:42:20)
ERROR Unresolved reference: efficiency (flood_impact_reactor.kts:44:13)
ERROR Unresolved reference: power (flood_impact_reactor.kts:44:38)
ERROR Unresolved reference: getPowerProduction (flood_impact_reactor.kts:45:36)
ERROR Unresolved reference: consPower (flood_impact_reactor.kts:45:60)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:46:13)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:46:38)
ERROR Unresolved reference: warmupSpeed (flood_impact_reactor.kts:46:50)
ERROR Unresolved reference: timeScale (flood_impact_reactor.kts:46:64)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:47:29)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:48:17)
ERROR Unresolved reference: finFx (flood_impact_reactor.kts:50:17)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:50:33)
ERROR Unresolved reference: finFx (flood_impact_reactor.kts:51:17)
ERROR Unresolved reference: targetEmitter (flood_impact_reactor.kts:52:21)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:53:38)
ERROR Unresolved reference: targetEmitter (flood_impact_reactor.kts:54:25)
ERROR Cannot infer a type for this parameter. Please specify it explicitly. (flood_impact_reactor.kts:54:67)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:59:33)
ERROR Unresolved reference: x (flood_impact_reactor.kts:63:52)
ERROR The integer literal does not conform to the expected type Float (flood_impact_reactor.kts:63:68)
ERROR The integer literal does not conform to the expected type Float (flood_impact_reactor.kts:63:71)
ERROR Unresolved reference: y (flood_impact_reactor.kts:63:76)
ERROR The integer literal does not conform to the expected type Float (flood_impact_reactor.kts:63:92)
ERROR The integer literal does not conform to the expected type Float (flood_impact_reactor.kts:63:95)
ERROR The floating-point literal does not conform to the expected type Double (flood_impact_reactor.kts:65:46)
ERROR Unresolved reference: x (flood_impact_reactor.kts:66:29)
ERROR Unresolved reference: y (flood_impact_reactor.kts:67:29)
ERROR Unresolved reference: finFx (flood_impact_reactor.kts:74:17)
ERROR Unresolved reference: += (flood_impact_reactor.kts:74:23)
ERROR Unresolved reference: targetEmitter (flood_impact_reactor.kts:76:17)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:76:54)
ERROR Unresolved reference: x (flood_impact_reactor.kts:77:50)
ERROR Unresolved reference: y (flood_impact_reactor.kts:77:53)
ERROR Unresolved reference: creeperEmitters (flood_impact_reactor.kts:78:17)
ERROR Unresolved reference: targetEmitter (flood_impact_reactor.kts:78:40)
ERROR Unresolved reference: x (flood_impact_reactor.kts:79:43)
ERROR Unresolved reference: y (flood_impact_reactor.kts:79:46)
ERROR Unresolved reference: x (flood_impact_reactor.kts:80:49)
ERROR Unresolved reference: y (flood_impact_reactor.kts:80:52)
ERROR Unresolved reference: targetEmitter (flood_impact_reactor.kts:81:39)
ERROR Unresolved reference: team (flood_impact_reactor.kts:86:42)
ERROR Type mismatch: inferred type is Int but Float was expected (flood_impact_reactor.kts:87:78)
ERROR Unresolved reference: team (flood_impact_reactor.kts:87:90)
ERROR Unresolved reference: targetEmitter (flood_impact_reactor.kts:89:17)
ERROR Unresolved reference: kill (flood_impact_reactor.kts:90:37)
ERROR Unresolved reference: getPowerProduction (flood_impact_reactor.kts:92:29)
ERROR Unresolved reference: consPower (flood_impact_reactor.kts:92:52)
ERROR Unresolved reference: timer (flood_impact_reactor.kts:95:17)
ERROR Unresolved reference: timerUse (flood_impact_reactor.kts:95:23)
ERROR Unresolved reference: itemDuration (flood_impact_reactor.kts:95:33)
ERROR Unresolved reference: timeScale (flood_impact_reactor.kts:95:48)
ERROR Unresolved reference: consume (flood_impact_reactor.kts:96:17)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:99:13)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:99:38)
ERROR Unresolved reference: totalProgress (flood_impact_reactor.kts:101:9)
ERROR Unresolved reference: += (flood_impact_reactor.kts:101:23)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:101:26)
ERROR Unresolved reference: productionEfficiency (flood_impact_reactor.kts:102:9)
ERROR Unresolved reference: warmup (flood_impact_reactor.kts:102:42)
 */
