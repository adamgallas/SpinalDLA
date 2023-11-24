package systolic

import spinal.core._
import spinal.lib._

import scala.language.postfixOps

class PE1d(
            selfInput: Bundle,
            selfOutput: Bundle,
            share: Bundle,
            isStaged: Boolean = true,
            isLastPE: Boolean = false
          ) extends Component {

  val inp = if (selfInput != null) selfInput.clone().asInput() else null
  val out = if (selfOutput != null) selfOutput.clone().asOutput() else null
  val src = share.clone().asInput()
  val dst = if (!isLastPE) share.clone().asOutput() else null

  if (!isLastPE) {
    if (isStaged) {
      dst := RegNext(src)
    }
    else {
      dst := src
    }
  }
}
