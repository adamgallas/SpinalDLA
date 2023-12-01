package xilinx.DSP48E2IntArith.standalone

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class mul_int16() extends Component {
  val io = new Bundle {
    val a = in Bits (16 bits)
    val b = in Bits (16 bits)
    val ab = out Bits (32 bits)
  }

  val latency = 4
  val build = DSP48E2AttrBuild()
  build.setAsMultiplier("M=AxB")

  val valOfInMode = build.setStaticINMODE((2, 2, 0, 0, 0), "PA=A")
  val valOfOPMode = build.setStaticOPMODE("P=M+C")
  val valOfALUMode = build.setStaticALUMODE()
  val attrs = build.attrs

  val dsp = new DSP48E2(attrs)

  // data
  dsp.DATAIN.A := io.b.asSInt.resize(30).asBits
  dsp.DATAIN.B := io.a.asSInt.resize(18).asBits
  dsp.DATAIN.D.clearAll()
  dsp.DATAIN.C.clearAll()
  dsp.DATAIN.CARRYIN.clearAll()

  // inst
  dsp.INST.ALUMODE := valOfALUMode
  dsp.INST.INMODE := valOfInMode
  dsp.INST.OPMODE := valOfOPMode
  dsp.INST.CARRYINSEL.clearAll()

  // cascade
  dsp.CASCDATAIN.all.foreach(_.clearAll())
  dsp.CASCDATAIN.MULTSIGN.clearAll()

  // ce
  dsp.CEs.A1.set()
  dsp.CEs.A2.set()
  dsp.CEs.B1.set()
  dsp.CEs.B2.set()
  dsp.CEs.M.set()
  dsp.CEs.P.set()

  dsp.CEs.all.foreach(
    ce => if (!ce.hasAssignement) ce.clearAll()
  )

  dsp.RSTs.all.foreach(
    rst => if (!rst.hasAssignement) rst.clearAll()
  )

  io.ab := dsp.DATAOUT.P.take(32)
}
