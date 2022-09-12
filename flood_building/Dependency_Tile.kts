
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

class Tile {
    var data: Byte = 0
    var build: Building? = null
    var block: Block? = null
    var floor: Floor? = null
    var x: Float = 0f
    var y: Float = 0f
    var creep = 0f
}
