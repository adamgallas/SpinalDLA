package example

import spinal.core._
import xilinx.DSP48E2IntArithmetic.dualCascade.int12_ws_AB_C_P

import scala.language.postfixOps

class firefly16_pe(isLastPEHorizontal: Boolean = false) extends Component {

  val length = 16

  case class inpBdl() extends Bundle {
    val a = Vec(Bits(8 bits), 4)
    val b = Vec(Bits(8 bits), 4)
    val enPrefetch = in Bool()
    val enFetch = in Bool()
    val clrPrefetch = in Vec(Bool(), length)
  }

  case class outBdl() extends Bundle {
    val out = Vec(Bits(12 bits), 4)
  }

  case class horizBdl() extends Bundle {
    val aSel = in Vec(Bits(1 bits), length)
    val bSel = in Vec(Bits(1 bits), length)
  }

  val inp = new inpBdl().asInput()
  val out = new outBdl().asOutput()
  val srcH = new horizBdl().asInput()
  val dstH = if (!isLastPEHorizontal) new horizBdl().asOutput() else null

  val srcHDly = RegNext(srcH)
  if (!isLastPEHorizontal) dstH := srcHDly

  val chain = new int12_ws_ab_c_p_clb(length, 8)

  chain.io.enPrefetch := inp.enPrefetch
  chain.io.enFetch := inp.enFetch
  chain.io.clrPrefetch := inp.clrPrefetch
  chain.io.a := inp.a
  chain.io.b := inp.b

  chain.io.aSel := srcHDly.aSel
  chain.io.bSel := srcHDly.bSel

  out.out := chain.io.ab
}
