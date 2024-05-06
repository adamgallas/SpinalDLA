package example

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int8_ws_b_p_clb(length: Int) extends Component {

  val io = new Bundle {
    val a = in Bits (8 bits)
    val b = in Vec(Bits(8 bits), length)
    val c = in Vec(Bits(8 bits), length)
    val ab = out Bits (18 bits)
    val ac = out Bits (18 bits)

    val enPrefetch = in Bool()
    val enFetch = in Bool()
    val clrPrefetch = in Vec(Bool(), length)
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
    ad_pack.set_ad_pack_attr(attrs(i))
    b.set_static_b_input_attr(attrs(i), 1)
//    ad_pack.set_pingpong_b_attr(attrs(i))
    c.set_mute_c_attr(attrs(i))
    if (i != 0) b.set_b_cascade(attrs(i))
  }

  val dsp48e2s = attrs.map(attr => new DSP48E2(attr))
  val breg = Vec(Bits(8 bits), length)
  val enPrefetchChain = Vec(Bool(), length)
  val enFetchChain = Vec(Bool(), length)

  breg.foreach(_.setAsReg())

  for (i <- 0 until length) {
    inmode.assign_static_inmode_ctrl(dsp48e2s(i))
    inmode.assign_default(dsp48e2s(i))
    opmode.assign_static_opmode_ctrl(dsp48e2s(i))
    w.w_sel_c(dsp48e2s(i), False)
    x.x_sel_m(dsp48e2s(i), True)
    y.y_sel_m(dsp48e2s(i), True)
    z.z_sel_pcin(dsp48e2s(i), if (i != 0) True else False)
    ad_pack.assign_ad_pack_ctrl(dsp48e2s(i))
//    ad_pack.assign_pingpong_b_ctrl(dsp48e2s(i), enPrefetchChain(i), enFetchChain(i))
    dsp48e2s(i).CEs.B1 := enFetchChain(i)
    inmode.assign_inmode_b(dsp48e2s(i), high4b1 = True)

    c.assign_mute_c_ctrl(dsp48e2s(i))
    assign_m_ctrl(dsp48e2s(i), ce = True, rst = False)
    assign_p_ctrl(dsp48e2s(i), ce = True, rst = False)

    dsp48e2s(i).DATAIN.B := breg(i).asSInt.resize(18).asBits
    dsp48e2s(i).DATAIN.A := io.b(i).asSInt.resize(30).asBits
    dsp48e2s(i).DATAIN.D := io.c(i).asSInt.expand ## B(27 - 9 bits, default -> false)

    if (i == 0) {
//      dsp48e2s(i).DATAIN.B := io.a.asSInt.resize(18).asBits
      when(enPrefetchChain(i)){
        breg(i) := io.a
      }

      enPrefetchChain(i) := io.enPrefetch
      enFetchChain(i) := io.enFetch
    }
    else {
      dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P
//      dsp48e2s(i).CASCDATAIN.B := dsp48e2s(i - 1).CASCDATAOUT.B
      when(enPrefetchChain(i)){
        breg(i) := breg(i - 1)
      }

      enPrefetchChain(i).setAsReg()
      enFetchChain(i).setAsReg().init(False)
      enPrefetchChain(i) := enPrefetchChain(i - 1)
      enFetchChain(i) := enFetchChain(i - 1)
      enPrefetchChain(i).clearWhen(io.clrPrefetch(i))
    }
  }

  val P = dsp48e2s.last.DATAOUT.P
  val abRes = P(17 downto 0).asBits
  val abNeg = B"0" ## abRes.msb
  val acRes = P(35 downto 18).asSInt

  io.ab := abRes
  io.ac := acRes.asBits
}
