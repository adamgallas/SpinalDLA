package xilinx.DSP48E2IntArith

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

/**
 * This component implement a chain of DSP48E2 for multiplex-add dot product of four 12-bit vectors.
 * For 8-bit inputs, the maximum length of the chain should not be larger than 16 to avoid overflow.
 * The last DSP48E2 in the chain can be configured as an accumulator, however, the accumulator does not support overflow protection.
 * Since A, B and C ports are all occupied, splitting the chain is not supported.
 *
 * @param length the length of the DSP48E2 chain
 * @param acc    set the last DSP48E2 in the chain is configured as an accumulator
 */

class xdotp_four12_chain(length: Int, acc: Boolean = false) extends Component {

  val io = new Bundle {
    val aSel = in Vec(Bits(1 bits), length)
    val bSel = in Vec(Bits(1 bits), length)
    val a = in Vec(Vec(Bits(12 bits), 4), length)
    val b = in Vec(Vec(Bits(12 bits), 4), length)
    val ab = out Vec(Bits(12 bits), 4)

    val valid = if (acc) in Bool() else null
    val last = if (acc) in Bool() else null
  }

  val latency = length + 2 - 1

  val lastDSPAccValid = if (acc) Bool().setAsReg().init(False) else null
  if (acc) {
    lastDSPAccValid.setWhen(io.valid).clearWhen(io.last)
  }

  val inMode = Bits(5 bits)
  val opModes = Vec(Bits(9 bits), length)
  val dsp48e2s = for (i <- 0 until length) yield {
    val build = DSP48E2AttrBuild()
    build.setAsALU(4)
    build.setStaticALUMODE()
    if (i == 0) inMode := build.setStaticINMODE((1, 1, 1, 0, 0), "-")
    else build.setStaticINMODE((1, 1, 1, 0, 0), "-")

    opModes(i) := build.setDynamicOPMODEforALU((
      io.bSel(i).msb,
      if (i == length - 1 && acc) lastDSPAccValid else False,
      if (i == 0) False
      else if (i == length - 1 && acc) io.valid
      else True,
      io.aSel(i).msb
    ))
    val attrs = build.attrs
    new DSP48E2(attrs)
  }

  for (i <- 0 until length) {
    // data
    val AB = io.a(i).asBits
    dsp48e2s(i).DATAIN.A := AB.drop(18)
    dsp48e2s(i).DATAIN.B := AB.take(18)
    dsp48e2s(i).DATAIN.C := io.b(i).asBits
    dsp48e2s(i).DATAIN.D.clearAll()
    dsp48e2s(i).DATAIN.CARRYIN.clearAll()

    // inst
    dsp48e2s(i).INST.ALUMODE.clearAll()
    dsp48e2s(i).INST.INMODE := inMode
    dsp48e2s(i).INST.OPMODE := opModes(i)
    dsp48e2s(i).INST.CARRYINSEL.clearAll()

    // cascade
    if (i == 0)
      dsp48e2s(i).CASCDATAIN.P.clearAll()
    else
      dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P

    // ce
    dsp48e2s(i).CEs.A2.set()
    dsp48e2s(i).CEs.B2.set()
    dsp48e2s(i).CEs.C.set()
    dsp48e2s(i).CEs.P.set()
    dsp48e2s(i).CEs.CTRL.set()
    dsp48e2s(i).CEs.all.foreach(
      ce => if (!ce.hasAssignement) ce.clearAll()
    )

    // rst
    if (i == length - 1 && acc) {
      dsp48e2s(i).addGeneric("IS_RSTA_INVERTED", "1'b1")
      dsp48e2s(i).addGeneric("IS_RSTB_INVERTED", "1'b1")
      dsp48e2s(i).addGeneric("IS_RSTC_INVERTED", "1'b1")
      dsp48e2s(i).RSTs.A := io.valid
      dsp48e2s(i).RSTs.B := io.valid
      dsp48e2s(i).RSTs.C := io.valid
    }
    dsp48e2s(i).RSTs.all.foreach(
      rst => if (!rst.hasAssignement) rst.clearAll()
    )
  }

  // output
  io.ab.assignFromBits(dsp48e2s.last.DATAOUT.P)
}
