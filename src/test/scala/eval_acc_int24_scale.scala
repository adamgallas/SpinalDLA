import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArith.standalone.acc_int24_scale

import scala.language.postfixOps
import scala.util.Random

object eval_acc_int24_scale extends App {

  SpinalVerilog(new acc_int24_scale)

  val pass = 4
  val sample = 32
  val scale = 9
  val a = Array.fill(pass)(Array.fill(sample)(Random.nextInt(256) - 128))
  val b = Array.fill(pass)(Array.fill(sample)(Random.nextInt(256) - 128))
  val aAcc = a.map(_.sum)
  val bAcc = b.map(_.sum)
  val aAccScale = aAcc.map(_ * scale)
  val bAccScale = bAcc.map(_ * scale)

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new acc_int24_scale)
    .doSim { dut =>
      import dut._

      io.a #= 0
      io.b #= 0
      io.scale #= scale
      io.valid #= false
      io.last #= false

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      def check(p: Int) = {
        for (i <- 0 until sample + accLatency + 1) {
          if (i < sample) {
            io.a #= a(p)(i) & 0xffffff
            io.b #= b(p)(i) & 0xffffff
          }
          if (i == sample + accLatency) {
            println(aAcc(p), (io.aAcc.toInt << 8) >> 8)
            println(bAcc(p), (io.bAcc.toInt << 8) >> 8)
          }

          if (i == 0) io.valid #= true
          if (i == sample - 1) io.last #= true
          if (i == sample) {
            io.valid #= false
            io.last #= false
          }

          clockDomain.waitSampling(1)
        }
      }

      for (p <- 0 until pass) {
        fork {
          check(p)
        }
        clockDomain.waitSampling(sample + 5)
      }

      clockDomain.waitSampling(1024)
      simSuccess()
    }
}
