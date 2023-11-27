package xilinx.DSP48E2IntArith.dualCascade

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int8_ws_prefetch(length: Int) extends Component {

  val io = new Bundle {
    val a = in Bits (8 bits)
    val b = in Vec(Bits(8 bits), length)
    val c = in Vec(Bits(8 bits), length)
    val ab = out Bits (18 bits)
    val ac = out Bits (18 bits)

    val CE1 = in Bool()
    val CE2 = in Bool()
    val rstCE1 = in Vec(Bool(), length)
  }

  val latency = length + 4 - 1

  val inModes = Vec(Bits(5 bits), length)
  val opModes = Vec(Bits(9 bits), length)

  val comDSPBuild = DSP48E2AttrBuild()
  comDSPBuild.setAsMultiplier("M=PAxB")
  comDSPBuild.setStaticALUMODE()
  comDSPBuild.attrs.B_INPUT = "CASCADE"
  val comInMode = comDSPBuild.setStaticINMODE((1, 2, 0, 1, 1), "PA=D+A")
  val comOpMode = comDSPBuild.setStaticOPMODE("P=M+PCIN")

  val firstDSPBuild = DSP48E2AttrBuild()
  firstDSPBuild.setAsMultiplier("M=PAxB")
  firstDSPBuild.setStaticALUMODE()
  val firstInMode = firstDSPBuild.setStaticINMODE((1, 2, 0, 1, 1), "PA=D+A")
  val firstOpMode = firstDSPBuild.setStaticOPMODE("P=M")

  val dsp48e2s = for (i <- 0 until length) yield {
    val attrs = if (i == 0) firstDSPBuild.attrs else comDSPBuild.attrs
    new DSP48E2(attrs)
  }

  val ce1Chain = Vec(Bool(), length)
  val ce2Chain = Vec(Bool(), length)

  for (i <- 0 until length) {

    dsp48e2s(i).DATAIN.A := Repeat(io.b(i).msb, 30 - 8) ## io.b(i)
    dsp48e2s(i).DATAIN.D := io.c(i).msb ## io.c(i) ## B(27 - 9 bits, default -> false)
    dsp48e2s(i).DATAIN.C.clearAll()
    dsp48e2s(i).DATAIN.CARRYIN.clearAll()

    dsp48e2s(i).CASCDATAIN.A.clearAll()
    dsp48e2s(i).CASCDATAIN.CARRYCAS.clearAll()
    dsp48e2s(i).CASCDATAIN.MULTSIGN.clearAll()

    dsp48e2s(i).INST.ALUMODE.clearAll()
    dsp48e2s(i).INST.CARRYINSEL.clearAll()

    dsp48e2s(i).CEs.A1.set()
    dsp48e2s(i).CEs.D.set()
    dsp48e2s(i).CEs.AD.set()
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
      dsp48e2s(i).DATAIN.B := Repeat(io.a.msb, 18 - 8) ## io.a
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

  val P = dsp48e2s.last.DATAOUT.P
  val abRes = P(17 downto 0).asBits
  val abNeg = B"0" ## abRes.msb
  val acRes = P(35 downto 18).asSInt + abNeg.asSInt

  io.ab := abRes
  io.ac := acRes.asBits
}