package example

import spinal.core._

import scala.language.postfixOps

class tpu14_sa() extends Component {

  val length = 14

  val pe = for (i <- 0 until length) yield {
    new tpu14_pe(isLastPEHorizontal = i == length - 1)
  }

  val inp = pe.map(_.inp.toIo())
  val out = pe.map(_.out.toIo())
  val srcH = pe(0).srcH.toIo()

  for (i <- 1 until length) {
    pe(i).srcH := pe(i - 1).dstH
  }
}

object tpu extends App {
  SpinalVerilog(new tpu14_sa())
}