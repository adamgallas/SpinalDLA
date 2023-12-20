package xilinx.DSP48E2IntArithmetic.standalone

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int24_acc() extends Component {
  val io = new Bundle {
    val a = in Bits (24 bits)
    val b = in Bits (24 bits)
    val c = in Bits (24 bits)
    val d = in Bits (24 bits)

    val ab = out Bits (24 bits)
    val cd = out Bits (24 bits)

    val valid = in Bool()
    val last = in Bool()
  }

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val accValid = Bool().setAsReg().init(False)
  accValid.setWhen(io.valid).clearWhen(io.last)

  val latency = 2
  val attr = new DSP48E2Attributes

  set_alu_attr(attr,simd = 2)
  inmode.set_static_inmode_attr(attr)
  opmode.set_dynamic_opmode_attr(attr)
  ab_concat.set_ab_concat_attr(attr)
  c.set_c_input_attr(attr)
  d.set_mute_d_attr(attr)

  val dsp = new DSP48E2(attr)

  inmode.assign_static_inmode_ctrl(dsp)
  inmode.assign_default(dsp)
  opmode.assign_dynamic_opmode_ctrl(dsp)
  w.w_sel_c(dsp, io.valid)
  x.x_sel_ab(dsp, io.valid)
  y.y_sel_c(dsp, False)
  z.z_sel_p(dsp, accValid)
  ab_concat.assign_ab_concat_ctrl(dsp)
  c.assign_c_input_ctrl(dsp)
  d.assign_mute_d_ctrl(dsp)

  assign_m_ctrl(dsp)
  assign_p_ctrl(dsp, ce = True, rst = False)

  val ac = io.a ## io.c
  val bd = io.b ## io.d
  dsp.DATAIN.A := ac.drop(18)
  dsp.DATAIN.B := ac.take(18)
  dsp.DATAIN.C := bd

  io.ab := dsp.DATAOUT.P.drop(24)
  io.cd := dsp.DATAOUT.P.take(24)
}
