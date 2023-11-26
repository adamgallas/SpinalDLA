package xilinx.DSP48E2IntArith

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

/**
 * This component implement a chain of DSP48E2 for dot product of two 8-bit vectors according to the white paper WP487.
 * The maximum length of the chain should not be larger than 7 to avoid overflow.
 * However, if the input vectors are sparse, or the bit width of each element is smaller than 8, the chain can be longer but lost overflow protection.
 * This component support splitting the chain into two parts to reduce the granularity of the DSP48E2 chain,
 * which is useful to increase routing flexibility.
 *
 * @param length  the length of the DSP48E2 chain
 * @param splitAt the position of the DSP48E2 to split the chain
 */


class dotp_int8_chain(length: Int, splitAt: Int = -1) extends Component {

  val io = new Bundle {
    val a = in Vec(Bits(8 bits), length)
    val b = in Vec(Bits(8 bits), length)
    val c = in Vec(Bits(8 bits), length)
    val ab = out Bits (18 bits)
    val ac = out Bits (18 bits)
  }

  val latency = length + 4 - 1

  val fromIndex = if (splitAt == -1) -1 else splitAt - 1
  val toIndex = if (splitAt == -1) -1 else splitAt + 2
  require(toIndex <= length - 1)

  val inModes = Vec(Bits(5 bits), length)
  val opModes = Vec(Bits(9 bits), length)

  val dsp48e2s = for (i <- 0 until length) yield {

    val build = DSP48E2AttrBuild()
    build.setAsMultiplier("M=PAxB")
    build.setStaticALUMODE()

    inModes(i) := build.setStaticINMODE(
      (1, 2, if (i == toIndex) 1 else 0, 1, 1), "PA=D+A")
    opModes(i) := build.setStaticOPMODE(
      if (i == 0 || i == fromIndex + 1) "P=M"
      else if (i == toIndex) "P=M+C+PCIN"
      else "P=M+PCIN"
    )
    val attrs = build.attrs
    new DSP48E2(attrs)
  }

  if (splitAt != -1) dsp48e2s(toIndex).DATAIN.C := RegNext(dsp48e2s(fromIndex).DATAOUT.P)

  for (i <- 0 until length) {

    dsp48e2s(i).DATAIN.A := Repeat(io.b(i).msb, 30 - 8) ## io.b(i)
    dsp48e2s(i).DATAIN.B := Repeat(io.a(i).msb, 18 - 8) ## io.a(i)
    dsp48e2s(i).DATAIN.D := io.c(i).msb ## io.c(i) ## B(27 - 9 bits, default -> false)
    dsp48e2s(i).DATAIN.CARRYIN.clearAll()

    dsp48e2s(i).CASCDATAIN.A.clearAll()
    dsp48e2s(i).CASCDATAIN.B.clearAll()
    dsp48e2s(i).CASCDATAIN.CARRYCAS.clearAll()
    dsp48e2s(i).CASCDATAIN.MULTSIGN.clearAll()

    dsp48e2s(i).INST.ALUMODE.clearAll()
    dsp48e2s(i).INST.INMODE := inModes(i)
    dsp48e2s(i).INST.OPMODE := opModes(i)
    dsp48e2s(i).INST.CARRYINSEL.clearAll()

    dsp48e2s(i).CEs.A1.set()
    dsp48e2s(i).CEs.B1.set()
    dsp48e2s(i).CEs.B2.set()
    dsp48e2s(i).CEs.D.set()
    dsp48e2s(i).CEs.AD.set()
    dsp48e2s(i).CEs.M.set()
    dsp48e2s(i).CEs.P.set()
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

  val P = dsp48e2s.last.DATAOUT.P
  val abRes = P(17 downto 0).asBits
  val abNeg = B"0" ## abRes.msb
  val acRes = P(35 downto 18).asSInt + abNeg.asSInt

  io.ab := abRes
  io.ac := acRes.asBits
}

