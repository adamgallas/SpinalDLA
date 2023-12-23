import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArithmetic.standalone._
import xilinx.DSP48E2IntArithmetic.cascade._
import xilinx.DSP48E2IntArithmetic.dualCascade._

import scala.language.postfixOps

object verilog_gen extends App {

  val cfg = SpinalConfig(
    mode = Verilog,
    anonymSignalPrefix = "tmp",
    nameWhenByFile = true,
    targetDirectory = "verilog/xilinx/DSP48E2Arithmetic"
  )

  // standalone

  cfg.generate(new int8_mul)
  cfg.generate(new int12_xadd)
  cfg.generate(new int16_mul)
  cfg.generate(new int24_acc)
  cfg.generate(new int24_acc_scale)
  cfg.generate(new uint4_mul)

  // cascade

  cfg.generate(new int8_dotp(8))
  cfg.generate(new int16_dotp(8))
  cfg.generate(new int16_dotp_ddr(8))
  cfg.generate(new int12_xdotp(8))

  // dualCascade

  cfg.generate(new int8_ws_AD_B(8))
  cfg.generate(new int8_ws_B_P(8))
  cfg.generate(new int12_ws_AB_C_P(8, 8))
  cfg.generate(new int16_os_B_P(8))
  cfg.generate(new int16_ws_B_P(8))
}