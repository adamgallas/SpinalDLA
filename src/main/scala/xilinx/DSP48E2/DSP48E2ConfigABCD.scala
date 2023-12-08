package xilinx.DSP48E2

import spinal.core._
import scala.language.postfixOps

object DSP48E2ConfigABCD {

  object inmode {
    def set_static_inmode_attr(attr: DSP48E2Attributes) = attr.INMODEREG = 0

    def set_dynamic_inmode_attr(attr: DSP48E2Attributes) = attr.INMODEREG = 1

    def assign_static_inmode_ctrl(dsp: DSP48E2): Unit = {
      dsp.CEs.INMODE.clear()
      dsp.RSTs.INMODE.clear()
    }

    def assign_dynamic_inmode_ctrl(dsp: DSP48E2): Unit = {
      dsp.CEs.INMODE.set()
      dsp.RSTs.INMODE.clear()
    }

    def assign_inmode_a(dsp: DSP48E2, high4a1: Bool) = dsp.INST.INMODE(0) := high4a1

    def assign_inmode_b(dsp: DSP48E2, high4b1: Bool) = dsp.INST.INMODE(4) := high4b1

    def assign_inmode_gate_ab(dsp: DSP48E2, high2gate: Bool) = dsp.INST.INMODE(1) := high2gate

    def assign_inmode_gate_d(dsp: DSP48E2, low2gate: Bool) = dsp.INST.INMODE(2) := low2gate

    def assign_inmode_add_sub(dsp: DSP48E2, high2sub: Bool) = dsp.INST.INMODE(3) := high2sub

    def assign_default(dsp: DSP48E2) = {
      assign_inmode_gate_ab(dsp, high2gate = False)
      assign_inmode_gate_d(dsp, low2gate = True)
      assign_inmode_add_sub(dsp, high2sub = False)
    }
  }

  object a {
    def set_mute_a_attr(attr: DSP48E2Attributes) = attr.AREG = 2

    def assign_mute_a_ctrl(dsp: DSP48E2): Unit = {
      dsp.CEs.A1.clearAll()
      dsp.CEs.A2.clearAll()
      dsp.RSTs.A.clearAll()
    }

    def set_a_cascade(attr: DSP48E2Attributes) = attr.A_INPUT = "CASCADE"

    def set_a_pingpong(attr: DSP48E2Attributes) = attr.AREG = 2

    def assign_a_pingpong_ctrl(dsp: DSP48E2, ce1: Bool, ce2: Bool): Unit = {
      inmode.assign_inmode_a(dsp, high4a1 = False)
      dsp.CEs.A1 := ce1
      dsp.CEs.A2 := ce2
      dsp.RSTs.A.clearAll()
    }

    def set_static_a_input_attr(attr: DSP48E2Attributes, pipe: Int) = attr.AREG = pipe

    def assign_static_a_input_ctrl(dsp: DSP48E2, pipe: Int) = {
      dsp.RSTs.A.clear()
      if (pipe == 2) {
        dsp.CEs.A1.set()
        dsp.CEs.A2.set()
        inmode.assign_inmode_a(dsp, high4a1 = False)
      }
      else {
        dsp.CEs.A1.set()
        inmode.assign_inmode_a(dsp, high4a1 = True)
      }
    }
  }

  object b {
    def set_mute_b_attr(attr: DSP48E2Attributes) = attr.BREG = 2

    def assign_mute_b_ctrl(dsp: DSP48E2): Unit = {
      dsp.CEs.B1.clearAll()
      dsp.CEs.B2.clearAll()
      dsp.RSTs.B.clearAll()
    }

    def set_b_cascade(attr: DSP48E2Attributes) = attr.B_INPUT = "CASCADE"

    def set_b_pingpong(attr: DSP48E2Attributes) = attr.BREG = 2

    def assign_b_pingpong_ctrl(dsp: DSP48E2, ce1: Bool, ce2: Bool): Unit = {
      inmode.assign_inmode_b(dsp, high4b1 = False)
      dsp.CEs.B1 := ce1
      dsp.CEs.B2 := ce2
      dsp.RSTs.B.clearAll()
    }

