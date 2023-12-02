package xilinx.DSP48E2IntArith.standalone

import spinal.core._
import xilinx.DSP48E2._

import scala.language.postfixOps

/**
 * This component implements four pairs of 12-bit mux-add operation using a single DSP48E2.
 * This is done by controlling the four wide-bus multiplexer in the DSP48E2.
 *
 * ab = Mux(aSel,a,0) + Mux(bSel,b,0)
 */

class xadd_four12() extends Component {
  val io = new Bundle {
    val aSel = in Bits (1 bits)
    val bSel = in Bits (1 bits)
    val a = in Vec(Bits(12 bits), 4)
    val b = in Vec(Bits(12 bits), 4)
    val ab = out Vec(Bits(12 bits), 4)
  }

  val latency = 2
  val build = DSP48E2AttrBuild()
  build.setAsALU(4)

  val valOfInMode = build.setStaticINMODE((1, 1, 1, 0, 0), "-")
  val valOfOPMode = build.setDynamicOPMode_C_P_PCIN_AB((io.bSel.msb, False, False, io.aSel.msb))
  val valOfALUMode = build.setStaticALUMODE()
  val attrs = build.attrs

  val dsp = new DSP48E2(attrs)
  val AB = io.a.asBits

  // data
  dsp.DATAIN.A := AB.drop(18)
  dsp.DATAIN.B := AB.take(18)
  dsp.DATAIN.C := io.b.asBits
  dsp.DATAIN.D.clearAll()
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
   * When in ALU mode, the INMODE[0] is not effective. When the pipeline stage of A input is 1,
   * the DSP48E2 will automatically use the A2 register, therefore the CE pin of the A2 register is set to high.
   */

  dsp.CEs.A2.set()
  dsp.CEs.B2.set()
  dsp.CEs.C.set()
  dsp.CEs.P.set()
  dsp.CEs.CTRL.set()
  dsp.CEs.all.foreach(
    ce => if (!ce.hasAssignement) ce.clearAll()
  )

  // rst
  dsp.RSTs.all.foreach(
    rst => if (!rst.hasAssignement) rst.clearAll()
  )

  // output
  val P = dsp.DATAOUT.P
  io.ab.assignFromBits(P)
}
