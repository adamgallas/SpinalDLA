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

  val pass = 8
  val reuse = 16
  val vecLength = 8

  val a = Array.fill(pass)(Array.fill(vecLength)(Random.nextInt(256) - 128))
  val b = Array.fill(pass)(Array.fill(reuse)(Random.nextInt(256) - 128))
  val c = Array.fill(pass)(Array.fill(reuse)(Random.nextInt(256) - 128))
  val abIn = Array.fill(pass)(Array.fill(reuse)(Array.fill(vecLength)(Random.nextInt(256) - 128)))
  val acIn = Array.fill(pass)(Array.fill(reuse)(Array.fill(vecLength)(Random.nextInt(256) - 128)))

  val abOut = for (p <- 0 until pass) yield {
    for (r <- 0 until reuse) yield {
      for (v <- 0 until vecLength) yield {
        abIn(p)(r)(v) + a(p)(v) * b(p)(r)
      }
    }
  }

  val acOut = for (p <- 0 until pass) yield {
    for (r <- 0 until reuse) yield {
      for (v <- 0 until vecLength) yield {
        acIn(p)(r)(v) + a(p)(v) * c(p)(r)
      }
    }
  }

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new int8_ws_AD_B(vecLength))
    .doSimUntilVoid { dut =>
      import dut._

      io.a #= 0
      io.b #= 0
      io.c #= 0
      io.abIn.foreach(_ #= 0)
      io.acIn.foreach(_ #= 0)
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

      def assign_psum(p: Int) = {
        clockDomain.waitSampling(2)
        for (i <- 0 until reuse + vecLength) {
          for (j <- 0 until vecLength) {
            if (j <= i && i < reuse + j) {
              io.abIn(j) #= abIn(p)(i - j)(j) & 0x3ffff
              io.acIn(j) #= acIn(p)(i - j)(j) & 0x3ffff
            }
          }
          clockDomain.waitSampling(1)
        }
      }

      def assign_input(p: Int) = {
        for (i <- 0 until reuse) {
          io.b #= b(p)(i) & 0xff
          io.c #= c(p)(i) & 0xff
          clockDomain.waitSampling(1)
        }
      }

      def check_output(p: Int) = {
        clockDomain.waitSampling(5)
        for (i <- 0 until reuse + vecLength) {
          println(i)
          for (j <- 0 until vecLength) {
            if (j <= i && i < reuse + j) {

              println((io.abOut(j).toInt << 14) >> 14, abOut(p)(i - j)(j))
              println((io.acOut(j).toInt << 14) >> 14, acOut(p)(i - j)(j))
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
          assign_psum(pp)
        }
        fork {
          assign_input(pp)
        }
        fork {
          check_output(pp)
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
