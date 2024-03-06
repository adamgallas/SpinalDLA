package example

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._
import systolic._
import xilinx.DSP48E2IntArithmetic.cascade.int8_dotp
import xilinx.DSP48E2IntArithmetic.standalone.dpuczdx8g_acc

import scala.language.postfixOps

class b1024_pe(
                clkx1: ClockDomain,
                length: Int,
                isLastPEVertical: Boolean = false,
                isLastPEHorizontal: Boolean = false
              ) extends Component {

  case class inpBdl() extends Bundle {
    val inVld = Bool()
    val accVld = Bool()
    val biasRst = Bool()
    val bias0 = Bits(26 bits)
    val bias1 = Bits(26 bits)
    val bias2 = Bits(26 bits)
    val bias3 = Bits(26 bits)
  }

  case class outBdl() extends Bundle {
    val out0 = Bits(29 bits)
    val out1 = Bits(29 bits)
    val out2 = Bits(29 bits)
    val out3 = Bits(29 bits)
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

  val chain0 = new int8_dotp(length)
  val chain1 = new int8_dotp(length)

  chain0.io.a := srcVDly.a0
  chain0.io.b := srcHDly.b0
  chain0.io.c := srcHDly.c0

  chain1.io.a := srcVDly.a1
  chain1.io.b := srcHDly.b1
  chain1.io.c := srcHDly.c1

  val ab0 = RegNext(chain0.io.ab) addTag crossClockDomain
  val ab1 = RegNext(chain1.io.ab) addTag crossClockDomain
  val ab0Dly = RegNext(RegNext(chain0.io.ab)) addTag crossClockDomain
  val ab1Dly = RegNext(RegNext(chain1.io.ab)) addTag crossClockDomain

  val ac0 = RegNext(chain0.io.ac) addTag crossClockDomain
  val ac1 = RegNext(chain1.io.ac) addTag crossClockDomain
  val ac0Dly = RegNext(RegNext(chain0.io.ac)) addTag crossClockDomain
  val ac1Dly = RegNext(RegNext(chain1.io.ac)) addTag crossClockDomain

  val x1 = new ClockingArea(clkx1) {
    val acc0 = new dpuczdx8g_acc()
    acc0.io.inVld := inp.inVld
    acc0.io.accVld := inp.accVld
    acc0.io.biasRst := inp.biasRst
    acc0.io.bias0 := inp.bias0
    acc0.io.bias1 := inp.bias1
    out.out0 := acc0.io.out0
    out.out1 := acc0.io.out1
    acc0.io.p0 := ab0
    acc0.io.p1 := ab1
    acc0.io.p2 := ab0Dly
    acc0.io.p3 := ab1Dly

    val acc1 = new dpuczdx8g_acc()
    acc1.io.inVld := inp.inVld
    acc1.io.accVld := inp.accVld
    acc1.io.biasRst := inp.biasRst
    acc1.io.bias0 := inp.bias2
    acc1.io.bias1 := inp.bias3
    out.out2 := acc1.io.out0
    out.out3 := acc1.io.out1
    acc1.io.p0 := ac0
    acc1.io.p1 := ac1
    acc1.io.p2 := ac0Dly
    acc1.io.p3 := ac1Dly
  }
}

object pe extends App {
  SpinalVerilog(new b1024_pe(ClockDomain.external("clkx1"), 4))
  SpinalVerilog(new enhanced_b1024_pe(ClockDomain.external("clkx2"), 4))
}
