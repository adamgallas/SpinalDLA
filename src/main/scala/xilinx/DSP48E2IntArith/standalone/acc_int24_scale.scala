package xilinx.DSP48E2IntArith.standalone

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2.{DSP48E2, DSP48E2AttrBuild}

import scala.language.postfixOps

class acc_int24_scale() extends Component {
  val io = new Bundle {
    val a = in Bits (24 bits)
    val b = in Bits (24 bits)
    val scale = in Bits (18 bits)
    val P = out Bits (48 bits)
    val valid = in Bool()
    val last = in Bool()
  }

  val latency = 2
  val accValid = Bool().setAsReg().init(False)
  accValid.setWhen(io.valid).clearWhen(io.last)
  val build = DSP48E2AttrBuild()

  val multEnable = Bool()
  val muteA = Bool() // high active
  val muteD = Bool() // low active

  val valOfOPMode = build.setDynamicOPMode_C_P_RND_M((io.valid, accValid, io.b.msb & io.valid, multEnable))
  val valOfALUMode = build.setStaticALUMODE()
  build.attrs.BREG = 2

  val attrs = build.attrs
  val dsp = new DSP48E2(attrs)
  dsp.addGeneric("RND", B(BigInt("ffffff000000", 16)))

  val ab = io.a ## io.b
  val aAcc = dsp.DATAOUT.P.drop(24)
  val bAcc = dsp.DATAOUT.P.take(24)

  // data
  dsp.DATAIN.A := aAcc.asSInt.resize(30 bits).asBits
  dsp.DATAIN.D := bAcc.asSInt.resize(27 bits).asBits
  dsp.DATAIN.B := io.scale
  dsp.DATAIN.C := ab
  dsp.DATAIN.CARRYIN.clearAll()

  // inst
  dsp.INST.ALUMODE := valOfALUMode
  dsp.INST.INMODE := B"00" ## muteD ## muteA ## B"1"
  dsp.INST.OPMODE := valOfOPMode
  dsp.INST.CARRYINSEL.clearAll()

  // cascade
  dsp.CASCDATAIN.all.foreach(_.clearAll())

  dsp.CEs.A1.set()
  dsp.CEs.B1.set()
  dsp.CEs.B2.set()
  dsp.CEs.D.set()
  dsp.CEs.C.set()
  dsp.CEs.P.set()
  dsp.CEs.INMODE.set()
  dsp.CEs.CTRL.set()
  dsp.CEs.all.foreach(
    ce => if (!ce.hasAssignement) ce.clearAll()
  )

  dsp.RSTs.all.foreach(
    rst => if (!rst.hasAssignement) rst.clearAll()
  )

  io.P := dsp.DATAOUT.P
}
