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

var creeperBlocks: HashMap<Int, Block> = HashMap<Int, Block>() // 每层对应方块设定
var creeperLevels: HashMap<Block, Integer> = HashMap() //层数
var creeperTeam: Team = Team.blue // 洪水队伍

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
        creeperLevels.put(set.getValue(), set.getKey())
    }
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

/*
WARNING This class shouldn't be used in Kotlin. Use kotlin.Int instead. (flood_water.kts:20:35)
ERROR Expression 'entrySet' of type '(Mutable)Set<(Mutable)Map.(Mutable)Entry<Int!, Block!>!>!' cannot be invoked as a function. The function 'invoke()' is not found (flood_water.kts:49:31)
ERROR Cannot access 'entrySet': it is package-private in 'HashMap' (flood_water.kts:49:31)
WARNING Variable 'newFlags' is never used (flood_water.kts:50:13)
ERROR Unresolved reference: creep (flood_water.kts:57:18)
ERROR Type mismatch: inferred type is Integer but Int was expected (flood_water.kts:60:31)
ERROR The integer literal does not conform to the expected type Integer (flood_water.kts:60:72)
ERROR Unresolved reference: creep (flood_water.kts:61:141)
ERROR Unresolved reference: creep (flood_water.kts:61:175)
ERROR Unresolved reference: creep (flood_water.kts:62:60)
*/
