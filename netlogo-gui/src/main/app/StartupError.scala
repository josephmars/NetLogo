// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app

import java.awt.BorderLayout
import java.awt.event.{ WindowAdapter, WindowEvent }
import javax.swing.{ JFrame, JLabel, JTextArea, WindowConstants }

// Unfortunately errors can occur during startup of the NetLogo GUI.  On Windows these are swallowed by the launcher and
// just give a generic "Failed to launch JVM" message.  Other platforms might be a little better, but this way we'll
// know for sure the user can get some information about what happened, and maybe work around it if the error is clear
// enough or ask for help if not.  The error message JFrame is not at all pretty, but the error should be big and
// copyable at least.  Headless doesn't have these issues as it can just dump startup errors to the console.  -Jeremy B April
// 2023

object StartupError {
  def report(ex: Throwable) {
    System.err.println("NetLogo Startup Error")
    ex.printStackTrace()

    val stack   = ex.getStackTrace.map(_.toString).mkString("\n")
    val message = s"Exception: ${ex.getMessage} (${ex.getClass.toString})\n\nStack Trace:\n$stack"

    val frame = new JFrame("NetLogo Startup Error")
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    frame.addWindowListener(new WindowAdapter() {
      override def windowClosing(e: WindowEvent) {
        sys.exit(1)
      }
    })

    val title = new JLabel("NetLogo encountered an error while starting to run.  See the details below.")
    title.setFont(title.getFont().deriveFont(16f))
    frame.add(title, BorderLayout.NORTH)

    val report = new JTextArea(message)
    report.setFont(report.getFont().deriveFont(18f))
    report.setEditable(false)
    report.setLineWrap(true)
    frame.add(report)
    report.selectAll()

    frame.setSize(640, 640)
    frame.setVisible(true)

  }

}
