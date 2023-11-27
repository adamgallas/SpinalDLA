import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArith.dualCascade.int8_ws_prefetch

import scala.language.postfixOps
import scala.util.Random

object eval_int8_ws_prefetch extends App {

  val pass = 8
  val reuse = 8
  val vecLength = 8

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
    .compile(new int8_ws_prefetch(vecLength))
    .doSimUntilVoid { dut =>
      import dut._

      var goFetch = true
      var fetchFinish = false
      var fetchStop = false
      var checking = false

      io.a #= 0
      io.b.foreach(_ #= 0)
      io.c.foreach(_ #= 0)
      io.CE1 #= false
      io.CE2 #= false
      io.rstCE1.foreach(_ #= true)

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      def fetch(p: Int) = {

        println(s"fetch $p begin")

        goFetch = false
        if (p == pass) {
          io.CE2 #= true
          clockDomain.waitSampling(1)
          io.CE2 #= false
          fetchStop = true
          fetchFinish = true
        }
        else {

          io.CE1 #= false
          io.CE2 #= false
          for (v <- 0 until vecLength) {
            io.a #= a(p)(vecLength - v - 1) & 0xff
            io.CE1 #= true

            if (v == 0 && p != 0) io.CE2 #= true
            else io.CE2 #= false

            if (v == vecLength - 1) io.rstCE1.foreach(_ #= true)
            else io.rstCE1.foreach(_ #= false)

            clockDomain.waitSampling(1)
          }
          io.CE1 #= false
          io.rstCE1.foreach(_ #= false)
        }
        if (p == 0) {
          goFetch = true
          fetchFinish = false
        }
        else {
          goFetch = false
          fetchFinish = true
        }
        println(s"fetch $p end")
      }

      def check(p: Int): Unit = {

        println(s"check $p begin")

        checking = true
        for (i <- 0 until reuse + latency) {
          if (i == reuse) {
            goFetch = true
          }
          if (i == reuse) {
            checking = false
          }
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

        println(s"check $p end")
      }

      fork {
        var fetchCnt = 0
        while (true) {
          if (goFetch && !fetchStop) {
            fetch(fetchCnt)
            fetchCnt += 1
          }
          else {
            clockDomain.waitSampling()
          }
        }
      }

      fork {
        var checkCnt = 0
        while (checkCnt < pass) {
          if (fetchFinish && !checking) {
            fork {
              checkCnt += 1
              check(checkCnt - 1)
            }
          }
          clockDomain.waitSampling()
        }
        clockDomain.waitSampling(128)
        simSuccess()
      }
    }
}
