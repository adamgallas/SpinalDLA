import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArithmetic.dualCascade.int16_os_B_P

import scala.language.postfixOps
import scala.util.Random

object eval_int16_os_offload extends App {

  SpinalVerilog(new int16_os_B_P(4))

  val accLength = 16
  val vecLength = 4
  val a = Array.fill(accLength)(Random.nextInt(256) - 128)
  val b = Array.fill(vecLength)(Array.fill(accLength)(Random.nextInt(256) - 128))
  val ab = for (v <- 0 until vecLength) yield {
    (a, b(v)).zipped.map(_ * _).sum
  }

  println(a.mkString(", "))
  println(ab.mkString(", "))

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new int16_os_B_P(vecLength))
    .doSimUntilVoid { dut =>
      import dut._

      io.a #= 0
      io.b.foreach(_ #= 0)
      io.accValid #= false
      io.accLast #= false

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      def check() = {
        for (i <- 0 until accLength + latency + vecLength) {
          if (i < accLength) {
            io.a #= a(i) & 0xffff
            io.accValid #= true
          }
          if (i == accLength - 1) io.accLast #= true
          if (i == accLength) {
            io.accValid #= false
            io.accLast #= false
          }
          for (j <- 0 until vecLength) {
            if (j <= i && i < accLength + j)
              io.b(j) #= b(j)(i - j) & 0xffff
            else io.b(j) #= 0
          }
          if (i >= accLength + latency) {
            assert(io.ab.toBigInt.toInt == ab.reverse(i - accLength - latency))
          }
          clockDomain.waitSampling(1)
        }
      }

      for (p <- 0 until 4) {
        fork {
          check()
        }
        clockDomain.waitSampling(accLength + vecLength)
      }

      clockDomain.waitSampling(128)
      simSuccess()
    }
}
