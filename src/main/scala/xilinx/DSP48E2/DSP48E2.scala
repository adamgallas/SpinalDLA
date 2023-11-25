package xilinx.DSP48E2

import spinal.core._

import scala.language.postfixOps

/**
 * The DSP48E2 blackbox wrapper for SpinalHDL is inspired by the Chainsaw implementation:
 *
 * https://github.com/Chainsaw-Team/Chainsaw/tree/master/src/main/scala/Chainsaw/device
 */

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
