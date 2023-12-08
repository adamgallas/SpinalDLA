package xilinx.DSP48E2IntArithmetic.standalone

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int16_mul() extends Component {
  val io = new Bundle {
    val a = in Bits (16 bits)
    val b = in Bits (16 bits)
    val ab = out Bits (32 bits)
  }

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val latency = 4
  val attr = new DSP48E2Attributes

  set_mul_attr(attr)
  inmode.set_static_inmode_attr(attr)
  opmode.set_static_opmode_attr(attr)
  a.set_static_a_input_attr(attr,2)
  b.set_static_b_input_attr(attr,2)
  c.set_mute_c_attr(attr)
  d.set_mute_d_attr(attr)

  val dsp = new DSP48E2(attr)

  inmode.assign_static_inmode_ctrl(dsp)
  inmode.assign_default(dsp)
  opmode.assign_static_opmode_ctrl(dsp)
  w.w_sel_c(dsp,False)
  x.x_sel_m(dsp,True)
  y.y_sel_m(dsp,True)
  z.z_sel_p(dsp,False)
  a.assign_static_a_input_ctrl(dsp,2)
  b.assign_static_b_input_ctrl(dsp,2)
  c.assign_mute_c_ctrl(dsp)
  d.assign_mute_d_ctrl(dsp)

  assign_m_ctrl(dsp, ce = True, rst = False)
  assign_p_ctrl(dsp, ce = True, rst = False)

  dsp.DATAIN.A := io.b.asSInt.resize(30).asBits
  dsp.DATAIN.B := io.a.asSInt.resize(18).asBits
  io.ab := dsp.DATAOUT.P.take(32)
}