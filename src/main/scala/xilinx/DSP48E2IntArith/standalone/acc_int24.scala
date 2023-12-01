package xilinx.DSP48E2IntArith.standalone

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2.{DSP48E2, DSP48E2AttrBuild}

import scala.language.postfixOps

class acc_int24() extends Component {
  val io=new Bundle{
    val a = in Bits(24 bits)
    val b = in Bits(24 bits)
    val c = in Bits(24 bits)
    val d = in Bits(24 bits)

    val ab = out Bits(24 bits)
    val cd = out Bits(24 bits)

    val valid = in Bool()
    val last = in Bool()
  }

  val accValid = Bool().setAsReg().init(False)
  accValid.setWhen(io.valid).clearWhen(io.last)
  val build = DSP48E2AttrBuild()
  build.setAsALU(2)

  val valOfInMode = build.setStaticINMODE((1, 1, 1, 0, 0), "-")
  val valOfOPMode = build.setDynamicOPMODEforALU((io.valid, accValid, False, io.valid))
  val valOfALUMode = build.setStaticALUMODE()

  val attrs = build.attrs
  val dsp = new DSP48E2(attrs)

  val ac = io.a ## io.c
  val bd = io.b ## io.d

  // data
  dsp.DATAIN.A := ac.drop(18)
  dsp.DATAIN.B := ac.take(18)
  dsp.DATAIN.C := bd
  dsp.DATAIN.D.clearAll()
  dsp.DATAIN.CARRYIN.clearAll()

  // inst
  dsp.INST.ALUMODE := valOfALUMode
  dsp.INST.INMODE := valOfInMode
  dsp.INST.OPMODE := valOfOPMode
  dsp.INST.CARRYINSEL.clearAll()

  // cascade
  dsp.CASCDATAIN.all.foreach(_.clearAll())

  // ce
  dsp.CEs.A2.set()
  dsp.CEs.B2.set()
  dsp.CEs.C.set()
  dsp.CEs.P.set()
  dsp.CEs.CTRL.set()
  dsp.CEs.all.foreach(
    ce => if (!ce.hasAssignement) ce.clearAll()
  )

  dsp.RSTs.all.foreach(
    rst => if (!rst.hasAssignement) rst.clearAll()
  )

  io.ab := dsp.DATAOUT.P.drop(48)
  io.cd := dsp.DATAOUT.P.take(48)
}
