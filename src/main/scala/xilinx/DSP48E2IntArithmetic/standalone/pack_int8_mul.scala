package xilinx.DSP48E2IntArithmetic.standalone

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class pack_int8_mul extends Component {
  val io = new Bundle {
    val a = in Bits (8 bits)
    val b = in Bits (8 bits)
    val c = in Bits (8 bits)
    val ab = out Bits (16 bits)
    val ac = out Bits (16 bits)
  }

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val latency = 4
  val attr = new DSP48E2Attributes

  set_mul_attr(attr)
  inmode.set_static_inmode_attr(attr)
  opmode.set_static_opmode_attr(attr)
  ad_pack.set_static_ad_pack_attr(attr)
  ad_pack.set_static_b_attr(attr)
  c.set_c_input_attr(attr)

  val dsp = new DSP48E2(attr)

  inmode.assign_static_inmode_ctrl(dsp)
  inmode.assign_default(dsp)
  opmode.assign_static_opmode_ctrl(dsp)
  w.w_sel_c(dsp, True)
  x.x_sel_m(dsp, True)
  y.y_sel_m(dsp, True)
  z.z_sel_p(dsp, False)
  ad_pack.assign_static_ad_pack_ctrl(dsp)
  ad_pack.assign_static_b_ctrl(dsp)
  c.assign_c_input_ctrl(dsp)

  assign_m_ctrl(dsp, ce = True, rst = False)
  assign_p_ctrl(dsp, ce = True, rst = False)

  val abNeg = io.a.orR & io.b.orR & (io.a.msb ^ io.b.msb)
  val abNegReg = Delay(abNeg, 2)
  dsp.DATAIN.A := io.b.asSInt.resize(30).asBits
  dsp.DATAIN.B := io.a.asSInt.resize(18).asBits
  dsp.DATAIN.D := io.c.asSInt.expand ## B(27 - 9 bits, default -> false)
  dsp.DATAIN.C := B(29 bits, default -> false) ## abNegReg ## B(18 bits, default -> false)

  val P = dsp.DATAOUT.P
  io.ab := P.take(16)
  io.ac := P.drop(18).take(16)
}
