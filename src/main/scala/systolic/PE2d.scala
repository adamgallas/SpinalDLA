package systolic

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

class PE2d(
            selfInput: Bundle,
            selfOutput: Bundle,
            shareVertical: Bundle,
            shareHorizontal: Bundle,
            isStagedVertical: Boolean = true,
            isStagedHorizontal: Boolean = true,
            isLastPEVertical: Boolean = false,
            isLastPEHorizontal: Boolean = false
          ) extends Component {

  val inp = if (selfInput != null) selfInput.clone().asInput() else null
  val out = if (selfOutput != null) selfOutput.clone().asOutput() else null
  val srcV = shareVertical.clone().asInput()
  val srcH = shareHorizontal.clone().asInput()
  val dstV = if (!isLastPEVertical) shareVertical.clone().asOutput() else null
  val dstH = if (!isLastPEHorizontal) shareHorizontal.clone().asOutput() else null

  if (!isLastPEVertical) {
    if (isStagedVertical) {
      dstV := RegNext(srcV)
    }
    else {
      dstV := srcV
    }
  }

  if (!isLastPEHorizontal) {
    if (isStagedHorizontal) {
      dstH := RegNext(srcH)
    }
    else {
      dstH := srcH
    }
  }
}