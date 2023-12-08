package xilinx.DSP48E2

import spinal.core._
import scala.language.postfixOps

object DSP48E2ConfigMode {

  def set_alu_attr(attr: DSP48E2Attributes, simd: Int = 1) = {
    attr.MREG = 0
    attr.USE_MULT = "NONE"
    simd match {
      case 1 => attr.USE_SIMD = "ONE48"
      case 2 => attr.USE_SIMD = "TWO24"
      case 4 => attr.USE_SIMD = "FOUR12"
    }
  }

  def set_mul_attr(attr: DSP48E2Attributes) = {
    attr.MREG = 1
    attr.USE_MULT = "MULTIPLY"
    attr.USE_SIMD = "ONE48"
  }

  def assign_m_ctrl(dsp: DSP48E2, ce: Bool = False, rst: Bool = False): Unit = {
    dsp.CEs.M := ce
    dsp.RSTs.M := rst
  }

  def assign_p_ctrl(dsp: DSP48E2, ce: Bool = False, rst: Bool = False): Unit = {
    dsp.CEs.P := ce
    dsp.RSTs.P := rst
  }
}
