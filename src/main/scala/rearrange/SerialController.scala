package rearrange

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

class SerialController[T <: Data](
                                   dataType: HardType[T], mem: Mem[T],
                                   inDimWidth: List[Int],
                                   outDimWidth: List[Int],
                                   outStrideWidth: List[Int]
                                 ) extends Component {

  require(outDimWidth.length == outStrideWidth.length)

  val io = new Bundle {
    val push = slave(Stream(dataType()))
    val pop = master(Stream(dataType()))
    val wrCmd = master(Flow(MemWriteCmd(mem)))
    val rdPort = master(MemReadPort(dataType(), mem.addressWidth))
  }

  val cfg = new Bundle {
    val inDims = in(Vec(inDimWidth.map(w => UInt(w bits))))
    val outDims = in(Vec(outDimWidth.map(w => UInt(w bits))))
    val outStrides = in(Vec(outStrideWidth.map(w => UInt(w bits))))
  }

  
}
