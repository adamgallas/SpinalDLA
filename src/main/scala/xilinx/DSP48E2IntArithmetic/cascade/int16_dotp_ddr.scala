package xilinx.DSP48E2IntArithmetic.cascade

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int16_dotp_ddr(length: Int, acc: Boolean = false) extends Component {

  val io = new Bundle {
    val a = in Vec(Bits(16 bits), length)
    val b = in Vec(Bits(16 bits), length)
    val ab = out Bits (48 bits)
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
    a.set_time_multiplex_a_input_attr(attrs(i))
    b.set_static_b_input_attr(attrs(i), 2)
    c.set_mute_c_attr(attrs(i))
    d.set_mute_d_attr(attrs(i))
  }

  val dsp48e2s = attrs.map(attr => new DSP48E2(attr))
  val ce1 = Vec(Bool(), length)
  val ce2 = Vec(Bool(), length)
  val selA = Vec(Bool(), length)
  val selB = Vec(Bool(), length)

  val cnt = UInt(2 bits).setAsReg().init(0)
  cnt := cnt + 1
  ce1.head := RegNext(cnt === 3, init = False)
  ce2.head := RegNext(cnt === 1, init = False)

  selA.head.setAsReg().init(False)
  selA.head.toggleWhen(True)

  for (i <- 0 until length) {

    inmode.assign_dynamic_inmode_ctrl(dsp48e2s(i))
    inmode.assign_default(dsp48e2s(i))
    opmode.assign_static_opmode_ctrl(dsp48e2s(i))
    w.w_sel_p(dsp48e2s(i), False)
    x.x_sel_m(dsp48e2s(i), True)
    y.y_sel_m(dsp48e2s(i), True)
    z.z_sel_pcin(dsp48e2s(i), if (i != 0) True else False)
    a.assign_time_multiplex_a_input_ctrl(dsp48e2s(i), ce1(i), ce2(i), selA(i))
    b.assign_static_b_input_ctrl(dsp48e2s(i),2)
    c.assign_mute_c_ctrl(dsp48e2s(i))
    d.assign_mute_d_ctrl(dsp48e2s(i))

    assign_m_ctrl(dsp48e2s(i), ce = True, rst = False)
    assign_p_ctrl(dsp48e2s(i), ce = True, rst = False)

    dsp48e2s(i).DATAIN.A := io.b(i).asSInt.resize(30).asBits
    dsp48e2s(i).DATAIN.B := io.a(i).asSInt.resize(18).asBits

    if (i != 0) {
      dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P
      ce1(i).setAsReg()
      ce2(i).setAsReg()
      selA(i).setAsReg()
      selB(i).setAsReg()
      ce1(i) := ce1(i - 1)
      ce2(i) := ce2(i - 1)
      selA(i) := selA(i - 1)
      selB(i) := selB(i - 1)
    }
  }

  io.ab := dsp48e2s.last.DATAOUT.P
}
