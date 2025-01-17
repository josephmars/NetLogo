// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.prim.etc;

import org.nlogo.core.AgentKindJ;
import org.nlogo.agent.AgentSet;
import org.nlogo.agent.Link;
import org.nlogo.agent.Turtle;
import org.nlogo.api.LogoException;
import org.nlogo.core.Syntax;
import org.nlogo.nvm.Command;
import org.nlogo.nvm.Context;

public final class _layoutspring
    extends Command {
  public _layoutspring() {
    this.switches = true;
  }



  @Override
  public void perform(final Context context)
      throws LogoException {
    AgentSet nodeset = argEvalAgentSet(context, 0, AgentKindJ.Turtle());
    AgentSet linkset = argEvalAgentSet(context, 1, AgentKindJ.Link());
    double springConstant = argEvalDoubleValue(context, 2);
    double springLength = argEvalDoubleValue(context, 3);
    double repulsionConstant = argEvalDoubleValue(context, 4);
    org.nlogo.agent.Layouts.spring
        (world, nodeset, linkset, springConstant, springLength, repulsionConstant,
            context.job.random);
    context.ip = next;
  }
}
