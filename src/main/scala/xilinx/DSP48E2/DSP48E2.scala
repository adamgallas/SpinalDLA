package xilinx.DSP48E2

import spinal.core._

import scala.language.postfixOps

/**
 * The DSP48E2 blackbox wrapper for SpinalHDL is inspired by the Chainsaw implementation:
 *
 * https://github.com/Chainsaw-Team/Chainsaw/tree/master/src/main/scala/Chainsaw/device
 */

case class DSP48E2INPUT() extends Bundle {
  val A = in Bits (30 bits)
  val B = in Bits (18 bits)
  val C = in Bits (48 bits)
  val D = in Bits (27 bits)
  val CARRYIN = in Bits (1 bits)
  val all = Seq(A, B, C, D, CARRYIN)
}

case class DSP48E2OUTPUT() extends Bundle {
  val P = out Bits (48 bits)
  val CARRYOUT = out Bits (4 bits)
  val XOROUT = out Bits (8 bits)
  val OVERFLOW, UNDERFLOW = out Bool()
  val PATTERNBDETECT, PATTERNDETECT = out Bool()
}

case class DSP48E2CONTROL() extends Bundle {
  val ALUMODE = Bits(4 bits)
  val INMODE = Bits(5 bits)
  val OPMODE = Bits(9 bits)
  val CARRYINSEL = Bits(3 bits)
  val all = Seq(ALUMODE, INMODE, OPMODE, CARRYINSEL)
}

case class DSP48E2CASC() extends Bundle {
  val A = Bits(30 bits)
  val B = Bits(18 bits)
  val P = Bits(48 bits)
  val CARRYCAS = Bits(1 bits)
  val MULTSIGN = Bits(1 bits)
  val all = Seq(A, B, P, CARRYCAS)

  def setAsCascadeOut(): Unit = {
    all.foreach(signal => signal.setName(signal.getPartialName() + "COUT"))
    this.MULTSIGN.setName("MULTSIGNOUT")
  }

  def setAsCascadeIn(): Unit = {
    all.foreach(signal => signal.setName(signal.getPartialName() + "CIN"))
    this.MULTSIGN.setName("MULTSIGNIN")
  }
}

case class DSP48E2CEs() extends Bundle {
  val A1, A2, B1, B2, C, D, AD, M, P, CARRYIN, CTRL, INMODE, ALUMODE = Bool()
  val all = Seq(A1, A2, B1, B2, C, D, AD, M, P, CARRYIN, CTRL, INMODE, ALUMODE)
  all.foreach(signal => signal.setName("CE" + signal.getPartialName()))
}

case class DSP48E2RSTs() extends Bundle {
  val A, B, C, D, M, P, ALLCARRYIN, CTRL, INMODE, ALUMODE = Bool()
  val all = Seq(A, B, C, D, M, P, ALLCARRYIN, CTRL, INMODE, ALUMODE)
  all.foreach(signal => signal.setName("RST" + signal.getPartialName()))
}

class DSP48E2Attributes() {

  var A_INPUT = "DIRECT"
  var B_INPUT = "DIRECT"
  var AMULTSEL = "A"
  var BMULTSEL = "B"
  var PREADDINSEL = "A"

  var USE_MULT = "MULTIPLY"
  var USE_SIMD = "ONE48"

  var AREG, BREG, CREG, DREG, ADREG, MREG, PREG = 1
  var ACASCREG, BCASCREG = 1
  var CARRYINREG, CARRYINSELREG = 1
  var INMODEREG, OPMODEREG, ALUMODEREG = 1

  def generics = Seq(
    "A_INPUT" -> A_INPUT,
    "B_INPUT" -> B_INPUT,
    "AMULTSEL" -> AMULTSEL,
    "BMULTSEL" -> BMULTSEL,
    "PREADDINSEL" -> PREADDINSEL,

    "USE_MULT" -> USE_MULT,
    "USE_SIMD" -> USE_SIMD,

    "AREG" -> AREG,
    "BREG" -> BREG,
    "CREG" -> CREG,
    "DREG" -> DREG,
    "ADREG" -> ADREG,
    "MREG" -> MREG,
    "PREG" -> PREG,

    "ACASCREG" -> ACASCREG,
    "BCASCREG" -> BCASCREG,

    "CARRYINREG" -> CARRYINREG,
    "CARRYINSELREG" -> CARRYINSELREG,

    "INMODEREG" -> INMODEREG,
    "OPMODEREG" -> OPMODEREG,
    "ALUMODEREG" -> ALUMODEREG
  )
}

class DSP48E2(attrs: DSP48E2Attributes) extends BlackBox {
  addGenerics(attrs.generics: _*)
  val CLK = in Bool()
  val INST = in(DSP48E2CONTROL())

  val CASCDATAIN = in(DSP48E2CASC())
  val CASCDATAOUT = out(DSP48E2CASC())

  val CEs = in(DSP48E2CEs())
  val RSTs = in(DSP48E2RSTs())

  val DATAIN = in(DSP48E2INPUT())
  val DATAOUT = out(DSP48E2OUTPUT())

  INST.setName("")
  DATAIN.setName("")
  DATAOUT.setName("")
  CASCDATAOUT.setAsCascadeOut()
  CASCDATAIN.setAsCascadeIn()

  CEs.all.foreach(_.default(False))
  RSTs.all.foreach(_.default(False))
  DATAIN.all.foreach(s => s.default(s.getZero))
  INST.all.foreach(s => s.default(s.getZero))

  CASCDATAIN.all.foreach(s => s.default(s.getZero))
  CASCDATAIN.MULTSIGN.default(CASCDATAIN.MULTSIGN.getZero)

  mapClockDomain(clock = CLK)
}
