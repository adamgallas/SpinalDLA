package example

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._
import systolic._
import xilinx.DSP48E2IntArithmetic.dualCascade.int8_ws_B_P

import scala.language.postfixOps

class tpu14_pe(
                isLastPEHorizontal: Boolean = false
              ) extends Component {

  case class inpBdl() extends Bundle {
    val a0 = in Bits (8 bits)
    val enPrefetch0 = in Bool()
    val enFetch0 = in Bool()
    val clrPrefetch0 = in Vec(Bool(), 7)

    val a1 = in Bits (8 bits)
    val enPrefetch1 = in Bool()
    val enFetch1 = in Bool()
    val clrPrefetch1 = in Vec(Bool(), 7)
  }

  case class outBdl() extends Bundle {
    val out = Bits(48 bits)
  }

  case class horizBdl() extends Bundle {
    val b0 = in Vec(Bits(8 bits), 7)
    val c0 = in Vec(Bits(8 bits), 7)

    val b1 = in Vec(Bits(8 bits), 7)
    val c1 = in Vec(Bits(8 bits), 7)
  }

  val inp = new inpBdl().asInput()
  val out = new outBdl().asOutput()
  val srcH = new horizBdl().asInput()
  val dstH = if (!isLastPEHorizontal) new horizBdl().asOutput() else null

  val srcHDly = RegNext(srcH)
  if (!isLastPEHorizontal) dstH := srcHDly

  val chain0 = new int8_ws_B_P(7)
  val chain1 = new int8_ws_B_P(7)

  chain0.io.a := inp.a0
  chain0.io.enPrefetch := inp.enPrefetch0
  chain0.io.enFetch := inp.enFetch0
  chain0.io.clrPrefetch := inp.clrPrefetch0

  chain1.io.a := inp.a1
  chain1.io.enPrefetch := inp.enPrefetch1
  chain1.io.enFetch := inp.enFetch1
  chain1.io.clrPrefetch := inp.clrPrefetch1

  chain0.io.b := srcHDly.b0
  chain0.io.c := srcHDly.c0
  chain1.io.b := srcHDly.b1
  chain1.io.c := srcHDly.c1

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val attr = new DSP48E2Attributes

  set_alu_attr(attr, simd = 2)
  inmode.set_static_inmode_attr(attr)
  opmode.set_static_opmode_attr(attr)
  ab_concat.set_ab_concat_attr(attr)
  c.set_c_input_attr(attr)
  d.set_mute_d_attr(attr)

  val dsp = new DSP48E2(attr)

  inmode.assign_static_inmode_ctrl(dsp)
  inmode.assign_default(dsp)
  opmode.assign_static_opmode_ctrl(dsp)
  w.w_sel_c(dsp, True)
  x.x_sel_ab(dsp, True)
  y.y_sel_c(dsp, False)
  z.z_sel_p(dsp, False)
  ab_concat.assign_ab_concat_ctrl(dsp)
  c.assign_c_input_ctrl(dsp)
  d.assign_mute_d_ctrl(dsp)

  assign_m_ctrl(dsp)
  assign_p_ctrl(dsp, ce = True, rst = False)
  out.out := dsp.DATAOUT.P

  val ab0 = RegNext(chain0.io.ab)
  val ac0 = RegNext(chain0.io.ac)
  val ab1 = RegNext(chain1.io.ab)
  val ac1 = RegNext(chain1.io.ac)

  val pack0 = ab0.asSInt.resize(24 bits) ## ac0.asSInt.resize(24 bits)
  val pack1 = ab1.asSInt.resize(24 bits) ## ac1.asSInt.resize(24 bits)

  dsp.DATAIN.A := pack0.drop(18)
  dsp.DATAIN.B := pack0.take(18)
  dsp.DATAIN.C := pack1
}