    def set_static_b_input_attr(attr: DSP48E2Attributes, pipe: Int) = attr.BREG = pipe

    def assign_static_b_input_ctrl(dsp: DSP48E2, pipe: Int) = {
      dsp.RSTs.B.clear()
      if (pipe == 2) {
        dsp.CEs.B1.set()
        dsp.CEs.B2.set()
        inmode.assign_inmode_b(dsp, high4b1 = False)
      }
      else {
        dsp.CEs.B1.set()
        inmode.assign_inmode_b(dsp, high4b1 = True)
      }
    }
  }

  object c {
    def set_mute_c_attr(attr: DSP48E2Attributes) = attr.CREG = 1

    def assign_mute_c_ctrl(dsp: DSP48E2): Unit = {
      dsp.CEs.C.clearAll()
      dsp.RSTs.C.clearAll()
    }

    def set_c_input_attr(attr: DSP48E2Attributes) = attr.CREG = 1

    def assign_c_input_ctrl(dsp: DSP48E2, ce: Bool = True): Unit = {
      dsp.CEs.C := ce
      dsp.RSTs.C.clear()
    }
  }

  object d {
    def set_mute_d_attr(attr: DSP48E2Attributes) = {
      attr.ADREG = 1
      attr.DREG = 1
    }

    def assign_mute_d_ctrl(dsp: DSP48E2): Unit = {
      dsp.CEs.D.clearAll()
      dsp.CEs.AD.clear()
      dsp.RSTs.D.clearAll()
    }
  }

  object ab_concat {
    def set_static_ab_concat_attr(attr: DSP48E2Attributes) = {
      attr.AREG = 1
      attr.BREG = 1
    }

    def assign_static_ab_concat_ctrl(dsp: DSP48E2): Unit = {
      inmode.assign_inmode_a(dsp, high4a1 = False)
      inmode.assign_inmode_b(dsp, high4b1 = False)
      dsp.CEs.A2.set()
      dsp.CEs.B2.set()
      dsp.CEs.A1.clear()
      dsp.CEs.B1.clear()
      dsp.RSTs.A.clear()
      dsp.RSTs.B.clear()
    }

    def set_pingpong_ab_concat_attr(attr: DSP48E2Attributes) = {
      a.set_a_pingpong(attr)
      b.set_b_pingpong(attr)
    }

    def assign_pingpong_ab_concat_ctrl(dsp: DSP48E2, ce1: Bool, ce2: Bool): Unit = {
      a.assign_a_pingpong_ctrl(dsp, ce1, ce2)
      b.assign_b_pingpong_ctrl(dsp, ce1, ce2)
    }
  }

  object ad_pack {
    def set_static_ad_pack_attr(attr: DSP48E2Attributes) = {
      attr.AREG = 1
      attr.DREG = 1
      attr.ADREG = 1
      attr.AMULTSEL = "AD"
      attr.PREADDINSEL = "A"
    }

    def set_static_b_attr(attr: DSP48E2Attributes) = {
      b.set_static_b_input_attr(attr, 2)
    }

    def set_pingpong_b_attr(attr: DSP48E2Attributes) = {
      b.set_b_pingpong(attr)
    }

    def assign_static_ad_pack_ctrl(dsp: DSP48E2): Unit = {
      inmode.assign_inmode_a(dsp, high4a1 = True)
      dsp.CEs.A1.set()
      dsp.CEs.D.set()
      dsp.CEs.AD.set()
      dsp.RSTs.A.clear()
      dsp.RSTs.D.clear()
    }

    def assign_static_b_ctrl(dsp: DSP48E2): Unit = {
      b.assign_static_b_input_ctrl(dsp, 2)
    }

    def assign_pingpong_b_ctrl(dsp: DSP48E2, ce1: Bool, ce2: Bool): Unit = {
      b.assign_b_pingpong_ctrl(dsp, ce1, ce2)
    }
  }
}
