package xilinx.DSP48E2IntArithmetic.dualCascade

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int16_os_B_P(length: Int) extends Component {

  val io = new Bundle {
    val a = in Bits (16 bits)
    val b = in Vec(Bits(16 bits), length)
    val ab = out Bits (48 bits)

    val accValid = in Bool()
    val accLast = in Bool()
  }

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val latency = length + 4 - 1

  val selOfM = Vec(Bool(), length)
  val selOfPCIN = Vec(Bool(), length)
  val selOfP = Vec(Bool(), length)
  val rst = Vec(Bool(), length + 1)

  val validDly = RegNext(io.accValid, False)
  val lastDly = Delay(io.accLast, 5, init = False)

  val attrs = Array.fill(length)(new DSP48E2Attributes)

  for (i <- 0 until length) {
    set_mul_attr(attrs(i))
    inmode.set_static_inmode_attr(attrs(i))
    opmode.set_dynamic_opmode_attr(attrs(i))
    a.set_static_a_input_attr(attrs(i), 2)
    b.set_static_b_input_attr(attrs(i), 2)
    c.set_mute_c_attr(attrs(i))
    d.set_mute_d_attr(attrs(i))
    if (i != 0) b.set_b_cascade(attrs(i))
  }

  val dsp48e2s = attrs.map(attr => new DSP48E2(attr))

  for (i <- 0 until length) {
    inmode.assign_static_inmode_ctrl(dsp48e2s(i))
    inmode.assign_default(dsp48e2s(i))
    opmode.assign_dynamic_opmode_ctrl(dsp48e2s(i))
    w.w_sel_p(dsp48e2s(i), selOfP(i))
    x.x_sel_m(dsp48e2s(i), selOfM(i))
    y.y_sel_m(dsp48e2s(i), selOfM(i))
    z.z_sel_pcin(dsp48e2s(i), selOfPCIN(i))
    a.assign_static_a_input_ctrl(dsp48e2s(i), 2)
    b.assign_static_b_input_ctrl(dsp48e2s(i), 2)
    c.assign_mute_c_ctrl(dsp48e2s(i))
    d.assign_mute_d_ctrl(dsp48e2s(i))
    assign_m_ctrl(dsp48e2s(i), ce = True, rst = False)
    assign_p_ctrl(dsp48e2s(i), ce = True, rst = rst(i + 1))

    dsp48e2s(i).DATAIN.A := io.b(i).asSInt.resize(30).asBits
    if (i == 0) {
      dsp48e2s(i).DATAIN.B := io.a.asSInt.resize(18).asBits
    }
    else {
      dsp48e2s(i).CASCDATAIN.B := dsp48e2s(i - 1).CASCDATAOUT.B
      dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P
    }

    selOfM(i).setAsReg().init(false)
    if (i == 0) selOfM(i) := validDly
    else selOfM(i) := selOfM(i - 1)

    rst(i).setAsReg().init(false)
    if (i == 0) rst(0) := lastDly
    else rst(i) := rst(i - 1)

    selOfPCIN(i).setAsReg().init(false)
    selOfPCIN(i).setWhen(lastDly).clearWhen(rst(i))

    selOfP(i).setAsReg().init(false)
    selOfP(i).setWhen(selOfM(i)).clearWhen(lastDly)
  }

  rst.last.setAsReg().init(false)
  rst.last := rst.dropRight(1).last

  io.ab := dsp48e2s.last.DATAOUT.P
}
