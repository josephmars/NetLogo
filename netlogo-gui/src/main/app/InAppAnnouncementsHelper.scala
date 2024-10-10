package org.nlogo.app

import java.awt._
import javax.swing._
import org.nlogo.core.I18N

import java.net.URL
import scala.io.Source

object InAppAnnouncementsHelper {


  def downloadJson(url: String): String = {
    try {
      val source = Source.fromURL(new URL(url))
      val jsonString = source.mkString
      source.close()
      jsonString.replace("\\n", "\n")
    } catch {
      case e: Exception =>
        s"Failed to download JSON: ${e.getMessage}"
    }
  }

    def showJsonInDialog() {

      //create new file with a static method that contains the next new code and there is one method call to that static code
      val url = "https://ccl.northwestern.edu/netlogo/announce-test.json"
      val jsonContent = downloadJson(url)

      if (!jsonContent.isEmpty) {
          // Show the JSON content in a modal dialog with JTextArea
        SwingUtilities.invokeLater(() => {
        // Create a JTextArea
        val textArea = new JTextArea(10, 30)
        textArea.setText(jsonContent)
        textArea.setEditable(false)

        // Create a JScrollPane to wrap the JTextArea
        val scrollPane = new JScrollPane(textArea)

        // Create a JCheckBox
        val checkbox = new JCheckBox(I18N.gui.get("dialog.interface.newsNotificationDoNotShowAgain"))

        // Create a JPanel to hold both the JTextArea and JCheckBox
        val panel = new JPanel(new BorderLayout())
        panel.add(scrollPane, BorderLayout.CENTER)
        panel.add(checkbox, BorderLayout.SOUTH)

        // Custom OK button text
        val options: Array[AnyRef] = Array(I18N.gui.get("common.buttons.ok"))

        // Show the JOptionPane dialog with only an OK button
        val result = JOptionPane.showOptionDialog(
          null,                     // Parent component
          panel,                    // Content to display
          I18N.gui.get("dialog.interface.newsNotificationTitle"),  // Dialog title
          JOptionPane.DEFAULT_OPTION, // No default button selection
          JOptionPane.PLAIN_MESSAGE, // No icon (plain message)
          null,                     // No custom icon
          options,                  // Custom options (OK only)
          options(0)                // Default button
        )

        // Handle user input after pressing OK
        if (result == 0) { // 0 is the index for "OK"
          val doNotShowAgain = checkbox.isSelected
          println(s"Do not show again: $doNotShowAgain")
        }
      })
    }

    }
  }

