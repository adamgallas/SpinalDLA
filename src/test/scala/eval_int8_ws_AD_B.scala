import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArithmetic.dualCascade.int8_ws_AD_B

import scala.language.postfixOps
import scala.util.Random

object eval_int8_ws_AD_B extends App {

  SpinalVerilog(new int8_ws_AD_B(8))
}
