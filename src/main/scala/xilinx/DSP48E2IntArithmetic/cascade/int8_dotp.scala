package xilinx.DSP48E2IntArithmetic.cascade

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int8_dotp(length: Int) extends Component {

  val io = new Bundle {
    val a = in Vec(Bits(8 bits), length)
    val b = in Vec(Bits(8 bits), length)
    val c = in Vec(Bits(8 bits), length)
    val ab = out Bits (18 bits)
    val ac = out Bits (18 bits)
  }

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val latency = length + 4 - 1

  val attrs = Array.fill(length)(new DSP48E2Attributes)

  for (i <- 0 until length) {
    set_mul_attr(attrs(i))
    inmode.set_static_inmode_attr(attrs(i))
    opmode.set_static_opmode_attr(attrs(i))
    ad_pack.set_static_ad_pack_attr(attrs(i))
    ad_pack.set_static_b_attr(attrs(i))
    c.set_mute_c_attr(attrs(i))
  }

  val dsp48e2s = attrs.map(attr => new DSP48E2(attr))

  for (i <- 0 until length) {
    inmode.assign_static_inmode_ctrl(dsp48e2s(i))
    inmode.assign_default(dsp48e2s(i))
    opmode.assign_static_opmode_ctrl(dsp48e2s(i))
    w.w_sel_c(dsp48e2s(i), False)
    x.x_sel_m(dsp48e2s(i), True)
    y.y_sel_m(dsp48e2s(i), True)
    z.z_sel_pcin(dsp48e2s(i), if (i != 0) True else False)
    ad_pack.assign_static_ad_pack_ctrl(dsp48e2s(i))
    ad_pack.assign_static_b_ctrl(dsp48e2s(i))
    c.assign_mute_c_ctrl(dsp48e2s(i))
    assign_m_ctrl(dsp48e2s(i), ce = True, rst = False)
    assign_p_ctrl(dsp48e2s(i), ce = True, rst = False)

    dsp48e2s(i).DATAIN.A := io.b(i).asSInt.resize(30).asBits
    dsp48e2s(i).DATAIN.B := io.a(i).asSInt.resize(18).asBits
    dsp48e2s(i).DATAIN.D := io.c(i).asSInt.expand ## B(27 - 9 bits, default -> false)
    if (i != 0) dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P
  }

  val P = dsp48e2s.last.DATAOUT.P
  val abRes = P(17 downto 0).asBits
  val abNeg = B"0" ## abRes.msb
  val acRes = P(35 downto 18).asSInt + abNeg.asSInt

  io.ab := abRes
  io.ac := acRes.asBits
}
