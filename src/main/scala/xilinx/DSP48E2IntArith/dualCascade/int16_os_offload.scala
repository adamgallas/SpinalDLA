package xilinx.DSP48E2IntArith.dualCascade

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArith.dualCascade.int16_ws_prefetch

import scala.language.postfixOps
import scala.util.Random

class int16_os_offload(length: Int) extends Component {
  val io = new Bundle {
    val a = in Bits (16 bits)
    val b = in Vec(Bits(16 bits), length)
    val ab = out Bits (48 bits)

    val accValid = in Bool()
    val accLast = in Bool()
  }

  val latency = length + 4 - 1

  val selOfM = Vec(Bool(), length)
  val selOfPCIN = Vec(Bool(), length)
  val selOfP = Vec(Bool(), length)
  val rst = Vec(Bool(), length + 1)

  val validDly = RegNext(io.accValid, False)
  val lastDly = Delay(io.accLast, 5, init = False)

  val inModes = Vec(Bits(5 bits), length)
  val opModes = Vec(Bits(9 bits), length)

  val dsp48e2s = for (i <- 0 until length) yield {

    val build = DSP48E2AttrBuild()
    build.setAsMultiplier("M=AxB")
    build.setStaticALUMODE()

    if (i != 0) build.attrs.B_INPUT = "CASCADE"

    inModes(i) := build.setStaticINMODE((2, 2, 0, 0, 0), "PA=A")
    opModes(i) := build.setDynamicOPMODEforMult((False, selOfM(i), selOfP(i) ## selOfPCIN(i)))
    new DSP48E2(build.attrs)
  }

  for (i <- 0 until length) {

    dsp48e2s(i).DATAIN.A := Repeat(io.b(i).msb, 30 - 16) ## io.b(i)
    dsp48e2s(i).DATAIN.D.clearAll()
    dsp48e2s(i).DATAIN.C.clearAll()
    dsp48e2s(i).DATAIN.CARRYIN.clearAll()

    dsp48e2s(i).CASCDATAIN.A.clearAll()
    dsp48e2s(i).CASCDATAIN.CARRYCAS.clearAll()
    dsp48e2s(i).CASCDATAIN.MULTSIGN.clearAll()

    dsp48e2s(i).INST.ALUMODE.clearAll()
    dsp48e2s(i).INST.INMODE := inModes(i)
    dsp48e2s(i).INST.OPMODE := opModes(i)
    dsp48e2s(i).INST.CARRYINSEL.clearAll()

    dsp48e2s(i).CEs.A1.set()
    dsp48e2s(i).CEs.A2.set()
    dsp48e2s(i).CEs.M.set()
    dsp48e2s(i).CEs.B1.set()
    dsp48e2s(i).CEs.B2.set()
    dsp48e2s(i).CEs.P.set()
    dsp48e2s(i).CEs.CTRL.set()
    dsp48e2s(i).CEs.all.foreach(ce => if (!ce.hasAssignement) ce.clearAll())

    if (i == 0) {
      dsp48e2s(i).DATAIN.B := Repeat(io.a.msb, 18 - 16) ## io.a
      dsp48e2s(i).CASCDATAIN.B.clearAll()
      dsp48e2s(i).CASCDATAIN.P.clearAll()
    }
    else {
      dsp48e2s(i).DATAIN.B.clearAll()
      dsp48e2s(i).CASCDATAIN.B := dsp48e2s(i - 1).CASCDATAOUT.B
      dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P
    }
  }

  for (i <- 0 until length) {
    selOfM(i).setAsReg().init(false)
    if (i == 0) selOfM(i) := validDly
    else selOfM(i) := selOfM(i - 1)

    rst(i).setAsReg().init(false)
    if (i == 0) rst(0) := lastDly
    else rst(i) := rst(i - 1)

    dsp48e2s(i).RSTs.P := rst(i + 1)
    dsp48e2s(i).RSTs.all.foreach(rst => if (!rst.hasAssignement) rst.clearAll())

    selOfPCIN(i).setAsReg().init(false)
    selOfPCIN(i).setWhen(lastDly).clearWhen(rst(i))

    selOfP(i).setAsReg().init(false)
    selOfP(i).setWhen(selOfM(i)).clearWhen(lastDly)
  }
  rst.last.setAsReg().init(false)
  rst.last := rst.dropRight(1).last

  io.ab := dsp48e2s.last.DATAOUT.P
}
