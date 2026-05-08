package Vivado

import chisel3._
import org.chipsalliance.cde.config.{Config, Parameters}
import freechips.rocketchip.devices.debug.DebugModuleKey
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.tile.{BuildRoCC, OpcodeSet}
import freechips.rocketchip.util.DontTouch
import freechips.rocketchip.system._
import freechips.rocketchip.rocket._

class RocketSystem(implicit p: Parameters) extends RocketSubsystem
    with HasAsyncExtInterrupts
    with CanHaveMasterAXI4MemPort
    with CanHaveMasterAXI4MMIOPort
    with CanHaveSlaveAXI4Port
{
  val bootROM  = p(BootROMLocated(location)).map { BootROM.attach(_, this, CBUS) }
  override lazy val module = new RocketSystemModuleImp(this)
}

class RocketSystemModuleImp[+L <: RocketSystem](_outer: L) extends RocketSubsystemModuleImp(_outer)
    with HasRTCModuleImp
    with HasExtInterruptsModuleImp
    with DontTouch

// class WithGemmini(mesh_size: Int, bus_bits: Int) extends Config((site, here, up) => {
//   case BuildRoCC => up(BuildRoCC) ++ Seq(
//     (p: Parameters) => {
//       implicit val q = p
//       implicit val v = implicitly[ValName]
//       LazyModule(new gemmini.Gemmini(gemmini.GemminiConfigs.defaultConfig.copy(
//         meshRows = mesh_size, meshColumns = mesh_size, dma_buswidth = bus_bits)))
//     }
//   )
//   case SystemBusKey => up(SystemBusKey).copy(beatBytes = bus_bits/8)
// })

class WithDebugProgBuf(prog_buf_words: Int, imp_break: Boolean) extends Config((site, here, up) => {
  case DebugModuleKey => up(DebugModuleKey, site).map(_.copy(nProgramBufferWords = prog_buf_words, hasImplicitEbreak = imp_break))
})

/*----------------- 32-bit RocketChip ---------------*/
/* Note: Linux not supported yet on 32-bit cores     */

/* 32-bit config, max memory 2GB */
class Rocket32BaseConfig extends Config(
  new WithBootROMFile("workspace/bootrom.img") ++
  new WithExtMemSize(0x80000000L) ++
  new WithNExtTopInterrupts(8) ++
  new WithDTS("freechips,rocketchip-vivado", Nil) ++
  new WithDebugSBA ++
  new WithEdgeDataBits(64) ++
  new WithCoherentBusTopology ++
  new WithoutTLMonitors ++
  new BaseConfig)

class Rocket32s1 extends Config(
  new WithNBreakpoints(8) ++
  new WithNSmallCores(1)  ++
  new WithRV32            ++
  new Rocket32BaseConfig)

class Rocket32s2 extends Config(
  new WithNBreakpoints(8) ++
  new WithNSmallCores(2)  ++
  new WithRV32            ++
  new Rocket32BaseConfig)

/* With exposed JTAG port */
class Rocket32s2j extends Config(
  new WithNBreakpoints(8) ++
  new WithJtagDTM         ++
  new WithNSmallCores(2)  ++
  new WithRV32            ++
  new Rocket32BaseConfig)

class Rocket32s4 extends Config(
  new WithNBreakpoints(8) ++
  new WithNSmallCores(4)  ++
  new WithRV32            ++
  new Rocket32BaseConfig)

class Rocket32s8 extends Config(
  new WithNBreakpoints(8) ++
  new WithNSmallCores(8)  ++
  new WithRV32            ++
  new Rocket32BaseConfig)

class Rocket32s16 extends Config(
  new WithNBreakpoints(8) ++
  new WithNSmallCores(16) ++
  new WithRV32            ++
  new Rocket32BaseConfig)

/*----------------- 64-bit RocketChip ---------------*/

/*
 * WithExtMemSize(0x380000000L) = 14GB (16GB minus 2GB for IO) is max supported by the base config.
 * Actual memory size depends on the target board.
 * The Makefile changes the size to correct value during build.
 * It also sets right core clock frequency.
 */
