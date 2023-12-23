import spinal.core._
import spinal.core.sim._
import spinal.lib._
import spinal.lib.eda.bench.Rtl
import xilinx.DSP48E2._
import xilinx.DSP48E2IntArithmetic.cascade.int16_dotp

import scala.language.postfixOps
import scala.util.Random

object eval_int16_dotp extends App {

  val sampleLength = 32
  val vecLength = 8
  val acc = false

  val a = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(256) - 128))
  val b = Array.fill(sampleLength)(Array.fill(vecLength)(Random.nextInt(256) - 128))
  val ab = for (s <- 0 until sampleLength) yield {
    (a(s), b(s)).zipped.map(_ * _).sum
  }

  val accRes = ab.sum

  SimConfig.withFstWave
    .addRtl("data/sim/DSP48E2.v")
    .compile(new int16_dotp(vecLength, acc))
    .doSim { dut =>
      import dut._

      io.a.foreach(_ #= 0)
      io.b.foreach(_ #= 0)
      if (acc) {
        io.valid #= false
        io.last #= false
      }

      clockDomain.forkStimulus(10)
      clockDomain.waitSampling(32)

      def check() = {
        var sum = 0
        for (i <- 0 until sampleLength + latency) {
          for (j <- 0 until vecLength) {
            if (j <= i && i < sampleLength + j) {
              io.a(j) #= a(i - j)(j) & 0xffff
              io.b(j) #= b(i - j)(j) & 0xffff
            }
          }

          if (acc) {
            if (i == vecLength - 1) io.valid #= true
            if (i == sampleLength + vecLength - 1) io.valid #= false

            if (i == sampleLength + vecLength - 2) io.last #= true
            if (i == sampleLength + vecLength - 1) io.last #= false
          }

          if (i > latency) {
            val index = i - latency - 1
            sum += ab(index)
            if (acc) assert(sum == io.ab.toBigInt.toInt)
            else assert(ab(index) == io.ab.toBigInt.toInt)
          }
          clockDomain.waitSampling()
        }
        if (acc) {
          println(accRes, io.ab.toBigInt.toInt)
        }
      }

      for(p<-0 until 4){
        fork{
          check()
        }
        clockDomain.waitSampling(sampleLength)
      }

      clockDomain.waitSampling(32)
      simSuccess()
    }
}
