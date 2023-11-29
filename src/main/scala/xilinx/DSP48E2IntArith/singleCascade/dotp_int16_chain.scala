package xilinx.DSP48E2IntArith.singleCascade

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class dotp_int16_chain(length: Int, acc: Boolean = false, splitAt: Int = -1) extends Component {

  val io = new Bundle {
    val a = in Vec(Bits(16 bits), length)
    val b = in Vec(Bits(16 bits), length)
    val ab = out Bits (48 bits)

    val valid = if (acc) in Bool() else null
    val last = if (acc) in Bool() else null
  }

  val latency = length + 4 - 1

  val valid = Delay(io.valid, 2, init = False)
  val last = Delay(io.last, 2, init = False)
  val lastDSPAccValid = if (acc) Bool().setAsReg().init(False) else null
  if (acc) {
    lastDSPAccValid.setWhen(valid).clearWhen(last)
  }

  val fromIndex = if (splitAt == -1) -1 else splitAt - 1
  val toIndex = if (splitAt == -1) -1 else splitAt + 2
  require(toIndex < length - 1)

  val inModes = Vec(Bits(5 bits), length)
  val opModes = Vec(Bits(9 bits), length)

  val dsp48e2s = for (i <- 0 until length) yield {

    val build = DSP48E2AttrBuild()
    build.setAsMultiplier("M=AxB")
    build.setStaticALUMODE()

    inModes(i) := build.setStaticINMODE(
      (2, 2, if (i == toIndex) 1 else 0, 0, 0), "PA=A")

    if (i == length - 1 && acc) {
      opModes(i) := build.setDynamicOPModeforMultWithOutC(
        (lastDSPAccValid, valid, valid)
      )
    }
    else {
      opModes(i) := build.setStaticOPMODE(
        if (i == 0 || i == fromIndex + 1) "P=M"
        else if (i == toIndex) "P=M+C+PCIN"
        else "P=M+PCIN"
      )
    }

    val attrs = build.attrs
    new DSP48E2(attrs)
  }

  if (splitAt != -1) dsp48e2s(toIndex).DATAIN.C := RegNext(dsp48e2s(fromIndex).DATAOUT.P)

  for (i <- 0 until length) {

    dsp48e2s(i).DATAIN.A := Repeat(io.b(i).msb, 30 - 16) ## io.b(i)
    dsp48e2s(i).DATAIN.B := Repeat(io.a(i).msb, 18 - 16) ## io.a(i)
    dsp48e2s(i).DATAIN.D.clearAll()
    dsp48e2s(i).DATAIN.CARRYIN.clearAll()

    dsp48e2s(i).CASCDATAIN.A.clearAll()
    dsp48e2s(i).CASCDATAIN.B.clearAll()
    dsp48e2s(i).CASCDATAIN.CARRYCAS.clearAll()
    dsp48e2s(i).CASCDATAIN.MULTSIGN.clearAll()

    dsp48e2s(i).INST.ALUMODE.clearAll()
    dsp48e2s(i).INST.INMODE := inModes(i)
    dsp48e2s(i).INST.OPMODE := opModes(i)

    dsp48e2s(i).CEs.A1.set()
    dsp48e2s(i).CEs.A2.set()
    dsp48e2s(i).CEs.B1.set()
    dsp48e2s(i).CEs.B2.set()
    dsp48e2s(i).CEs.M.set()
    dsp48e2s(i).CEs.P.set()
    dsp48e2s(i).CEs.CTRL.set()
    if (i == toIndex) dsp48e2s(i).CEs.C.set()
    dsp48e2s(i).CEs.all.foreach(
      ce => if (!ce.hasAssignement) ce.clearAll()
    )

    dsp48e2s(i).RSTs.all.foreach(
      rst => if (!rst.hasAssignement) rst.clearAll()
    )

    if (i == 0)
      dsp48e2s(i).CASCDATAIN.P.clearAll()
    else if (i == splitAt)
      dsp48e2s(i).CASCDATAIN.P.clearAll()
    else
      dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P

    if (i != toIndex) dsp48e2s(i).DATAIN.C.clearAll()
  }

  io.ab := dsp48e2s.last.DATAOUT.P
}
