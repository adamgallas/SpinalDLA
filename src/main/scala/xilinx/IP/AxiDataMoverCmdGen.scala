package xilinx.IP

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

object AxiDataMoverCmdGen {
  def apply(stream: Stream[util.PairBundle[UInt, UInt]], baseAddr: UInt, inc: Bool = True, eof: Bool = False) = {
    val cmd = Stream(Bits(32 + 8 + baseAddr.getWidth bits))
    cmd.arbitrationFrom(stream)
    cmd.payload := B"00000000" ##
      (stream.A + baseAddr).resize(baseAddr.getWidth) ##
      B"0" ##
      eof ##
      B"000000" ##
      inc ##
      stream.B.resize(23)
    cmd
  }
}
