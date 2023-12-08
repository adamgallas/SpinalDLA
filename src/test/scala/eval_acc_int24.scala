import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArithmetic.standalone.acc_int24

import scala.language.postfixOps
import scala.util.Random

object eval_acc_int24 extends App {

  SpinalVerilog(new acc_int24)

  val pass = 4
  val sample = 32
  val a = Array.fill(pass)(Array.fill(sample)(Random.nextInt(256) - 128))
  val b = Array.fill(pass)(Array.fill(sample)(Random.nextInt(256) - 128))
  val c = Array.fill(pass)(Array.fill(sample)(Random.nextInt(256) - 128))
  val d = Array.fill(pass)(Array.fill(sample)(Random.nextInt(256) - 128))
  val ab = (a, b).zipped.map(_.sum + _.sum)
  val cd = (c, d).zipped.map(_.sum + _.sum)

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new acc_int24)
    .doSim { dut =>
      import dut._

      io.a #= 0
      io.b #= 0
      io.c #= 0
      io.d #= 0
      io.valid #= false
      io.last #= false

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      def check(p: Int) = {
        for (i <- 0 until sample + latency + 1) {
          if (i < sample) {
            io.a #= a(p)(i) & 0xffffff
            io.b #= b(p)(i) & 0xffffff
            io.c #= c(p)(i) & 0xffffff
            io.d #= d(p)(i) & 0xffffff
          }
          if (i == sample + latency) {
            assert(ab(p) == (io.ab.toInt << 8) >> 8)
            assert(cd(p) == (io.cd.toInt << 8) >> 8)
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
        clockDomain.waitSampling(sample)
      }

      clockDomain.waitSampling(1024)
      simSuccess()
    }
}
