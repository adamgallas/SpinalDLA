package controller

import spinal.core._
import spinal.lib._
import util.{BarrelShifter, LoopsCntGen}

import scala.language.postfixOps

class ParallelTransposeCtrl[T <: Data](dataType: HardType[T], mem: Mem[T], channels: Int, firstDimDepth: Int) extends Component {

  val depth = channels * firstDimDepth
  val addrWidth = log2Up(depth)

  val io = new Bundle {
    val rdPorts = Vec(master(MemReadPort(dataType(), log2Up(depth))), channels)
    val wrCmds = Vec(master(Flow(MemWriteCmd(mem))), channels)
    val push = slave(Stream(Vec(dataType(), channels)))
    val pop = master(Stream(Vec(dataType(), channels)))
  }

  val cfg = new Bundle {
    val interval = in UInt (log2Up(firstDimDepth) bits)
  }

  val popPre = Event
  val popping = popPre.fire
  val pushing = io.push.fire

  // pushing
  val firstDim = cfg.interval
  val secondDim = U(channels - 1, log2Up(channels) bits)
  val (pushCnt, _) = LoopsCntGen.regOvf(
    bound = List(firstDim, secondDim),
    enable = pushing
  )
  val pushAddr = UInt(addrWidth bits).setAsReg().init(0)
  when(pushing)(pushAddr := pushAddr + 1)
  val pushShift = pushCnt.last
  val pushPayload = io.push.payload
  val pushPayloadShift = BarrelShifter.leftT(pushPayload, pushShift)

  io.wrCmds.foreach(_.valid := pushing)
  io.wrCmds.foreach(_.payload.address := pushAddr)
  (io.wrCmds, pushPayloadShift).zipped.foreach(_.data := _)

  // popping
  val (popCnt, popCntOvf) = LoopsCntGen.regOvf(
    bound = List(secondDim, firstDim),
    enable = popping
  )
  val popBaseAddr = UInt(addrWidth bits).setAsReg().init(0)
  when(popping & popCntOvf.head)(popBaseAddr := popBaseAddr + 1 + cfg.interval)
  val offset = Vec(UInt(addrWidth bits), channels)
  val popAddrs = Vec(offset.map(_ + popBaseAddr))

  val popShift = popCnt.head
  val popPayload = Vec(dataType(), channels)
  val popPayloadShift = BarrelShifter.rightT(popPayload, popShift)

  io.rdPorts.foreach(_.cmd.valid := popPre.ready)
  (io.rdPorts, popAddrs).zipped.foreach(_.cmd.payload := _)
  (popPayload, io.rdPorts).zipped.foreach(_ := _.rsp)

  io.pop.arbitrationFrom(popPre.m2sPipe())
  io.pop.payload := popPayloadShift


}
