package xilinx.DSP48E2IntArithmetic.cascade

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int8_dotp_ddr(length: Int, acc: Boolean = false) extends Component {

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
    inmode.set_dynamic_inmode_attr(attrs(i))
    opmode.set_static_opmode_attr(attrs(i))
    ad_pack.set_ad_pack_attr(attrs(i))
    b.set_time_multiplex_b_input_attr(attrs(i))
    c.set_mute_c_attr(attrs(i))
  }

  val dsp48e2s = attrs.map(attr => new DSP48E2(attr))
  val ce1 = Vec(Bool(), length)
  val ce2 = Vec(Bool(), length)
  val selMux = Vec(Bool(), length)

  val cnt = UInt(2 bits).setAsReg().init(0)
  cnt := cnt + 1
  ce1.head := RegNext(cnt === 3, init = False)
  ce2.head := RegNext(cnt === 1, init = False)

  selMux.head.setAsReg().init(False)
  selMux.head.toggleWhen(True)

  for (i <- 0 until length) {

    inmode.assign_dynamic_inmode_ctrl(dsp48e2s(i))
    inmode.assign_default(dsp48e2s(i))
    opmode.assign_static_opmode_ctrl(dsp48e2s(i))
    w.w_sel_p(dsp48e2s(i), False)
    x.x_sel_m(dsp48e2s(i), True)
    y.y_sel_m(dsp48e2s(i), True)
    z.z_sel_pcin(dsp48e2s(i), if (i != 0) True else False)

    ad_pack.assign_ad_pack_ctrl(dsp48e2s(i))
    b.assign_time_multiplex_b_input_ctrl(dsp48e2s(i), ce1(i), ce2(i), selMux(i))
    c.assign_mute_c_ctrl(dsp48e2s(i))

    assign_m_ctrl(dsp48e2s(i), ce = True, rst = False)
    assign_p_ctrl(dsp48e2s(i), ce = True, rst = False)

    dsp48e2s(i).DATAIN.A := io.b(i).asSInt.resize(30).asBits
    dsp48e2s(i).DATAIN.B := io.a(i).asSInt.resize(18).asBits
    dsp48e2s(i).DATAIN.D := io.c(i).asSInt.expand ## B(27 - 9 bits, default -> false)

    if (i != 0) {
      dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P
      ce1(i).setAsReg()
      ce2(i).setAsReg()
      selMux(i).setAsReg()
      ce1(i) := ce1(i - 1)
      ce2(i) := ce2(i - 1)
      selMux(i) := selMux(i - 1)
    }
  }

  val P = dsp48e2s.last.DATAOUT.P
  val abRes = P(17 downto 0).asBits
  val abNeg = B"0" ## abRes.msb
//  val acRes = P(35 downto 18).asSInt + abNeg.asSInt
  val acRes = P(35 downto 18).asSInt

  io.ab := abRes
  io.ac := acRes.asBits
}