class RocketBaseConfig extends Config(
  new WithBootROMFile("workspace/bootrom.img") ++
  new WithExtMemSize(0x380000000L) ++
  new WithNExtTopInterrupts(8) ++
  new WithDTS("freechips,rocketchip-vivado", Nil) ++
  new WithDebugSBA ++
  new WithEdgeDataBits(64) ++
  new WithCoherentBusTopology ++
  new WithoutTLMonitors ++
  new BaseConfig)

class RocketWideBusConfig extends Config(
  new WithBootROMFile("workspace/bootrom.img") ++
  new WithExtMemSize(0x380000000L) ++
  new WithNExtTopInterrupts(8) ++
  new WithDTS("freechips,rocketchip-vivado", Nil) ++
  new WithDebugSBA ++
  new WithEdgeDataBits(256) ++
  new WithCoherentBusTopology ++
  new WithoutTLMonitors ++
  new BaseConfig)

class Rocket64b1 extends Config(
  new WithNBreakpoints(8) ++
  new WithNBigCores(1)    ++
  new RocketBaseConfig)

class Rocket64b2 extends Config(
  new WithNBreakpoints(8) ++
  new WithNBigCores(2)    ++
  new RocketBaseConfig)

/* With exposed BSCAN port - the name must end with 'e' */
/* With up to 256GB memory */
/* Note: lower 2GB are used for memory mapped IO, so max usable RAM size is 254GB */
class Rocket64b2e extends Config(
  new WithNBreakpoints(8) ++
  new WithNBigCores(2)    ++
  new WithExtMemSize(0x3f80000000L) ++
  new RocketBaseConfig)

/* With up to 256GB memory */
/* Note: lower 2GB are used for memory mapped IO, so max usable RAM size is 254GB */
class Rocket64b2m extends Config(
  new WithNBreakpoints(8) ++
  new WithNBigCores(2)    ++
  new WithExtMemSize(0x3f80000000L) ++
  new RocketBaseConfig)

/* With up to 256GB memory, 2 memory channels, L2 cache and wide memory bus */
class Rocket64b2m2 extends Config(
  new WithNBreakpoints(8) ++
  new WithNBigCores(2)    ++
  new WithExtMemSize(0x3f80000000L) ++
  new WithNMemoryChannels(2) ++
  new WithNBanks(4) ++ 
  new WithInclusiveCache ++
  new RocketWideBusConfig)

/* With up to 256GB memory, 4 memory channels, L2 cache and wide memory bus */
class Rocket64b4m4 extends Config(
  new WithNBreakpoints(8) ++
  new WithNBigCores(4)    ++
  new WithExtMemSize(0x3f80000000L) ++
  new WithNMemoryChannels(4) ++
  new WithNBanks(8) ++ 
  new WithInclusiveCache ++
  new RocketWideBusConfig)

/* With exposed JTAG port */
class Rocket64b2j extends Config(
  new WithNBreakpoints(8) ++
  new WithJtagDTM         ++
  new WithNBigCores(2)    ++
  new RocketBaseConfig)

/* Smaller debug module */
class Rocket64b2d1 extends Config(
  new WithNBreakpoints(1) ++
  new WithNBigCores(2)    ++
  new WithDebugProgBuf(1, true) ++
  new RocketBaseConfig)

/* Smaller debug module */
class Rocket64b2d2 extends Config(
  new WithNBreakpoints(2) ++
  new WithNBigCores(2)    ++
  new WithDebugProgBuf(2, true) ++
  new RocketBaseConfig)

/* Smaller debug module */
class Rocket64b2d3 extends Config(
  new WithNBreakpoints(3) ++
  new WithNBigCores(2)    ++
  new WithDebugProgBuf(2, false) ++
  new RocketBaseConfig)

/* With 512KB level 2 cache */
/* Note: adding L2 cache reduces max CPU clock frequency */
class Rocket64b2l2 extends Config(
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new WithNBigCores(2)    ++
  new RocketBaseConfig)

