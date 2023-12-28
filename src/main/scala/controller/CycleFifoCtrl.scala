package controller

import spinal.core._
import spinal.lib._
import util.LoopsCntGen

import scala.language.postfixOps

class CycleFifoCtrl[T <: Data](dataType: HardType[T], mem: Mem[T], depth: Int, widthOfReuseTimes: Int = 16) extends Component {
  // assuming mem has a readout cycle of 1
  val addrWidth = log2Up(depth)
  val io = new Bundle {
    val rdPort = master(MemReadPort(dataType(), log2Up(depth)))
    val wrCmd = master(Flow(MemWriteCmd(mem)))
    val push = slave(Stream(dataType()))
    val pop = master(Stream(dataType()))
  }
  val cfg = new Bundle {
    val reuseLength = in UInt (addrWidth bits)
    val reuseTimes = in UInt (widthOfReuseTimes bits)
  }

  val popPre = Event
  val popping = popPre.fire
  val pushing = io.push.fire

  val (popCnt, popCntOvf) = LoopsCntGen.regOvf(
    bound = List(cfg.reuseLength, cfg.reuseTimes),
    enable = popping
  )

  val pushPtr = UInt(log2Up(depth) bits) setAsReg() init 0
  val popPtr = UInt(log2Up(depth) bits) setAsReg() init 0
  val markStart = UInt(log2Up(depth) bits) setAsReg() init 0
  val markEnd = UInt(log2Up(depth) bits) setAsReg() init 0

  val pushPtrNext = UInt(log2Up(depth) bits)
  val pushPtrPlus = pushPtr + 1
  pushPtr := pushPtrNext
  pushPtrNext := pushPtr

  val popPtrNext = UInt(log2Up(depth) bits)
  val popPtrPlus = popPtr + 1
  popPtr := popPtrNext
  popPtrNext := popPtr

  when(pushing) {
    pushPtrNext := pushPtrPlus
  }

  when(popping) {
    popPtrNext := popPtrPlus
    when(popCntOvf(0)) {
      popPtrNext := markStart
      when(popCntOvf(1)) {
        popPtrNext := popPtrPlus
      }
    }
  }

  val endInc = popCnt(1) === 0
  val startInc = popCntOvf(1)

  when(popping & endInc) {
    markEnd := popPtrPlus
  }

  when(popping & startInc) {
    markStart := popPtrPlus
  }

  val startRising = RegInit(False)
  when(pushing =/= (popping & startInc)) {
    startRising := pushing
  }

  val endRising = RegInit(False)
  when(pushing =/= (popping & endInc)) {
    endRising := pushing
  }

  val pushMatchEnd = pushPtr === markEnd
  val pushMatchStart = pushPtr === markStart

  val full = Bool()
  val empty = Bool()

  full := pushMatchStart && startRising
  empty := pushMatchEnd && !endRising

  io.wrCmd.valid := pushing
  io.wrCmd.payload.address := pushPtr
  io.wrCmd.payload.data := io.push.payload

  io.rdPort.cmd.valid := popPre.ready
  io.rdPort.cmd.payload := popPtr

  io.push.ready := !full
  popPre.valid := !empty
  io.pop.arbitrationFrom(popPre.m2sPipe())
  io.pop.payload := io.rdPort.rsp
}
