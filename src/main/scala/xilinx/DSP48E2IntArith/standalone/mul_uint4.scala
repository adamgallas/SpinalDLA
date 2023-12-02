package xilinx.DSP48E2IntArith.standalone

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class mul_uint4() extends Component {
  val io = new Bundle {
    val w1 = in Bits (4 bits)
    val w2 = in Bits (4 bits)
    val a1 = in Bits (4 bits)
    val a2 = in Bits (4 bits)
    val a1w1 = out Bits (8 bits)
    val a1w2 = out Bits (8 bits)
    val a2w1 = out Bits (8 bits)
    val a2w2 = out Bits (8 bits)
  }

  val latency = 4
  val build = DSP48E2AttrBuild()
  build.setAsMultiplier("M=PAxB")

  val valOfInMode = build.setStaticINMODE((1, 2, 1, 1, 1), "PA=D+A")
  val valOfOPMode = build.setStaticOPMODE("P=M+C")
  val valOfALUMode = build.setStaticALUMODE()
  val attrs = build.attrs

  val dsp = new DSP48E2(attrs)

  // data
  dsp.DATAIN.A := io.w1.asSInt.resize(30).asBits
  dsp.DATAIN.B := B"000" ## io.a2 ## B"0000000" ## io.a1
  dsp.DATAIN.D := io.w2.msb ## io.w2 ## B(22 bits, default -> False)

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
  dsp.CEs.B1.set()
  dsp.CEs.B2.set()
  dsp.CEs.D.set()
  dsp.CEs.AD.set()
  dsp.CEs.M.set()
  dsp.CEs.P.set()
  dsp.CEs.all.foreach(
    ce => if (!ce.hasAssignement) ce.clearAll()
  )

  // rst
  dsp.RSTs.all.foreach(
    rst => if (!rst.hasAssignement) rst.clearAll()
  )

  // output
  val P = dsp.DATAOUT.P

  val sa1w1 = P(7 downto 0).asSInt
  val sa2w1 = P(18 downto 11).asSInt
  val sa1w2 = P(29 downto 22).asSInt
  val sa2w2 = P(40 downto 33).asSInt

  io.a1w1 := sa1w1.asBits
  io.a2w1 := sa2w1.asBits
  io.a1w2 := sa1w2.asBits
  io.a2w2 := sa2w2.asBits
}
