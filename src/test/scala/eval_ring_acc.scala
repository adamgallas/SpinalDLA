import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArithmetic.standalone.ring_acc

import scala.language.postfixOps
import scala.util.Random

object eval_ring_acc extends App {

  SpinalVerilog(new ring_acc)

  val pass = 4
  val sample = 32
  //  val a = Array.fill(pass)(Array.fill(sample)(Random.nextInt(256) - 128))
  //  val b = Array.fill(pass)(Array.fill(sample)(Random.nextInt(256) - 128))
  //  val c = Array.fill(pass)(Array.fill(sample)(Random.nextInt(256) - 128))
  //  val d = Array.fill(pass)(Array.fill(sample)(Random.nextInt(256) - 128))
  //  val ab = (a, b).zipped.map(_.sum + _.sum)
  //  val cd = (c, d).zipped.map(_.sum + _.sum)

  val p3 = Array.fill(pass)(Array.fill(sample)(Random.nextInt(256)))
  val sum = Array.fill(pass)(Array.fill(4)(0))

  for (p <- 0 until pass) {
    for (i <- 0 until sample) {
      sum(p)(i % 4) += p3(p)(i)
    }
  }

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new ring_acc)
    .doSim { dut =>
      import dut._

      io.inVld #= false
      io.fbVld #= false
      io.p0 #= 0
      io.p1 #= 0
      io.p2 #= 0
      io.p3 #= 0
      io.biasEn #= false
      io.bias0 #= 0
      io.bias1 #= 0

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      def check(p: Int) = {
        for (i <- 0 until sample + latency + 1) {
          if (i < sample) {
            io.p3 #= p3(p)(i) & 0xffffff
          }

          if (i == 0) io.inVld #= true
          if (i == 4) io.fbVld #= true
          if (i == sample) {
            io.inVld #= false
            io.fbVld #= false
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
