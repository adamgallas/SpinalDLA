import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArith.dualCascade.int16_ws_prefetch

import scala.language.postfixOps
import scala.util.Random

object eval_int16_ws_prefetch extends App {

  val pass = 8
  val reuse = 8
  val vecLength = 8
  val sampleLength = pass * reuse

  val a = Array.fill(pass)(Array.fill(vecLength)(Random.nextInt(256) - 128))
  val b = Array.fill(pass)(Array.fill(reuse)(Array.fill(vecLength)(Random.nextInt(256) - 128)))

  val ab = for (p <- 0 until pass) yield {
    for (r <- 0 until reuse) yield {
      (a(p), b(p)(r)).zipped.map(_ * _).sum
    }
  }

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new int16_ws_prefetch(vecLength))
    .doSimUntilVoid { dut =>
      import dut._

      io.a #= 0
      io.b.foreach(_ #= 0)
      io.CE1 #= false
      io.CE2 #= false
      io.rstCE1.foreach(_ #= true)

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      def preLoad(p: Int) = {
        io.CE1 #= false
        for (v <- 0 until vecLength) {
          io.a #= a(p)(vecLength - v - 1) & 0xffff
          io.CE1 #= true
          if (v == vecLength - 1) io.rstCE1.foreach(_ #= true)
          else io.rstCE1.foreach(_ #= false)
          clockDomain.waitSampling()
        }
        io.CE1 #= false
        io.rstCE1.foreach(_ #= false)
      }

      def load() = {
        io.CE2 #= true
        clockDomain.waitSampling()
        io.CE2 #= false
      }

      def procedure(p: Int) = {

        for (i <- 0 until reuse + latency) {
          for (j <- 0 until vecLength) {
            if (j <= i && i < reuse + j) {
              io.b(j) #= b(p)(i - j)(j) & 0xffff
            }
          }
          if (i > latency) {
            val index = i - latency - 1
            assert(io.ab.toBigInt.toInt == ab(p)(index))
          }
          clockDomain.waitSampling(1)
        }
      }

      preLoad(0)

      for (pp <- 0 until pass) {
        fork {
          load()
        }
        fork {
          procedure(pp)
        }
        if (pp < pass - 1) {
          fork {
            preLoad(pp + 1)
          }
        }
        clockDomain.waitSampling(reuse)
      }
      clockDomain.waitSampling(128)
      simSuccess()

    }
}
