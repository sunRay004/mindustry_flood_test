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

val updateInterval = 0.03f // 基本更新间隔（秒）
val transferRate = 0.25f // 基本转移率  注：保持在0.25f以下
val creeperDamage = 0.2f // 基础淹没损伤
val creeperEvaporationUponDamagePercent = 0.98f // 在损坏某物时会保留的水深百分比
val creeperUnitDamage = 2f //洪水单位伤害
val maxTileCreep = 10.5f //最大瓷砖水深
val creeperBlockDamageMultiplier = 0.75f //洪水方块伤害倍率

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
var sporeType: BulletType = Bullets.placeholder // 孢子种类
var sporeMaxRangeMultiplier = 30f //孢子最大射程倍率
var sporeAmount = 20f //孢子数量
var sporeRadius = 5f //孢子半径
var sporeSpeedMultiplier = 0.15f //孢子速度倍率
var sporeHealthMultiplier = 10f //孢子生命倍率
var sporeTargetOffset = 256f //孢子方向偏移
var unitShieldDamageMultiplier = 1.5f // 单位力墙伤害倍率
var buildShieldDamageMultiplier = 1.5f //建筑力墙伤害倍率
var shieldBoostProtectionMultiplier = 0.5f // 屏蔽助推保护倍率
var shieldCreeperDropAmount = 7f // 屏蔽洪水量
var shieldCreeperDropRadius = 4f // 屏蔽洪水半径
var nullifierRange: Float = 16f * tilesize // 抵消范围
var radarBeamDamage = 300f // 雷达光束对单位造成的伤害
var creepTowerDeposit = 0.3f // 蠕变塔每tick累积的蠕变量
var creepTowerRange = 300f // 略大于浪涌的范围
var nullifyDamage = 1500f // 核心suspended所需伤害
var nullifyTimeout = 180f // 核心保持suspended的tick数（在施加足够的伤害后重置）
var nullificationPeriod = 10f // 为了游戏结束，所有核心必须被suspended多少秒
var tutorialID = 0
val nullifiedCount = 0 //无效计数
var pulseOffset = 0 // 脉冲偏移
var creeperTeam: Team = Team.blue // 洪水队伍
var creeperBlocks: HashMap<Int, Block> = HashMap<Int, Block>() // 每层对应方块设定
/*
var creeperLevels: HashMap<Block, Integer> = HashMap() //层数
var creeperEmitters: Seq<Emitter> = Seq() //泉眼
var chargedEmitters: Seq<ChargedEmitter> = Seq() //可变泉眼
 */
var creeperableTiles: Seq<Tile> = Seq() //可淹没瓷砖
var shields: Seq<ForceProjector.ForceBuild> = Seq() //盾
val tutContinue = arrayOf(arrayOf("[#49e87c]\uE829 继续[]"))
val tutFinal = arrayOf(arrayOf("[#49e87c]\uE829 关闭[]"))
val tutStart =
    arrayOf(arrayOf("[#49e87c]\uE875 学习教程 []"), arrayOf("[#e85e49]跳过（不推荐） []"))
val tutEntries = arrayOf(
    "[accent]\uE875[] 教程 1/6", "在[#e056f0]\uE83B洪水模式[]中，[scarlet]无需防御敌军单位[]\n而是消灭全部的[accent]泉眼[]包括[accent]地方核心、发射台和加速器[]",
    "[accent]\uE875[] 教程 2/6", "[scarlet] 当心！[]\n[accent]泉眼[]会生成[#e056f0]\uE83B洪水[]。洪水会接近并伤害到附近的友方建筑的单位",
    "[accent]\uE875[] 教程 3/6", "[scarlet]当心！[]\n[accent]充能泉眼[]生成 [#e056f0]\uE83B洪水[]十分短暂，但洪水蔓延速度会快得多",
    "[accent]\uE875[] 教程 4/6", "你可以通过不间断的攻击[accent]抑制泉眼[]，破坏它们则需要摧毁[accent]充能泉眼[]",
    "[accent]\uE875[] 教程 5/6", "如果完全抑制了[accent]泉眼[]，你还可以在其周围建造一个\uF871[accent]冲击反应堆[]并激活，使泉眼[accent]无效化[]",
    "[accent]\uE875[] 教程 6/6", "如果[accent]泉眼[]周围环绕了最高级的洪水方块，就会开始[stat]升级[]你可以抑制它们以中止升级",
    "[white]\uF872[]", "[accent]孢子发射器[]\n[accent]钍反应堆[]会发射长距离的冲击炮弹并产生[accent]巨量洪水[]你可以用\uF80E裂解光束防御",
    "[white]\uF682[]", "[accent]洪水投影器[]\n[accent]震爆塔[]会在附近方块旁迅速放下洪水,使用与炮塔射击方向不同的[accent]方向[].\n射程比浪涌略大",
    "[white]\uF6AD[]", "[accent]洪水雷达[]\n[accent]雷达[]会瞄准附近单位，充能一小段时间之后向此单位[accent]射击[],使用与单位射击方向不同的[accent]方向[].\n射程略大于浪涌",
    "[white]\uF898[]", "[accent]洪水力墙[]\n[accent]力墙投影器[]和[accent]单位的护盾[]会主动吸收[#e056f0]洪水[]，但是吸收满后会[accent]爆炸[]",
    "[white]\uF7FA[]", "[accent]洪水爬爬[]\n[accent]爬爬单位[]会在接触到友方方块后爆炸,并释放大量[#e056f0]洪水[].",
    "[white]\uF7F5[]", "[accent]天垠[]免疫洪水，但[orange]不能对方块造成伤害[]可以用它们在洪水之上运输[accent]资源[]。它们不免疫泉眼和孢子发射器"
)