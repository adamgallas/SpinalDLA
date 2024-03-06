package xilinx.DSP48E2IntArithmetic.standalone

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class dpuczdx8g_acc() extends Component {

  val io = new Bundle {
    val inVld = in Bool()
    val accVld = in Bool()
    val p0 = in Bits (18 bits)
    val p1 = in Bits (18 bits)
    val p2 = in Bits (18 bits)
    val p3 = in Bits (18 bits)

    val biasRst = in Bool()
    val bias0 = in Bits (26 bits)
    val bias1 = in Bits (26 bits)

    val out0 = out Bits (29 bits)
    val out1 = out Bits (29 bits)
  }

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val p01 = io.p0.asSInt +^ io.p1.asSInt
  val p23 = io.p2.asSInt +^ io.p3.asSInt
  val p01Reg = RegNext(p01)
  val p23Reg = RegNext(p23)

  val attr = new DSP48E2Attributes
  set_mul_attr(attr)
  inmode.set_static_inmode_attr(attr)
  opmode.set_dynamic_opmode_attr(attr)
  ad_pack.set_ad_pack_attr(attr)
  ad_pack.set_static_b_attr(attr)
  c.set_mute_c_attr(attr)

  val dsp0 = new DSP48E2(attr)

  inmode.assign_static_inmode_ctrl(dsp0)
  inmode.assign_default(dsp0)
  opmode.assign_dynamic_opmode_ctrl(dsp0)
  w.w_sel_rnd(dsp0, False)
  x.x_sel_m(dsp0, io.inVld)
  y.y_sel_m(dsp0, io.inVld)
  z.z_sel_p(dsp0, io.accVld)
  ad_pack.assign_ad_pack_ctrl(dsp0)
  ad_pack.assign_static_b_ctrl(dsp0)
  c.assign_mute_c_ctrl(dsp0)

  assign_m_ctrl(dsp0, ce = True, rst = False)
  assign_p_ctrl(dsp0, ce = True, rst = False)

  dsp0.DATAIN.B := B(1,18 bits)
  dsp0.DATAIN.A := p01Reg.resize(30).asBits
  dsp0.DATAIN.D := io.bias0.asSInt.resize(27).asBits
  dsp0.RSTs.D := io.biasRst
  io.out0 := dsp0.DATAOUT.P.take(29)

  val dsp1 = new DSP48E2(attr)

  inmode.assign_static_inmode_ctrl(dsp1)
  inmode.assign_default(dsp1)
  opmode.assign_dynamic_opmode_ctrl(dsp1)
  w.w_sel_rnd(dsp1, False)
  x.x_sel_m(dsp1, io.inVld)
  y.y_sel_m(dsp1, io.inVld)
  z.z_sel_p(dsp1, io.accVld)
  ad_pack.assign_ad_pack_ctrl(dsp1)
  ad_pack.assign_static_b_ctrl(dsp1)
  c.assign_mute_c_ctrl(dsp1)

  assign_m_ctrl(dsp1, ce = True, rst = False)
  assign_p_ctrl(dsp1, ce = True, rst = False)

  dsp1.DATAIN.B := B(1,18 bits)
  dsp1.DATAIN.A := p23Reg.resize(30).asBits
  dsp1.DATAIN.D := io.bias1.asSInt.resize(27).asBits
  dsp1.RSTs.D := io.biasRst
  io.out1 := dsp1.DATAOUT.P.take(29)
}
