package example

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int12_ws_ab_c_p_clb(length: Int, width: Int) extends Component {

  val io = new Bundle {
    val aSel = in Vec(Bits(1 bits), length)
    val bSel = in Vec(Bits(1 bits), length)
    val a = in Vec(Bits(width bits), 4)
    val b = in Vec(Bits(width bits), 4)
    val ab = out Vec(Bits(12 bits), 4)

    val enPrefetch = in Bool()
    val enFetch = in Bool()
    val clrPrefetch = in Vec(Bool(), length)
  }

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  require(width <= 12)
  val latency = length + 2 - 1

  val attrs = Array.fill(length)(new DSP48E2Attributes)

  for (i <- 0 until length) {
    set_alu_attr(attrs(i), simd = 4)
    inmode.set_static_inmode_attr(attrs(i))
    opmode.set_dynamic_opmode_attr(attrs(i))
    ab_concat.set_ab_concat_attr(attrs(i))
    c.set_c_input_attr(attrs(i))
    d.set_mute_d_attr(attrs(i))
    if (i != 0) {
      a.set_a_cascade(attrs(i))
      b.set_b_cascade(attrs(i))
    }
  }

  val dsp48e2s = attrs.map(attr => new DSP48E2(attr))
  val enPrefetchChain = Vec(Bool(), length)
  val enFetchChain = Vec(Bool(), length)
  val aBits = io.a.asBits
  val bBits = io.b.asBits

  val abPortDataChain = Vec(Bits(width * 4 bits), length)
  val cPortDataChain = Vec(Bits(width * 4 bits), length)

  for (i <- 0 until length) {
    inmode.assign_static_inmode_ctrl(dsp48e2s(i))
    inmode.assign_default(dsp48e2s(i))
    opmode.assign_dynamic_opmode_ctrl(dsp48e2s(i))
    w.w_sel_p(dsp48e2s(i), False)
    x.x_sel_ab(dsp48e2s(i), io.aSel(i).msb)
    y.y_sel_c(dsp48e2s(i), io.bSel(i).msb)
    z.z_sel_pcin(dsp48e2s(i), if (i != 0) True else False)
    ab_concat.assign_ab_concat_ctrl(dsp48e2s(i), enFetchChain(i))
    c.assign_c_input_ctrl(dsp48e2s(i), enFetchChain(i))
    assign_m_ctrl(dsp48e2s(i))
    assign_p_ctrl(dsp48e2s(i), ce = True, rst = False)

    val ab = abPortDataChain(i).
      subdivideIn(4 slices).
      map(_.asSInt.resize(12 bits)).
      asBits()

    dsp48e2s(i).DATAIN.A := ab.drop(18)
    dsp48e2s(i).DATAIN.B := ab.take(18)

    dsp48e2s(i).DATAIN.C := cPortDataChain(i).
      subdivideIn(4 slices).
      map(_.asSInt.resize(12 bits)).
      asBits()

    abPortDataChain(i).setAsReg()
    cPortDataChain(i).setAsReg()
    if (i == 0) {
//      dsp48e2s(i).DATAIN.A := aBits.drop(18)
//      dsp48e2s(i).DATAIN.B := aBits.take(18)

      enPrefetchChain(i) := io.enPrefetch
      enFetchChain(i) := io.enFetch

      when(enPrefetchChain(i))(abPortDataChain(i) := aBits)
      when(enPrefetchChain(i))(cPortDataChain(i) := bBits)
    }
    else {
      dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P
//      dsp48e2s(i).CASCDATAIN.A := dsp48e2s(i - 1).CASCDATAOUT.A
//      dsp48e2s(i).CASCDATAIN.B := dsp48e2s(i - 1).CASCDATAOUT.B

      enPrefetchChain(i).setAsReg()
      enFetchChain(i).setAsReg().init(False)
      enPrefetchChain(i) := enPrefetchChain(i - 1)
      enFetchChain(i) := enFetchChain(i - 1)
      enPrefetchChain(i).clearWhen(io.clrPrefetch(i))

      when(enPrefetchChain(i))(abPortDataChain(i) := abPortDataChain(i - 1))
      when(enPrefetchChain(i))(cPortDataChain(i) := cPortDataChain(i - 1))
    }
  }

  io.ab.assignFromBits(dsp48e2s.last.DATAOUT.P)
}
