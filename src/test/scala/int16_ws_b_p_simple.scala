import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int16_ws_b_p_simple(length:Int) extends Component {

  val io = new Bundle {
    val a = in Bits (16 bits)
    val b = in Vec(Bits(16 bits), length)
    val p_out = out Bits (32 bits)

    val ena = in Vec(Bool(), length)
    val ena_d = in Vec(Bool(), length)

//    val enPrefetch = in Bool()
//    val enFetch = in Bool()
//    val clrPrefetch = in Vec(Bool(), length)
  }

  noIoPrefix()

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val latency = length + 4 - 1

  val attrs = Array.fill(length)(new DSP48E2Attributes)

  for (i <- 0 until length) {
    set_mul_attr(attrs(i))
    inmode.set_static_inmode_attr(attrs(i))
    opmode.set_static_opmode_attr(attrs(i))
    a.set_static_a_input_attr(attrs(i), 2)
    b.set_static_b_input_attr(attrs(i), 2)
    c.set_mute_c_attr(attrs(i))
    d.set_mute_d_attr(attrs(i))
    if (i != 0) b.set_b_cascade(attrs(i))
  }

  val dsp48e2s = attrs.map(attr => new DSP48E2(attr))
//  val enPrefetchChain = Vec(Bool(), length)
//  val enFetchChain = Vec(Bool(), length)

  for (i <- 0 until length) {
    inmode.assign_static_inmode_ctrl(dsp48e2s(i))
    inmode.assign_default(dsp48e2s(i))
    opmode.assign_static_opmode_ctrl(dsp48e2s(i))
    w.w_sel_c(dsp48e2s(i), False)
    x.x_sel_m(dsp48e2s(i), True)
    y.y_sel_m(dsp48e2s(i), True)
    z.z_sel_pcin(dsp48e2s(i), if (i != 0) True else False)
    a.assign_static_a_input_ctrl(dsp48e2s(i), 2)
    b.assign_b_pingpong_ctrl(dsp48e2s(i), io.ena(i), io.ena_d(i))
    c.assign_mute_c_ctrl(dsp48e2s(i))
    d.assign_mute_d_ctrl(dsp48e2s(i))
    assign_m_ctrl(dsp48e2s(i), ce = True, rst = False)
    assign_p_ctrl(dsp48e2s(i), ce = True, rst = False)

    dsp48e2s(i).DATAIN.A := io.b(i).asSInt.resize(30).asBits
    if (i == 0) {
      dsp48e2s(i).DATAIN.B := io.a.asSInt.resize(18).asBits
//      enPrefetchChain(i) := io.enPrefetch
//      enFetchChain(i) := io.enFetch
    }
    else {
      dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P
      dsp48e2s(i).CASCDATAIN.B := dsp48e2s(i - 1).CASCDATAOUT.B
//      enPrefetchChain(i).setAsReg()
//      enFetchChain(i).setAsReg().init(False)
//      enPrefetchChain(i) := enPrefetchChain(i - 1)
//      enFetchChain(i) := enFetchChain(i - 1)
//      enPrefetchChain(i).clearWhen(io.clrPrefetch(i))
    }
  }
  io.p_out := dsp48e2s.last.DATAOUT.P.resized
}

object int16_ws_b_p_simple extends App{
  val config = SpinalConfig(nameWhenByFile = false, anonymSignalPrefix = "t")
  config.generateVerilog(new int16_ws_b_p_simple(4))
}