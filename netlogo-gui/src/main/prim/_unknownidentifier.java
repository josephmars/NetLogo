// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.prim;

// This is used by IdentifierParser's "forgiving" mode, used by
// AutoConverter, in which unknown identifiers are assumed to be
// references to global variables that the compiler doesn't know
// about. - ST 7/7/06

import org.nlogo.core.Syntax;
import org.nlogo.nvm.Reporter;

public final class _unknownidentifier
    extends Reporter {
  @Override
  public Object report(final org.nlogo.nvm.Context context) {
    throw new IllegalStateException();
  }


}
