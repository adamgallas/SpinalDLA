package example

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._
import systolic._
import xilinx.DSP48E2IntArithmetic.cascade.int8_dotp_ddr
import xilinx.DSP48E2IntArithmetic.standalone.ring_acc

import scala.language.postfixOps

class enhanced_b1024_pe(
                         clkx2: ClockDomain,
                         length: Int,
                         isLastPEVertical: Boolean = false,
                         isLastPEHorizontal: Boolean = false
                       ) extends Component {

  case class inpBdl() extends Bundle {
    val inVld = Bool()
    val fbVld = Bool()
    val biasEn = Bool()
    val bias0 = Bits(24 bits)
    val bias1 = Bits(24 bits)
  }

  case class outBdl() extends Bundle {
    val out0 = Bits(48 bits)
    val out1 = Bits(48 bits)
  }

  case class vertBdl() extends Bundle {
    val a0 = Vec(Bits(8 bits), length)
    val a1 = Vec(Bits(8 bits), length)
  }

  case class horizBdl() extends Bundle {
    val b0 = Vec(Bits(8 bits), length)
    val b1 = Vec(Bits(8 bits), length)
    val c0 = Vec(Bits(8 bits), length)
    val c1 = Vec(Bits(8 bits), length)
  }

  val inp = new inpBdl().asInput()
  val srcV = new vertBdl().asInput()
  val srcH = new horizBdl().asInput()
  val out = new outBdl().asOutput()

  val dstV = if (!isLastPEVertical) new vertBdl().asOutput() else null
  val dstH = if (!isLastPEHorizontal) new horizBdl().asOutput() else null

  val srcVDly = RegNext(srcV)
  val srcHDly = RegNext(srcH)

  if (!isLastPEVertical) dstV := srcVDly
  if (!isLastPEHorizontal) dstH := srcHDly

  srcVDly addTag crossClockDomain
  srcHDly addTag crossClockDomain

  val x2 = new ClockingArea(clkx2) {
    val chain0 = new int8_dotp_ddr(length)
    val chain1 = new int8_dotp_ddr(length)

    val acc = new ring_acc()

    chain0.io.a := srcVDly.a0
    chain0.io.b := srcHDly.b0
    chain0.io.c := srcHDly.c0

    chain1.io.a := srcVDly.a1
    chain1.io.b := srcHDly.b1
    chain1.io.c := srcHDly.c1

    acc.io.inVld := inp.inVld
    acc.io.fbVld := inp.fbVld
    acc.io.biasEn := inp.biasEn
    acc.io.bias0 := inp.bias0
    acc.io.bias1 := inp.bias1

    acc.io.p0 := chain0.io.ab.asSInt.resize(24 bits).asBits
    acc.io.p1 := chain0.io.ac.asSInt.resize(24 bits).asBits
    acc.io.p2 := chain1.io.ab.asSInt.resize(24 bits).asBits
    acc.io.p3 := chain1.io.ac.asSInt.resize(24 bits).asBits

    out.out0 := acc.io.out0
    out.out1 := acc.io.out1
  }
}
