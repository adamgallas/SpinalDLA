import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArith.singleCascade.xdotp_four12_chain

import scala.language.postfixOps
import scala.util.Random

object eval_xdotp_four12_chain extends App {

  val sampleLength = 16
  val vecLength = 8
  val acc = true

  val aSel = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(2)))
  val bSel = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(2)))
  val a = Array.fill(sampleLength)(Array.fill(vecLength)(Array.fill(4)(Random.nextInt(256) - 128)))
  val b = Array.fill(sampleLength)(Array.fill(vecLength)(Array.fill(4)(Random.nextInt(256) - 128)))
  val ab = for (s <- 0 until sampleLength) yield {
    for (t <- 0 until 4) yield {
      (for (i <- 0 until vecLength) yield {
        aSel(s)(i) * a(s)(i)(t) + bSel(s)(i) * b(s)(i)(t)
      }).sum
    }
  }

  val accRes = ab.transpose.map(_.sum)

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new xdotp_four12_chain(vecLength, acc))
    .doSim { dut =>
      import dut._

      io.aSel.foreach(_ #= 0)
      io.bSel.foreach(_ #= 0)
      io.a.foreach(_.foreach(_ #= 0))
      io.b.foreach(_.foreach(_ #= 0))
      if (acc) {
        io.valid #= false
        io.last #= false
      }

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      var sum = Array.fill(4)(0)

      for (i <- 0 until sampleLength + latency) {

        for (j <- 0 until vecLength) {
          if (j <= i && i < sampleLength + j) {
            io.aSel(j) #= aSel(i - j)(j)
            io.bSel(j) #= bSel(i - j)(j)
            (io.a(j), a(i - j)(j)).zipped.foreach(_ #= _ & 0xfff)
            (io.b(j), b(i - j)(j)).zipped.foreach(_ #= _ & 0xfff)
          }
          else {
            io.aSel(j) #= 0
            io.bSel(j) #= 0
            io.a(j).foreach(_ #= 0)
            io.b(j).foreach(_ #= 0)
          }
        }

        if (vecLength - 1 <= i && i < sampleLength + vecLength - 1) {
          if (acc) {
            io.valid #= true
            io.last #= (i == sampleLength + vecLength - 2)
          }
        }
        else {
          if (acc) {
            io.valid #= false
            io.last #= false
          }
        }

        if (i > latency) {
          val index = i - latency - 1
          for (t <- 0 until 4) {
            sum(t) += ab(index)(t)
            if (acc) assert(sum(t) == (io.ab(t).toInt << 20) >> 20)
            else assert(ab(index)(t) == (io.ab(t).toInt << 20) >> 20)
          }
        }
        clockDomain.waitSampling()
      }

      if (acc) {
        println(accRes.mkString(", "))
        println(io.ab.map(v => (v.toInt << 20) >> 20).mkString(", "))
      }

      io.aSel.foreach(_ #= 0)
      io.bSel.foreach(_ #= 0)
      io.a.foreach(_.foreach(_ #= 0))
      io.b.foreach(_.foreach(_ #= 0))
      clockDomain.waitSampling(32)

      simSuccess()
    }
}
