package xilinx.DSP48E2

import spinal.core._
import scala.language.postfixOps

object DSP48E2ConfigWXYZ {

  object opmode {
    def set_static_opmode_attr(attr: DSP48E2Attributes) = attr.OPMODEREG = 0

    def set_dynamic_opmode_attr(attr: DSP48E2Attributes) = attr.OPMODEREG = 1

    def assign_static_opmode_ctrl(dsp: DSP48E2): Unit = {
      dsp.CEs.CTRL.clear()
      dsp.RSTs.CTRL.clear()
    }

    def assign_dynamic_opmode_ctrl(dsp: DSP48E2): Unit = {
      dsp.CEs.CTRL.set()
      dsp.RSTs.CTRL.clear()
    }
  }

  object w {
    def assign_w_ctrl(dsp: DSP48E2, wSel: Bits) = dsp.INST.OPMODE(8 downto 7) := wSel

    def w_sel_p(dsp: DSP48E2, high4P: Bool) = assign_w_ctrl(dsp, B"0" ## high4P)

    def w_sel_rnd(dsp: DSP48E2, high4rnd: Bool) = assign_w_ctrl(dsp, high4rnd ## B"0")

    def w_sel_c(dsp: DSP48E2, high4c: Bool) = assign_w_ctrl(dsp, high4c ## high4c)
  }

  object x {
    def assign_x_ctrl(dsp: DSP48E2, xSel: Bits) = dsp.INST.OPMODE(1 downto 0) := xSel

    def x_sel_m(dsp: DSP48E2, high4m: Bool) = assign_x_ctrl(dsp, B"0" ## high4m)

    def x_sel_p(dsp: DSP48E2, high4p: Bool) = assign_x_ctrl(dsp, high4p ## B"0")

    def x_sel_ab(dsp: DSP48E2, high4ab: Bool) = assign_x_ctrl(dsp, high4ab ## high4ab)

  }

  object y {
    def assign_y_ctrl(dsp: DSP48E2, ySel: Bits) = dsp.INST.OPMODE(3 downto 2) := ySel

    def y_sel_m(dsp: DSP48E2, high4m: Bool) = assign_y_ctrl(dsp, B"0" ## high4m)

    def y_sel_ff(dsp: DSP48E2, high4ff: Bool) = assign_y_ctrl(dsp, high4ff ## B"0")

    def y_sel_c(dsp: DSP48E2, high4c: Bool) = assign_y_ctrl(dsp, high4c ## high4c)

    def y_sel_c_over_m(dsp: DSP48E2, high4c: Bool) = assign_y_ctrl(dsp, high4c ## B"1")
  }

  object z {
    def assign_z_ctrl(dsp: DSP48E2, zSel: Bits) = dsp.INST.OPMODE(6 downto 4) := zSel

    def z_sel_pcin(dsp: DSP48E2, high4pcin: Bool) = assign_z_ctrl(dsp, B"00" ## high4pcin)

    def z_sel_p(dsp: DSP48E2, high4p: Bool) = assign_z_ctrl(dsp, B"0" ## high4p ## B"0")

    def z_sel_c(dsp: DSP48E2, high4c: Bool) = assign_z_ctrl(dsp, B"0" ## high4c ## high4c)
  }
}