/* With Gemmini 4x4 and 2 small cores */
/* Note: small core has no MMU and cannot boot mainstream Linux */
// class Rocket64s2gem4 extends Config(
//   new WithGemmini(4, 64)  ++
//   new WithInclusiveCache  ++
//   new WithNBreakpoints(8) ++
//   new WithNSmallCores(2)  ++
//   new RocketBaseConfig)

/* With Gemmini 4x4 and 2 medium cores */
/* Note: cannot get medium core to boot Linux: Oops - illegal instruction */
// class Rocket64m2gem4 extends Config(
//   new WithGemmini(4, 64)  ++
//   new WithInclusiveCache  ++
//   new WithNBreakpoints(8) ++
//   new WithNMedCores(2)    ++
//   new RocketBaseConfig)

/* With Gemmini 4x4 */
// class Rocket64b1gem4 extends Config(
//   new WithGemmini(4, 64)  ++
//   new WithInclusiveCache  ++
//   new WithNBreakpoints(8) ++
//   new WithNBigCores(1)    ++
//   new RocketBaseConfig)

/* With Gemmini 8x8 */
// class Rocket64b1gem8 extends Config(
//   new WithGemmini(8, 64)  ++
//   new WithInclusiveCache  ++
//   new WithNBreakpoints(8) ++
//   new WithNBigCores(1)    ++
//   new RocketBaseConfig)

/* With Gemmini 16x16 */
// class Rocket64b1gem16 extends Config(
//   new WithGemmini(16, 64) ++
//   new WithInclusiveCache  ++
//   new WithNBreakpoints(8) ++
//   new WithNBigCores(1)    ++
//   new RocketBaseConfig)

/* With Gemmini 4x4, 2 big cores */
// class Rocket64b2gem4 extends Config(
//   new WithGemmini(4, 64)  ++
//   new WithInclusiveCache  ++
//   new WithNBreakpoints(8) ++
//   new WithNBigCores(2)    ++
//   new RocketBaseConfig)

/* With Gemmini 8x8, 2 big cores */
// class Rocket64b2gem8 extends Config(
//   new WithGemmini(8, 64)  ++
//   new WithInclusiveCache  ++
//   new WithNBreakpoints(8) ++
//   new WithNBigCores(2)    ++
//   new RocketBaseConfig)

/* With Gemmini 16x16, 2 big cores */
// class Rocket64b2gem16 extends Config(
//   new WithGemmini(16, 64) ++
//   new WithInclusiveCache  ++
//   new WithNBreakpoints(8) ++
//   new WithNBigCores(2)    ++
//   new RocketBaseConfig)

class Rocket64b4 extends Config(
  new WithNBreakpoints(8) ++
  new WithNBigCores(4)    ++
  new RocketBaseConfig)

/* With level 2 cache and wide memory bus */
class Rocket64b4l2w extends Config(
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new WithNBigCores(4)    ++
  new RocketWideBusConfig)

class Rocket64b8 extends Config(
  new WithNBreakpoints(8) ++
  new WithNBigCores(8)    ++
  new RocketBaseConfig)

class Rocket64b16m extends Config(
  new WithNBreakpoints(4) ++
  new WithNBigCores(16)   ++
  new WithExtMemSize(0x3f80000000L) ++
  new RocketBaseConfig)

class Rocket64b24m extends Config(
  new WithNBreakpoints(4) ++
  new WithNBigCores(24)   ++
  new WithExtMemSize(0x3f80000000L) ++
  new RocketBaseConfig)

class Rocket64b32m extends Config(
  new WithNBreakpoints(4) ++
  new WithNBigCores(32)   ++
  new WithExtMemSize(0x3f80000000L) ++
  new RocketBaseConfig)

/* Without slave port - for use in HDL simulation */
class Rocket64b2s extends Config(
  new WithNBigCores(2)    ++
  new WithBootROMFile("workspace/bootrom.img") ++
  new WithExtMemSize(0x40000000) ++
  new WithNExtTopInterrupts(8) ++
  new WithEdgeDataBits(64) ++
  new WithCoherentBusTopology ++
  new WithoutTLMonitors ++
  new WithNoSlavePort ++
  new BaseConfig)

