package xilinx.DSP48E2IntArithmetic.dualCascade

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int8_ws_AD_B(length: Int) extends Component {

  val io = new Bundle {
    val a = in Bits (8 bits)
    val b = in Bits (8 bits)
    val c = in Bits (8 bits)
    val abIn = in Vec(Bits(18 bits), length)
    val acIn = in Vec(Bits(18 bits), length)
    val abOut = out Vec(Bits(18 bits), length)
    val acOut = out Vec(Bits(18 bits), length)

    val enPrefetch = in Bool()
    val enFetch = in Bool()
    val clrPrefetch = in Vec(Bool(), length)
  }

  io.abIn.foreach(_.default(B(0, 8 bits)))
  io.acIn.foreach(_.default(B(0, 8 bits)))

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val attrs = Array.fill(length)(new DSP48E2Attributes)

  for (i <- 0 until length) {
    set_mul_attr(attrs(i))
    inmode.set_static_inmode_attr(attrs(i))
    opmode.set_static_opmode_attr(attrs(i))
    ad_pack.set_ad_pack_attr(attrs(i))
    ad_pack.set_pingpong_b_attr(attrs(i))
    c.set_c_input_attr(attrs(i))
    if (i != 0) a.set_a_cascade(attrs(i))
    if (i != 0) b.set_b_cascade(attrs(i))
  }

  val dsp48e2s = attrs.map(attr => new DSP48E2(attr))
  val enPrefetchChain = Vec(Bool(), length)
  val enFetchChain = Vec(Bool(), length)
  val dPortDataChain = Vec(Bits(8 bits), length)

  for (i <- 0 until length) {
    inmode.assign_static_inmode_ctrl(dsp48e2s(i))
    inmode.assign_default(dsp48e2s(i))
    opmode.assign_static_opmode_ctrl(dsp48e2s(i))
    w.w_sel_c(dsp48e2s(i), True)
    x.x_sel_m(dsp48e2s(i), True)
    y.y_sel_m(dsp48e2s(i), True)
    z.z_sel_pcin(dsp48e2s(i), False)
    ad_pack.assign_ad_pack_ctrl(dsp48e2s(i))
    ad_pack.assign_pingpong_b_ctrl(dsp48e2s(i), enPrefetchChain(i), enFetchChain(i))
    c.assign_c_input_ctrl(dsp48e2s(i))
    assign_m_ctrl(dsp48e2s(i), ce = True, rst = False)
    assign_p_ctrl(dsp48e2s(i), ce = True, rst = False)

    dsp48e2s(i).DATAIN.D := dPortDataChain(i).asSInt.expand ## B(27 - 9 bits, default -> false)
    dsp48e2s(i).DATAIN.C := io.acIn(i).asSInt.resize(30 bits) ## io.abIn(i)
    io.abOut(i) := dsp48e2s(i).DATAOUT.P(17 downto 0)
    io.acOut(i) := dsp48e2s(i).DATAOUT.P(35 downto 18)

    a.assign_a_cascade(dsp48e2s(i))

    if (i == 0) {
      dPortDataChain(i) := io.c
      dsp48e2s(i).DATAIN.A := io.b.asSInt.resize(30).asBits
      dsp48e2s(i).DATAIN.B := io.a.asSInt.resize(18).asBits
      enPrefetchChain(i) := io.enPrefetch
      enFetchChain(i) := io.enFetch
    }
    else {
      dsp48e2s(i).CASCDATAIN.A := dsp48e2s(i - 1).CASCDATAOUT.A
      dsp48e2s(i).CASCDATAIN.B := dsp48e2s(i - 1).CASCDATAOUT.B
      dPortDataChain(i).setAsReg()
      dPortDataChain(i) := dPortDataChain(i - 1)
      enPrefetchChain(i).setAsReg()
      enFetchChain(i).setAsReg().init(False)
      enPrefetchChain(i) := enPrefetchChain(i - 1)
      enFetchChain(i) := enFetchChain(i - 1)
      enPrefetchChain(i).clearWhen(io.clrPrefetch(i))
    }
  }
}
