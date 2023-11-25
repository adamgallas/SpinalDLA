package xilinx.DSP48E2IntArith

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

/**
 * This component implements 2 8-bit multipliers with a share input using a single DSP48E2.
 *
 * ab = a * b, ac = a * c
 */

class mul_int8() extends Component {
  val io = new Bundle {
    val a = in Bits (8 bits)
    val b = in Bits (8 bits)
    val c = in Bits (8 bits)
    val ab = out Bits (16 bits)
    val ac = out Bits (16 bits)
  }

  val latency = 4
  val build = DSP48E2AttrBuild()
  build.setAsMultiplier("M=PAxB")

  val valOfInMode = build.setStaticINMODE((1, 2, 1, 1, 1), "PA=D+A")
  val valOfOPMode = build.setStaticOPMODE("P=M+C")
  val valOfALUMode = build.setStaticALUMODE()
  val attrs = build.attrs

  val dsp = new DSP48E2(attrs)
  val abNeg = io.a.orR & io.b.orR & (io.a.msb ^ io.b.msb)
  val abNegReg = Delay(abNeg, 2)

  // data
  dsp.DATAIN.A := io.b.asSInt.resize(30).asBits
  dsp.DATAIN.B := io.a.asSInt.resize(18).asBits
  dsp.DATAIN.D := io.c.asSInt.expand ## B(27 - 9 bits, default -> false)
  dsp.DATAIN.C := B(29 bits, default -> false) ## abNegReg ## B(18 bits, default -> false)
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

  /**
   * When in USE_MULT mode, the INMODE[0] is effective. setStaticInmode will set INMODE[0] to 1 by default,
   * choosing the A1 register, therefore the CE pin of the A1 register is set to high.
   */

  dsp.CEs.A1.set()
  dsp.CEs.B1.set()
  dsp.CEs.B2.set()
  dsp.CEs.C.set()
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
  io.ab := P.take(16)
  io.ac := P.drop(18).take(16)
}
