package systolic

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

class GlobalSignalTree[T <: Data](dataType: HardType[T], fanout: Int, leaf: Int) extends Component {
  val io = new Bundle {
    val input = in(dataType())
    val outputs = out(Vec(dataType(), leaf))
  }

}
