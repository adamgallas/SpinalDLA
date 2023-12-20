package xilinx.DSP48E2IntArithmetic.standalone

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int24_acc_scale() extends Component {
  val io = new Bundle {
    val a = in Bits (24 bits)
    val b = in Bits (24 bits)
    val scale = in Bits (18 bits)
    val valid = in Bool()
    val last = in Bool()

    val aAcc = out Bits (24 bits)
    val bAcc = out Bits (24 bits)
    val scaleRes = out Bits (48 bits)
  }

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val accLatency = 2
  val scaleLatency = 5

  val accValid = Bool().setAsReg().init(False)
  accValid.setWhen(io.valid).clearWhen(io.last)

  val ADCe = Delay(io.last, accLatency, init = False)
  val multEnable = Delay(ADCe || Delay(ADCe, 1, init = False), 1, init = False)
  val muteA = ADCe // high active
  val muteD = Delay(~ADCe, 1, init = True) // low active

  val attr = new DSP48E2Attributes

  set_mul_attr(attr)
  inmode.set_dynamic_inmode_attr(attr)
  opmode.set_dynamic_opmode_attr(attr)
  ad_pack.set_ad_pack_attr(attr)
  ad_pack.set_static_b_attr(attr)
  c.set_c_input_attr(attr)

  val dsp = new DSP48E2(attr)

  inmode.assign_dynamic_inmode_ctrl(dsp)
  inmode.assign_inmode_gate_ab(dsp, muteA)
  inmode.assign_inmode_gate_d(dsp, muteD)
  inmode.assign_inmode_add_sub(dsp, False)
  opmode.assign_dynamic_opmode_ctrl(dsp)
  w.w_sel_rnd(dsp, io.b.msb & io.valid)
  x.x_sel_ab(dsp, multEnable)
  y.y_sel_c_over_m(dsp, io.valid)
  z.z_sel_p(dsp, accValid)
  ad_pack.assign_ad_pack_ctrl(dsp, use_ad = false, ce = ADCe)
  ad_pack.assign_static_b_ctrl(dsp)
  c.assign_c_input_ctrl(dsp)

  assign_m_ctrl(dsp, ce = True, rst = False)
  assign_p_ctrl(dsp, ce = True, rst = False)

  val ab = io.a ## io.b
  val aAccBits = dsp.DATAOUT.P.drop(24)
  val bAccBits = dsp.DATAOUT.P.take(24)

  dsp.DATAIN.A := aAccBits.asSInt.resize(30 bits).asBits
  dsp.DATAIN.D := bAccBits.asSInt.resize(27 bits).asBits
  dsp.DATAIN.B := io.scale
  dsp.DATAIN.C := ab

  io.scaleRes := dsp.DATAOUT.P
  io.aAcc := aAccBits
  io.bAcc := bAccBits
}
