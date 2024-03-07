package example

import spinal.core._
import spinal.lib._
import xilinx.DSP48E2._
import systolic._
import xilinx.DSP48E2IntArithmetic.cascade.int8_dotp_ddr
import xilinx.DSP48E2IntArithmetic.standalone.ring_acc

import scala.language.postfixOps

class enhanced_b1024_sa(
                          clkx2: ClockDomain,
                          length: Int,
                          width: Int,
                          height: Int
                        ) extends Component {

  clockDomain.setName("fast")

  val pe = for (i <- 0 until height) yield {
    for (j <- 0 until width) yield {
      new enhanced_b1024_pe(
        clkx2 = clkx2,
        length = length,
        isLastPEVertical = i == height - 1,
        isLastPEHorizontal = j == width - 1
      )
    }
  }

  val inp = for (i <- 0 until height) yield {
    for (j <- 0 until width) yield {
      pe(i)(j).inp.toIo()
    }
  }

  val out = for (i <- 0 until height) yield {
    for (j <- 0 until width) yield {
      pe(i)(j).out.toIo()
    }
  }

  val srcH = for (i <- 0 until height) yield {
    pe(i)(0).srcH.toIo()
  }

  val srcV = for (j <- 0 until width) yield {
    pe(0)(j).srcV.toIo()
  }


  for (i <- 0 until height) {
    for (j <- 1 until width) {
      pe(i)(j).srcH := pe(i)(j - 1).dstH
    }
  }

  for (j <- 0 until width) {
    for (i <- 1 until height) {
      pe(i)(j).srcV := pe(i - 1)(j).dstV
    }
  }
}
