package xilinx.DSP48E2

import spinal.core._
import scala.language.postfixOps

case class DSP48E2AttrBuild() {
  val attrs = new DSP48E2Attributes

  def setAsALU(SIMD: Int) = {
    attrs.MREG = 0
    attrs.USE_MULT = "NONE"
    SIMD match {
      case 1 => attrs.USE_SIMD = "ONE48"
      case 2 => attrs.USE_SIMD = "TWO24"
      case 4 => attrs.USE_SIMD = "FOUR12"
    }
  }

  def setMultiplier(mode: String = "M=AxB") = {
    attrs.MREG = 1
    mode match {
      case "M=AxB" | "-" =>
        attrs.AMULTSEL = "A"
        attrs.BMULTSEL = "B"
      case "M=PAxB" =>
        attrs.AMULTSEL = "AD"
        attrs.BMULTSEL = "B"
      case "M=AxPA" =>
        attrs.AMULTSEL = "A"
        attrs.BMULTSEL = "AD"
      case "M=PAxPA" =>
        attrs.AMULTSEL = "AD"
        attrs.BMULTSEL = "AD"
    }
  }

  def setStaticINMODE(pipe: (Int, Int, Int, Int, Int), mode: String = "PA=D+A") = {
    val (aPipe, bPipe, cPipe, dPipe, adPipe) = pipe
    val INMODE = Array(0, 0, 0, 0, 0)
    require(aPipe <= 2)
    attrs.AREG = aPipe
    aPipe match {
      case 1 => INMODE(0) = 1
      case 2 => INMODE(0) = 0
    }

    require(bPipe <= 2)
    attrs.BREG = bPipe
    bPipe match {
      case 1 => INMODE(4) = 1
      case 2 => INMODE(4) = 0
    }

    require(cPipe <= 1)
    attrs.CREG = cPipe

    require(dPipe <= 1)
    attrs.DREG = dPipe

    require(adPipe <= 1)
    attrs.ADREG = adPipe

    INMODE(1) = 0
    INMODE(2) = 1
    INMODE(3) = 0
    mode match {
      case "PA=D+A" | "-" =>
        attrs.PREADDINSEL = "A"
      case "PA=D+B" =>
        attrs.PREADDINSEL = "B"
      case "PA=D-A" =>
        attrs.PREADDINSEL = "A"
        INMODE(3) = 1
      case "PA=D-B" =>
        attrs.PREADDINSEL = "B"
        INMODE(3) = 1
      case "PA=A" =>
        attrs.PREADDINSEL = "A"
        INMODE(2) = 0
      case "PA=B" =>
        attrs.PREADDINSEL = "B"
        INMODE(2) = 0
      case "PA=D" =>
        INMODE(1) = 1
    }
    Vec(INMODE.map(s => Bool(s.toBoolean))).asBits
  }

  def setStaticOPMODE(mode: String = "P=M") = {
    val OPMODE = Array(0, 0, 0, 0)
    val bitWidth = Array(2, 2, 3, 2)
    mode match {
      // M
      case "P=M" | "-" =>
        OPMODE(0) = 1; OPMODE(1) = 1; OPMODE(2) = 0; OPMODE(3) = 0
      case "P=M+C" =>
        OPMODE(0) = 1; OPMODE(1) = 1; OPMODE(2) = 0; OPMODE(3) = 3
      case "P=M+P" =>
        OPMODE(0) = 1; OPMODE(1) = 1; OPMODE(2) = 2; OPMODE(3) = 0
      case "P=M+PCIN" =>
        OPMODE(0) = 1; OPMODE(1) = 1; OPMODE(2) = 1; OPMODE(3) = 0
      case "P=M+C+P" =>
        OPMODE(0) = 1; OPMODE(1) = 1; OPMODE(2) = 2; OPMODE(3) = 3
      case "P=M+C+PCIN" =>
        OPMODE(0) = 1; OPMODE(1) = 1; OPMODE(2) = 1; OPMODE(3) = 3
      // AB
      case "P=AB" =>
        OPMODE(0) = 3; OPMODE(1) = 0; OPMODE(2) = 0; OPMODE(3) = 0
      case "P=AB+C" =>
        OPMODE(0) = 3; OPMODE(1) = 0; OPMODE(2) = 0; OPMODE(3) = 3
      case "P=AB+P" =>
        OPMODE(0) = 3; OPMODE(1) = 0; OPMODE(2) = 2; OPMODE(3) = 0
      case "P=AB+PCIN" =>
        OPMODE(0) = 3; OPMODE(1) = 0; OPMODE(2) = 1; OPMODE(3) = 0
      case "P=AB+C+P" =>
        OPMODE(0) = 3; OPMODE(1) = 0; OPMODE(2) = 2; OPMODE(3) = 3
      case "P=AB+C+PCIN" =>
        OPMODE(0) = 3; OPMODE(1) = 0; OPMODE(2) = 1; OPMODE(3) = 3
    }
    Vec((OPMODE, bitWidth).zipped.map((s, w) => U(s, w bits))).asBits
  }
}