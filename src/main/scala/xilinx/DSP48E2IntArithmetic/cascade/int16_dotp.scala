package xilinx.DSP48E2IntArithmetic.cascade

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int16_dotp(length: Int, acc: Boolean = false) extends Component {

  val io = new Bundle {
    val a = in Vec(Bits(16 bits), length)
    val b = in Vec(Bits(16 bits), length)
    val ab = out Bits (48 bits)

    val valid = if (acc) in Bool() else null
    val last = if (acc) in Bool() else null
  }

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val latency = length + 4 - 1

  val valid = if (acc) Delay(io.valid, 2, init = False) else null
  val last = if (acc) Delay(io.last, 2, init = False) else null
  val lastDSPAccValid = if (acc) Bool().setAsReg().init(False) else null
  if (acc) {
    lastDSPAccValid.setWhen(valid).clearWhen(last)
  }


  val attrs = Array.fill(length)(new DSP48E2Attributes)

  for (i <- 0 until length) {
    set_mul_attr(attrs(i))
    inmode.set_static_inmode_attr(attrs(i))
    opmode.set_static_opmode_attr(attrs(i))
    a.set_static_a_input_attr(attrs(i), 2)
    b.set_static_b_input_attr(attrs(i), 2)
    c.set_mute_c_attr(attrs(i))
    d.set_mute_d_attr(attrs(i))
  }

  val dsp48e2s = attrs.map(attr => new DSP48E2(attr))

  for (i <- 0 until length) {
    val accCond = i == length - 1 && acc

    inmode.assign_static_inmode_ctrl(dsp48e2s(i))
    inmode.assign_default(dsp48e2s(i))
    opmode.assign_static_opmode_ctrl(dsp48e2s(i))
    w.w_sel_p(dsp48e2s(i), if (accCond) lastDSPAccValid else False)
    x.x_sel_m(dsp48e2s(i), if (accCond) valid else True)
    y.y_sel_m(dsp48e2s(i), if (accCond) valid else True)
    z.z_sel_pcin(dsp48e2s(i), if (i != 0) if (accCond) valid else True else False)
    a.assign_static_a_input_ctrl(dsp48e2s(i), 2)
    b.assign_static_b_input_ctrl(dsp48e2s(i), 2)
    c.assign_mute_c_ctrl(dsp48e2s(i))
    d.assign_mute_d_ctrl(dsp48e2s(i))

    assign_m_ctrl(dsp48e2s(i), ce = True, rst = False)
    assign_p_ctrl(dsp48e2s(i), ce = True, rst = False)

    dsp48e2s(i).DATAIN.A := io.b(i).asSInt.resize(30).asBits
    dsp48e2s(i).DATAIN.B := io.a(i).asSInt.resize(18).asBits

    if (i != 0) dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P

  }

  io.ab := dsp48e2s.last.DATAOUT.P
}
