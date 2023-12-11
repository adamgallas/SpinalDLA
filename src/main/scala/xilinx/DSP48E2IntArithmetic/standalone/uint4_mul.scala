package xilinx.DSP48E2IntArithmetic.standalone

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class uint4_mul() extends Component {
  val io = new Bundle {
    val w1 = in Bits (4 bits)
    val w2 = in Bits (4 bits)
    val a1 = in Bits (4 bits)
    val a2 = in Bits (4 bits)
    val a1w1 = out Bits (8 bits)
    val a1w2 = out Bits (8 bits)
    val a2w1 = out Bits (8 bits)
    val a2w2 = out Bits (8 bits)
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
  c.set_mute_c_attr(attr)

  val dsp = new DSP48E2(attr)

  inmode.assign_static_inmode_ctrl(dsp)
  inmode.assign_default(dsp)
  opmode.assign_static_opmode_ctrl(dsp)
  w.w_sel_c(dsp, False)
  x.x_sel_m(dsp, True)
  y.y_sel_m(dsp, True)
  z.z_sel_p(dsp, False)
  ad_pack.assign_static_ad_pack_ctrl(dsp)
  ad_pack.assign_static_b_ctrl(dsp)
  c.assign_mute_c_ctrl(dsp)

  assign_m_ctrl(dsp, ce = True, rst = False)
  assign_p_ctrl(dsp, ce = True, rst = False)

  dsp.DATAIN.A := io.w1.asSInt.resize(30).asBits
  dsp.DATAIN.B := B"000" ## io.a2 ## B"0000000" ## io.a1
  dsp.DATAIN.D := io.w2.msb ## io.w2 ## B(22 bits, default -> False)

  val P = dsp.DATAOUT.P

  val sa1w1 = P(7 downto 0).asSInt
  val sa2w1 = P(18 downto 11).asSInt
  val sa1w2 = P(29 downto 22).asSInt
  val sa2w2 = P(40 downto 33).asSInt

  io.a1w1 := sa1w1.asBits
  io.a2w1 := sa2w1.asBits
  io.a1w2 := sa1w2.asBits
  io.a2w2 := sa2w2.asBits
}