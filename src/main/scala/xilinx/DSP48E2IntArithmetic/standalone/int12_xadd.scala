package xilinx.DSP48E2IntArithmetic.standalone

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int12_xadd extends Component {

  val io = new Bundle {
    val aSel = in Bits (1 bits)
    val bSel = in Bits (1 bits)
    val a = in Vec(Bits(12 bits), 4)
    val b = in Vec(Bits(12 bits), 4)
    val ab = out Vec(Bits(12 bits), 4)
  }

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val latency = 2
  val attr = new DSP48E2Attributes

  set_alu_attr(attr, simd = 4)
  inmode.set_static_inmode_attr(attr)
  opmode.set_dynamic_opmode_attr(attr)
  ab_concat.set_ab_concat_attr(attr)
  c.set_c_input_attr(attr)
  d.set_mute_d_attr(attr)

  val dsp = new DSP48E2(attr)

  inmode.assign_static_inmode_ctrl(dsp)
  inmode.assign_default(dsp)
  opmode.assign_dynamic_opmode_ctrl(dsp)
  w.w_sel_c(dsp, io.bSel.msb)
  x.x_sel_ab(dsp, io.aSel.msb)
  y.y_sel_c(dsp, False)
  z.z_sel_p(dsp, False)
  ab_concat.assign_ab_concat_ctrl(dsp)
  c.assign_c_input_ctrl(dsp)
  d.assign_mute_d_ctrl(dsp)

  assign_m_ctrl(dsp)
  assign_p_ctrl(dsp, ce = True, rst = False)

  val AB = io.a.asBits
  dsp.DATAIN.A := AB.drop(18)
  dsp.DATAIN.B := AB.take(18)
  dsp.DATAIN.C := io.b.asBits

  val P = dsp.DATAOUT.P
  io.ab.assignFromBits(P)
}
