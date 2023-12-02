package xilinx.DSP48E2IntArith.dualCascade

import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArith.dualCascade.int16_ws_prefetch

import scala.language.postfixOps
import scala.util.Random

class four12_ws_prefetch(length: Int, width: Int) extends Component {

  val io = new Bundle {
    val aSel = in Vec(Bits(1 bits), length)
    val bSel = in Vec(Bits(1 bits), length)
    val a = in Vec(Bits(width bits), 4)
    val b = in Vec(Bits(width bits), 4)
    val ab = out Vec(Bits(12 bits), 4)

    val CE1 = in Bool()
    val CE2 = in Bool()
    // rstCE1 should be the same for all CE1 registers,
    // this duplication is for fanout reduction outside this component
    val rstCE1 = in Vec(Bool(), length)
  }

  require(width <= 12)
  val latency = length + 2 - 1

  val inModes = Vec(Bits(5 bits), length)
  val opModes = Vec(Bits(9 bits), length)

  val aBits = io.a.map(_.asSInt.resize(12 bits)).asBits()
  val bBits = io.b.map(_.asSInt.resize(12 bits)).asBits()
  val ce1Chain = Vec(Bool(), length)
  val ce2Chain = Vec(Bool(), length)
  val CPortChain = Vec(Bits(48 bits), length)

  val dsp48e2s = for (i <- 0 until length) yield {
    val build = DSP48E2AttrBuild()
    build.setAsALU(4)
    build.setStaticALUMODE()
    if(i== 0) {
      build.attrs.A_INPUT = "DIRECT"
      build.attrs.B_INPUT = "DIRECT"
    }
    else {
      build.attrs.A_INPUT = "CASCADE"
      build.attrs.B_INPUT = "CASCADE"
    }

    inModes(i) := build.setStaticINMODE((2, 2, 1, 0, 0), "-")
    opModes(i) := build.setDynamicOPMode_C_P_PCIN_AB((
      io.bSel(i).msb,
      False,
      if (i == 0) False else True,
      io.aSel(i).msb
    ))
    val attrs = build.attrs
    new DSP48E2(attrs)
  }

  for (i <- 0 until length) {
    CPortChain(i).setAsReg()
    if (i == 0) {
      dsp48e2s(i).CASCDATAIN.A.clearAll()
      dsp48e2s(i).CASCDATAIN.B.clearAll()
      dsp48e2s(i).CASCDATAIN.P.clearAll()
      dsp48e2s(i).DATAIN.A := aBits.drop(18)
      dsp48e2s(i).DATAIN.B := aBits.take(18)
      ce1Chain(i) := io.CE1
      ce2Chain(i) := io.CE2
      when(ce1Chain(i)) {
        CPortChain(i) := bBits
      }
    }
    else {
      dsp48e2s(i).CASCDATAIN.A := dsp48e2s(i - 1).CASCDATAOUT.A
      dsp48e2s(i).CASCDATAIN.B := dsp48e2s(i - 1).CASCDATAOUT.B
      dsp48e2s(i).CASCDATAIN.P := dsp48e2s(i - 1).CASCDATAOUT.P
      dsp48e2s(i).DATAIN.A.clearAll()
      dsp48e2s(i).DATAIN.B.clearAll()

      ce1Chain(i).setAsReg()
      ce2Chain(i).setAsReg().init(false)

      ce1Chain(i) := ce1Chain(i - 1)
      ce2Chain(i) := ce2Chain(i - 1)
      ce1Chain(i).clearWhen(io.rstCE1(i))
      when(ce1Chain(i)) {
        CPortChain(i) := CPortChain(i - 1)
      }
    }

    dsp48e2s(i).DATAIN.D.clearAll()
    dsp48e2s(i).DATAIN.C := CPortChain(i)
    dsp48e2s(i).DATAIN.CARRYIN.clearAll()

    dsp48e2s(i).CASCDATAIN.CARRYCAS.clearAll()
    dsp48e2s(i).CASCDATAIN.MULTSIGN.clearAll()

    dsp48e2s(i).INST.ALUMODE.clearAll()
    dsp48e2s(i).INST.INMODE := inModes(i)
    dsp48e2s(i).INST.OPMODE := opModes(i)

    dsp48e2s(i).CEs.A1 := ce1Chain(i)
    dsp48e2s(i).CEs.A2 := ce2Chain(i)
    dsp48e2s(i).CEs.B1 := ce1Chain(i)
    dsp48e2s(i).CEs.B2 := ce2Chain(i)
    dsp48e2s(i).CEs.C := ce2Chain(i)
    dsp48e2s(i).CEs.P.set()
    dsp48e2s(i).CEs.CTRL.set()

    dsp48e2s(i).CEs.all.foreach(
      ce => if (!ce.hasAssignement) ce.clearAll()
    )
    dsp48e2s(i).RSTs.all.foreach(
      rst => if (!rst.hasAssignement) rst.clearAll()
    )
  }

  io.ab.assignFromBits(dsp48e2s.last.DATAOUT.P)
}
