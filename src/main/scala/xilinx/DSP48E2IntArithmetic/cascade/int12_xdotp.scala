package xilinx.DSP48E2IntArithmetic.cascade

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class int12_xdotp(length: Int, acc: Boolean = false) extends Component {

  val io = new Bundle {
    val aSel = in Vec(Bits(1 bits), length)
    val bSel = in Vec(Bits(1 bits), length)
    val a = in Vec(Vec(Bits(12 bits), 4), length)
    val b = in Vec(Vec(Bits(12 bits), 4), length)
    val ab = out Vec(Bits(12 bits), 4)

    val valid = if (acc) in Bool() else null
    val last = if (acc) in Bool() else null
  }

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val latency = length + 2 - 1

  val lastDSPAccValid = if (acc) Bool().setAsReg().init(False) else null
  if (acc) lastDSPAccValid.setWhen(io.valid).clearWhen(io.last)

  val attrs = Array.fill(length)(new DSP48E2Attributes)

  for (i <- 0 until length) {
    set_alu_attr(attrs(i), simd = 4)
    inmode.set_static_inmode_attr(attrs(i))
    opmode.set_dynamic_opmode_attr(attrs(i))
    ab_concat.set_static_ab_concat_attr(attrs(i))
    c.set_c_input_attr(attrs(i))
    d.set_mute_d_attr(attrs(i))
  }

  val dsp48e2s = attrs.map(attr => new DSP48E2(attr))

  for (i <- 0 until length) {
    val accCond = i == length - 1 && acc

    inmode.assign_static_inmode_ctrl(dsp48e2s(i))
    inmode.assign_default(dsp48e2s(i))
    opmode.assign_dynamic_opmode_ctrl(dsp48e2s(i))
    w.w_sel_p(dsp48e2s(i), if (accCond) lastDSPAccValid else False)
    x.x_sel_ab(dsp48e2s(i), io.aSel(i).msb)
    y.y_sel_c(dsp48e2s(i), io.bSel(i).msb)
    z.z_sel_pcin(dsp48e2s(i), if (i != 0) if (accCond) io.valid else True else False)
    ab_concat.assign_static_ab_concat_ctrl(dsp48e2s(i))
    c.assign_c_input_ctrl(dsp48e2s(i))
    d.assign_mute_d_ctrl(dsp48e2s(i))

    assign_m_ctrl(dsp48e2s(i))
    assign_p_ctrl(dsp48e2s(i), ce = True, rst = False)

    val AB = io.a(i).asBits
    dsp48e2s(i).DATAIN.A := AB.drop(18)
    dsp48e2s(i).DATAIN.B := AB.take(18)
    dsp48e2s(i).DATAIN.C := io.b(i).asBits
    if (i != 0) dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P

    if(accCond){
      dsp48e2s(i).addGeneric("IS_RSTA_INVERTED", "1'b1")
      dsp48e2s(i).addGeneric("IS_RSTB_INVERTED", "1'b1")
      dsp48e2s(i).addGeneric("IS_RSTC_INVERTED", "1'b1")
      dsp48e2s(i).RSTs.A.removeAssignments()
      dsp48e2s(i).RSTs.B.removeAssignments()
      dsp48e2s(i).RSTs.C.removeAssignments()
      dsp48e2s(i).RSTs.A := io.valid
      dsp48e2s(i).RSTs.B := io.valid
      dsp48e2s(i).RSTs.C := io.valid
    }
  }

  val P = dsp48e2s.last.DATAOUT.P
  io.ab.assignFromBits(P)
}
