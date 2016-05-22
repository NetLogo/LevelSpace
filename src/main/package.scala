package org.nlogo

import org.nlogo.api.World

package object ls {
  type FutureJob[R] = Function1[World, R]
}

