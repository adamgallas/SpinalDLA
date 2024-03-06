package xilinx.DSP48E2IntArithmetic.standalone

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._

import scala.language.postfixOps

class ring_acc() extends Component {

  val io = new Bundle {
    val inVld = in Bool()
    val fbVld = in Bool()
    val p0 = in Bits (24 bits)
    val p1 = in Bits (24 bits)
    val p2 = in Bits (24 bits)
    val p3 = in Bits (24 bits)

    val biasEn = in Bool()
    val bias0 = in Bits (24 bits)
    val bias1 = in Bits (24 bits)


    val out0 = out Bits (48 bits)
    val out1 = out Bits (48 bits)
  }

  import DSP48E2ConfigMode._
  import DSP48E2ConfigABCD._
  import DSP48E2ConfigWXYZ._

  val latency = 2

  val down = new Area {

    val attr = new DSP48E2Attributes

    set_alu_attr(attr,simd = 2)
    inmode.set_static_inmode_attr(attr)
    opmode.set_dynamic_opmode_attr(attr)
    ab_concat.set_ab_concat_attr(attr)
    c.set_c_input_attr(attr)
    d.set_mute_d_attr(attr)

    val dsp = new DSP48E2(attr)

    inmode.assign_static_inmode_ctrl(dsp)
    inmode.assign_default(dsp)
    opmode.assign_dynamic_opmode_ctrl(dsp)
    w.w_sel_rnd(dsp, False)
    x.x_sel_ab(dsp, True)
    y.y_sel_c(dsp, False)
    z.z_sel_c(dsp, io.biasEn)
    ab_concat.assign_ab_concat_ctrl(dsp)
    c.assign_c_input_ctrl(dsp)
    d.assign_mute_d_ctrl(dsp)

    assign_m_ctrl(dsp)
    assign_p_ctrl(dsp, ce = True, rst = False)

    val bias = io.bias0 ## io.bias1
    val p0p1 = io.p0 ## io.p1

    dsp.DATAIN.A := p0p1.drop(18)
    dsp.DATAIN.B := p0p1.take(18)
    dsp.DATAIN.C := bias
  }

  val up = new Area{

    val attr = new DSP48E2Attributes

    set_alu_attr(attr,simd = 2)
    inmode.set_static_inmode_attr(attr)
    opmode.set_dynamic_opmode_attr(attr)
    ab_concat.set_ab_concat_attr(attr)
    c.set_c_input_attr(attr)
    d.set_mute_d_attr(attr)

    val dsp = new DSP48E2(attr)

    inmode.assign_static_inmode_ctrl(dsp)
    inmode.assign_default(dsp)
    opmode.assign_dynamic_opmode_ctrl(dsp)
    w.w_sel_rnd(dsp, False)
    x.x_sel_ab(dsp, io.inVld)
    y.y_sel_c(dsp, io.fbVld)
    z.z_sel_pcin(dsp, io.inVld)
    ab_concat.assign_ab_concat_ctrl(dsp)
    c.assign_c_input_ctrl(dsp)
    d.assign_mute_d_ctrl(dsp)

    assign_m_ctrl(dsp)
    assign_p_ctrl(dsp, ce = True, rst = False)

    val p2p3 = io.p2 ## io.p3
    dsp.DATAIN.A := p2p3.drop(18)
    dsp.DATAIN.B := p2p3.take(18)

    val delayReg0 = Bits(48 bits)
    val delayReg1 = Bits(48 bits)
    delayReg0.setAsReg().init(0)
    delayReg1.setAsReg().init(0)

    delayReg0 := dsp.DATAOUT.P
    delayReg1 := delayReg0
    dsp.DATAIN.C := delayReg1

    dsp.CASCDATAIN.P := down.dsp.CASCDATAOUT.P

    io.out0 := delayReg0
    io.out1 := delayReg1
  }
}