/*----------------- Sonic BOOM   ---------------*/

class Rocket64w1 extends Config(
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithNSmallBooms(1) ++
  new RocketBaseConfig)

class Rocket64x1 extends Config(
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithNMediumBooms(1) ++
  new RocketWideBusConfig)

// tag 参照了 gem5 tage 的配置，但我不想把 ghist 拉到 130 bits，就仍然使用 6 个 table
class Gem5TageConfig extends boom.v3.common.TAGEConfig(boom.v3.ifu.BoomTageParams(
    tableInfo = Seq(
      (  256,       2,     9),
      (  256,       4,     9),
      (  256,       8,     10),
      (  256,      16,     10),
      (  256,      32,     11),
      (  256,      64,     11)
    )
  )) 

/* hardware overhead comparisons used in the paper */

class CoupledLatencyBus128LoopConfig extends Config(
  // new boom.v3.common.DisableEventCounter ++
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyBus128LoopPf4Config extends Config(
  // new boom.v3.common.DisableEventCounter ++
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/* coupled final: final means 13KB BTB, 32KB DCache, 16 ways DTLB, Bus width 128, loop preditor, 50M Freq */

class LatencyUnmodifiedBoom extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class UnmodifiedBoom extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledLatencyRASFixFinalDTLB64 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 64) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledLatencyRASFixFinalDTLB32 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 32) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledLatencyRASFixFinal extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)


class CoupledLatencyFinal extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/* true base configs: true_coupled_baseline 分支的 configs */

