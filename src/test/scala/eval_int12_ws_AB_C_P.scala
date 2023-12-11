import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArithmetic.dualCascade.int12_ws_AB_C_P

import scala.language.postfixOps
import scala.util.Random

object eval_int12_ws_AB_C_P extends App {

  val pass = 8
  val reuse = 16
  val vecLength = 4
  val sampleLength = pass * reuse

  val a = Array.fill(pass)(Array.fill(vecLength)(Array.fill(4)(Random.nextInt(256) - 128)))
  val b = Array.fill(pass)(Array.fill(vecLength)(Array.fill(4)(Random.nextInt(256) - 128)))
  val aSel = Array.fill(pass)(Array.fill(reuse)(Array.fill(vecLength)(Random.nextInt(2))))
  val bSel = Array.fill(pass)(Array.fill(reuse)(Array.fill(vecLength)(Random.nextInt(2))))

  val ab = for (p <- 0 until pass) yield {
    for (r <- 0 until reuse) yield {
      for (s <- 0 until 4) yield {
        (for (v <- 0 until vecLength) yield {
          aSel(p)(r)(v) * a(p)(v)(s) + bSel(p)(r)(v) * b(p)(v)(s)
        }).sum
      }
    }
  }

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new int12_ws_AB_C_P(vecLength, 8))
    .doSimUntilVoid { dut =>
      import dut._

      io.a.foreach(_ #= 0)
      io.b.foreach(_ #= 0)
      io.aSel.foreach(_ #= 0)
      io.bSel.foreach(_ #= 0)
      io.enPrefetch #= false
      io.enFetch #= false
      io.clrPrefetch.foreach(_ #= true)

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      def preLoad(p: Int) = {
        io.enPrefetch #= false
        for (v <- 0 until vecLength) {
          for (s <- 0 until 4) {
            io.a(s) #= a(p)(vecLength - v - 1)(s) & 0xff
            io.b(s) #= b(p)(vecLength - v - 1)(s) & 0xff
          }
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
              io.aSel(j) #= aSel(p)(i - j)(j)
              io.bSel(j) #= bSel(p)(i - j)(j)
            }
          }
          if (i > latency) {
            val index = i - latency - 1

            for (s <- 0 until 4) {
              assert(((io.ab(s).toInt << 20) >> 20) == ab(p)(index)(s))
            }
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
