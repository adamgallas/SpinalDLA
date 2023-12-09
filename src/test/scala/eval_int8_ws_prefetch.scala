import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArithmetic.dualCascade.int8_ws_B_P

import scala.language.postfixOps
import scala.util.Random

object eval_int8_ws_prefetch extends App {

  val pass = 8
  val reuse = 16
  val vecLength = 8
  val sampleLength = pass * reuse

  val a = Array.fill(pass)(Array.fill(vecLength)(Random.nextInt(256) - 128))
  val b = Array.fill(pass)(Array.fill(reuse)(Array.fill(vecLength)(Random.nextInt(256) - 128)))
  val c = Array.fill(pass)(Array.fill(reuse)(Array.fill(vecLength)(Random.nextInt(256) - 128)))


  val ab = for (p <- 0 until pass) yield {
    for (r <- 0 until reuse) yield {
      (a(p), b(p)(r)).zipped.map(_ * _).sum
    }
  }

  val ac = for (p <- 0 until pass) yield {
    for (r <- 0 until reuse) yield {
      (a(p), c(p)(r)).zipped.map(_ * _).sum
    }
  }

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new int8_ws_B_P(vecLength))
    .doSimUntilVoid { dut =>
      import dut._

      io.a #= 0
      io.b.foreach(_ #= 0)
      io.c.foreach(_ #= 0)
      io.enPrefetch #= false
      io.enFetch #= false
      io.clrPrefetch.foreach(_ #= true)

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      def preLoad(p: Int) = {
        io.enPrefetch #= false
        for (v <- 0 until vecLength) {
          io.a #= a(p)(vecLength - v - 1) & 0xff
          io.enPrefetch #= true
          if (v == vecLength - 1) io.clrPrefetch.foreach(_ #= true)
          else io.clrPrefetch.foreach(_ #= false)
          clockDomain.waitSampling()
        }
        io.enPrefetch #= false
        io.clrPrefetch.foreach(_ #= false)
      }

      def load() = {
        io.enFetch #= true
        clockDomain.waitSampling()
        io.enFetch #= false
      }

      def procedure(p: Int) = {

        for (i <- 0 until reuse + latency) {
          for (j <- 0 until vecLength) {
            if (j <= i && i < reuse + j) {
              io.b(j) #= b(p)(i - j)(j) & 0xff
              io.c(j) #= c(p)(i - j)(j) & 0xff
            }
          }
          if (i > latency) {
            val index = i - latency - 1
            assert((io.ab.toInt << 14) >> 14 == (ab(p)(index)))
            assert((io.ac.toInt << 14) >> 14 == (ac(p)(index)))
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