class CoupledLatencyBus128LoopFTQ48IC32F50 extends Config(
  new boom.v3.common.FTQConfigs(48) ++
  new boom.v3.common.ICacheConfigs(8) ++
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledLatencyBus128LoopIC32F50 extends Config(
  new boom.v3.common.ICacheConfigs(8) ++
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledLatencyBus128LoopFTQ48F50 extends Config(
  new boom.v3.common.FTQConfigs(48) ++
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledLatencyBus128LoopOnlyDTLB16F50 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledLatencyBus128LoopBTB256DTLB16F50 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledLatencyBus128LoopDTLB16F50 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledLatencyBus128DTLB16F50 extends Config(
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledLatencyDTLB16F50 extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledLatencyDTLB64F50 extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 64) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledDTLB64 extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 64) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledRestoreGhist extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledTAGE256 extends Config(
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledF2BTB256 extends Config(
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class CoupledDCache64KDTLB32 extends Config(
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/* decoupled final: final means 13KB BTB, 32KB DCache, 16 ways DTLB, Bus width 128, loop preditor, 50M Freq */

class LatencyFinalDTLB64Dist8Pf4 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 64) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyFinalDTLB32Dist8Pf4 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

// class LatencyFinalNoPf extends Config(
//   new boom.v3.common.EnablePfConfig(false) ++
//   new boom.v3.common.UseLoopConfig(true) ++
//   new boom.v3.common.BusWidthConfigs(128) ++
//   new boom.v3.common.EnableBoomFlushGHistRestore ++
//   new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
//   new boom.v3.common.DCacheConfigs(64, 8, 16) ++
//   new boom.v3.common.PfMSHRNumber(0) ++
//   new WithInclusiveCache  ++
//   new WithNBreakpoints(8) ++
//   new boom.v3.common.WithMyMediumBooms(1) ++
//   new RocketWideBusConfig)

class LatencyFinalPf0 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(0) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyFinalPf4 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyFinalPf3 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(3) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyFinalPf2 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(2) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyFinalPf1 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyFinalDist16Pf4 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.LimitPrefetchDist(16) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyFinalDist12Pf4 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.LimitPrefetchDist(12) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyFinalDist8Pf4 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/* ftq and icache configs */

class LatencyBus128LoopFTQ48IC32Dist8Pf4F50 extends Config(
  new boom.v3.common.FTQConfigs(48) ++
  new boom.v3.common.ICacheConfigs(8) ++
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyBus128LoopIC32Dist8Pf4F50 extends Config(
  new boom.v3.common.ICacheConfigs(8) ++
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyBus128LoopFTQ48Dist8Pf4F50 extends Config(
  new boom.v3.common.FTQConfigs(48) ++
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/**
  * 目前确定的必需的修改：
  * DCache 16KB -> 32KB
  * DTLB 8 -> 16
  * BTB 128 -> 256
  * 总线宽度 64 -> 128
  */

class LatencyBus128LoopBTB256DTLB16Pf4F50 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyBus128LoopBTB256DTLB16Pf3F50 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(3) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyBus128LoopBTB256DTLB16Pf2F50 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(2) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyBus128LoopBTB256DTLB16Pf1F50 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyBus128LoopBTB256DTLB16Pf0F50 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(0) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyBus128LoopBTB256DTLB16Dist8Pf4F50 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/* loop: 尝试加回 loop predictor */

class LatencyBus128LoopOnlyDTLB16Dist8Pf4F50 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyBus128LoopDTLB16Dist8Pf4F50 extends Config(
  new boom.v3.common.UseLoopConfig(true) ++
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyBus128DTLB16SimplifyCommDist8Pf4F50 extends Config(
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/* configs：尝试 128bits 的总线宽度 */

class LatencyBus128DTLB16Pf1F50 extends Config(
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyBus128DTLB16Pf4F50 extends Config(
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyBus128DTLB16Dist8Pf4F50 extends Config(
  new boom.v3.common.BusWidthConfigs(128) ++
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyDTLB16Pf1F50 extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyDTLB16Pf4F50 extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyDTLB16Dist8Pf4F50 extends Config(
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(64, 8, 16) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/* configs: 将 DTLB 的大小扩大到 64 */

class LatencyDTLB64Dist8Pf4F50 extends Config(
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 64) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyDTLB64Pf4F50 extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 64) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class LatencyDTLB64Pf1F50 extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 64) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class DTLB64Dist8Pf4F50 extends Config(
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 64) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class DTLB64Dist8Pf4 extends Config(
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 64) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class DTLB64Pf1 extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 64) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/* configs: 控制 prefetch 距离的测试 */

// 默认比较的配置延续 RestoreGhistPf1

class Dist8Pf1 extends Config(
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class Dist12Pf1 extends Config(
  new boom.v3.common.LimitPrefetchDist(12) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class Dist16Pf1 extends Config(
  new boom.v3.common.LimitPrefetchDist(16) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class Dist8Pf2 extends Config(
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(2) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class Dist12Pf2 extends Config(
  new boom.v3.common.LimitPrefetchDist(12) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(2) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class Dist16Pf2 extends Config(
  new boom.v3.common.LimitPrefetchDist(16) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(2) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class Dist8Pf3 extends Config(
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(3) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class Dist12Pf3 extends Config(
  new boom.v3.common.LimitPrefetchDist(12) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(3) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class Dist16Pf3 extends Config(
  new boom.v3.common.LimitPrefetchDist(16) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(3) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class Dist8Pf4 extends Config(
  new boom.v3.common.LimitPrefetchDist(8) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class Dist12Pf4 extends Config(
  new boom.v3.common.LimitPrefetchDist(12) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class Dist16Pf4 extends Config(
  new boom.v3.common.LimitPrefetchDist(16) ++
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/* configs: rob flush restore ghist 配置测试部分 */

// 默认比较的配置延续 TAGE256Pf1

class RestoreGhistPf0 extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(0) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class RestoreGhistPf1 extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class RestoreGhistPf2 extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(2) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class RestoreGhistPf3 extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(3) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class RestoreGhistPf4 extends Config(
  new boom.v3.common.EnableBoomFlushGHistRestore ++
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/* configs: tage 配置测试部分 */

// 默认比较的配置延续 F2BTB256Set256EBTBPf1

class TAGE256Pf0 extends Config(
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(0) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class TAGE256Pf1 extends Config(
  new Gem5TageConfig ++
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)


/* configs: f2 btb 配置测试部分 */

// 默认比较的配置延续 DCache64KDTLB32WPf1Config

class F2BTB256Set256EBTBPf0 extends Config(
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(0) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class F2BTB256Set256EBTBPf1 extends Config(
  new boom.v3.common.BTBConfig(256, 2, 21, 256) ++
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/*  configs: dcache 配置测试部分 */

class DCacheDefaultPf1Config extends Config(
  new boom.v3.common.DCacheConfigs(64, 4, 8) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class DCache64KDTLB32WPf0Config extends Config(
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(0) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class DCache64KDTLB32WPf1Config extends Config(
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class DCache64KDTLB32WPf2Config extends Config(
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(2) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class DCache64KDTLB32WPf3Config extends Config(
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(3) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class DCache64KDTLB32WPf4Config extends Config(
  new boom.v3.common.DCacheConfigs(128, 8, 32) ++
  new boom.v3.common.PfMSHRNumber(4) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/* END */

class MyRocketConfig extends Config(
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class PfMSHR0Config extends Config(
  new boom.v3.common.PfMSHRNumber(0) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class PfMSHR1Config extends Config(
  new boom.v3.common.PfMSHRNumber(1) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class PfMSHR2Config extends Config(
  new boom.v3.common.PfMSHRNumber(2) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class PfMSHR3Config extends Config(
  new boom.v3.common.PfMSHRNumber(3) ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)
/*
class NoPredictorConfig extends Config(
  new boom.v3.common.DisableBoomBranchPredictor ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

class FullMSHRSkipConfig extends Config(
  new boom.v3.common.EnableBoomFullMSHRSkip ++
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)
*/
class MyRocketConfigTest extends Config(
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithMyMediumBooms(1) ++
  new RocketWideBusConfig)

/* Note: multi-core BOOM appears unstable */
class Rocket64x2 extends Config(
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithNMediumBooms(2) ++
  new RocketWideBusConfig)

class Rocket64x4 extends Config(
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithNMediumBooms(4) ++
  new RocketWideBusConfig)

class Rocket64x8 extends Config(
  new WithInclusiveCache  ++
  new WithNBreakpoints(4) ++
  new boom.v3.common.WithNMediumBooms(8) ++
  new RocketWideBusConfig)

class Rocket64x12 extends Config(
  new WithInclusiveCache  ++
  new WithNBreakpoints(4) ++
  new boom.v3.common.WithNMediumBooms(12) ++
  new RocketWideBusConfig)

/* With up to 256GB memory, 4 memory channels */
/* Note: lower 2GB are used for memory mapped IO, so max usable RAM size is 254GB */
class Rocket64x12m4 extends Config(
  new WithNBreakpoints(4) ++
  new boom.v3.common.WithNMediumBooms(12) ++
  new WithExtMemSize(0x3f80000000L) ++
  new WithNMemoryChannels(4) ++
  new WithNBanks(8) ++ 
  new WithInclusiveCache ++
  new RocketWideBusConfig)

/* Note: 3-way BOOM appears unstable */
class Rocket64y1 extends Config(
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithNLargeBooms(1) ++
  new RocketWideBusConfig)

/* Note: 4-way BOOM appears unstable */
class Rocket64z1 extends Config(
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithNMegaBooms(1) ++
  new RocketWideBusConfig)

/* With up to 256GB memory */
/* Note: lower 2GB are used for memory mapped IO, so max usable RAM size is 254GB */
/* Note: 4-way BOOM appears unstable */
class Rocket64z2m extends Config(
  new WithInclusiveCache  ++
  new WithNBreakpoints(8) ++
  new boom.v3.common.WithNMegaBooms(2) ++
  new WithExtMemSize(0x3f80000000L) ++
  new RocketWideBusConfig)
