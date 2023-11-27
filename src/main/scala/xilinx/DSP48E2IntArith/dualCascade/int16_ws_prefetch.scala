package xilinx.DSP48E2IntArith.dualCascade


import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int16_ws_prefetch(length: Int) extends Component {

  val io = new Bundle {
    val a = in Bits (16 bits)
    val b = in Vec(Bits(16 bits), length)
    val ab = out Bits (48 bits)

    val CE1 = in Bool()
    val CE2 = in Bool()
    // rstCE1 should be the same for all CE1 registers,
    // this duplication is for fanout reduction outside this component
    val rstCE1 = in Vec(Bool(), length)
  }

  val latency = length + 4 - 1

  val inModes = Vec(Bits(5 bits), length)
  val opModes = Vec(Bits(9 bits), length)

  val comDSPBuild = DSP48E2AttrBuild()
  comDSPBuild.setAsMultiplier("M=AxB")
  comDSPBuild.setStaticALUMODE()
  comDSPBuild.attrs.B_INPUT = "CASCADE"
  val comInMode = comDSPBuild.setStaticINMODE((2, 2, 0, 0, 0), "PA=A")
  val comOpMode = comDSPBuild.setStaticOPMODE("P=M+PCIN")

  val firstDSPBuild = DSP48E2AttrBuild()
  firstDSPBuild.setAsMultiplier("M=AxB")
  firstDSPBuild.setStaticALUMODE()
  val firstInMode = firstDSPBuild.setStaticINMODE((2, 2, 0, 0, 0), "PA=A")
  val firstOpMode = firstDSPBuild.setStaticOPMODE("P=M")

  val dsp48e2s = for (i <- 0 until length) yield {
    val attrs = if (i == 0) firstDSPBuild.attrs else comDSPBuild.attrs
    new DSP48E2(attrs)
  }

  val ce1Chain = Vec(Bool(), length)
  val ce2Chain = Vec(Bool(), length)

  for (i <- 0 until length) {

    dsp48e2s(i).DATAIN.A := Repeat(io.b(i).msb, 30 - 16) ## io.b(i)
    dsp48e2s(i).DATAIN.D.clearAll()
    dsp48e2s(i).DATAIN.C.clearAll()
    dsp48e2s(i).DATAIN.CARRYIN.clearAll()

    dsp48e2s(i).CASCDATAIN.A.clearAll()
    dsp48e2s(i).CASCDATAIN.CARRYCAS.clearAll()
    dsp48e2s(i).CASCDATAIN.MULTSIGN.clearAll()

    dsp48e2s(i).INST.ALUMODE.clearAll()
    dsp48e2s(i).INST.CARRYINSEL.clearAll()

    dsp48e2s(i).CEs.A1.set()
    dsp48e2s(i).CEs.A2.set()
    dsp48e2s(i).CEs.M.set()
    dsp48e2s(i).CEs.P.set()
    dsp48e2s(i).CEs.B1 := ce1Chain(i)
    dsp48e2s(i).CEs.B2 := ce2Chain(i)
    dsp48e2s(i).CEs.all.foreach(
      ce => if (!ce.hasAssignement) ce.clearAll()
    )

    dsp48e2s(i).RSTs.all.foreach(
      rst => if (!rst.hasAssignement) rst.clearAll()
    )

    if (i == 0) {
      dsp48e2s(i).CASCDATAIN.B.clearAll()
      dsp48e2s(i).DATAIN.B := Repeat(io.a.msb, 18 - 16) ## io.a
      dsp48e2s(i).INST.INMODE := firstInMode
      dsp48e2s(i).INST.OPMODE := firstOpMode
      dsp48e2s(i).CASCDATAIN.P.clearAll()

      ce1Chain(i) := io.CE1
      ce2Chain(i) := io.CE2
    }
    else {
      dsp48e2s(i).CASCDATAIN.B := dsp48e2s(i - 1).CASCDATAOUT.B
      dsp48e2s(i).DATAIN.B.clearAll()
      dsp48e2s(i).INST.INMODE := comInMode
      dsp48e2s(i).INST.OPMODE := comOpMode
      dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P

      ce1Chain(i).setAsReg()
      ce2Chain(i).setAsReg().init(false)

      ce1Chain(i) := ce1Chain(i - 1)
      ce2Chain(i) := ce2Chain(i - 1)
      ce1Chain(i).clearWhen(io.rstCE1(i))
    }
  }

  io.ab := dsp48e2s.last.DATAOUT.P
}