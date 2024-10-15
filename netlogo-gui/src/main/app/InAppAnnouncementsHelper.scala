package org.nlogo.app

import org.json.simple.parser.{JSONParser, ParseException}
import org.json.simple.{JSONArray, JSONObject}
import java.awt._
import javax.swing._
import org.nlogo.core.I18N
import scala.io.Source
import scala.collection.immutable.List

// JsonObject case class
case class JsonObject(eventId: Int, date: String, title: String, fullText: String)

object InAppAnnouncementsHelper {

  def parseJsonToList(jsonContent: String): List[JsonObject] = {
    try {
      val parser = new JSONParser()
      // Parse the string to get a JSON array
      val jsonArray = parser.parse(jsonContent).asInstanceOf[JSONArray]

      // Debug: Print the size of the array
      println(s"Array size: ${jsonArray.size()}")

      // Convert the JSONArray into a list of JsonObject
      jsonArray.toArray.flatMap { obj =>
        val jsonObject = obj.asInstanceOf[JSONObject]

        // Debug: Print each JSONObject
        println(s"Parsing JSON object: $jsonObject")

        // Extract fields and handle potential nulls
        Option(jsonObject.get("event-id").asInstanceOf[Long].toInt).map { eventId =>
          val title = Option(jsonObject.get("title")).map(_.toString).getOrElse("")
          val fullText = Option(jsonObject.get("fullText")).map(_.toString).getOrElse("")
          val date = Option(jsonObject.get("date")).map(_.toString).getOrElse("")
          // Return a JsonObject
          JsonObject(eventId, date, title, fullText)
        }
      }.toList
    } catch {
      case e: ParseException =>
        throw new Exception(s"Error parsing JSON: ${e.getMessage}",e)
      case e: ClassCastException =>
        throw new Exception(s"Error casting JSON objects: ${e.getMessage}", e)
      case e: Exception =>
        throw new Exception(s"General error: ${e.getMessage}", e)
    }
  }

  def fetchJsonFromUrl(url: String): String = {
    try {
      val source = Source.fromURL(url)
      val content = source.mkString
      source.close()
      content
    } catch {
      case e: Exception =>
        throw new Exception(s"Error fetching JSON from URL: ${e.getMessage}", e)
        ""
    }
  }

  def formatJsonObjectList(jsonObjects: List[JsonObject]): String = {
    jsonObjects.map { obj =>
      s"* ${I18N.gui.get("dialog.interface.update")}: ${obj.date}\n  ${obj.fullText}"
    }.mkString("\n\n")
  }

  def showJsonInDialog(): Unit = {
    val url = "https://ccl.northwestern.edu/netlogo/announce-test.json"

    try {
      val jsonContent = fetchJsonFromUrl(url)
      val jsonObjectList = parseJsonToList(jsonContent)

      // Format the list of JsonObjects into a string
      val formattedString = formatJsonObjectList(jsonObjectList)

      if (!jsonContent.trim.isEmpty) {
        println(formattedString) // Debug: Print formatted string

        // Show the JSON content in a modal dialog with JTextArea
        SwingUtilities.invokeLater(() => {
          // Create a JTextArea
          val textArea = new JTextArea(20, 45)
          textArea.setText(formattedString)
          textArea.setEditable(false)
          // Scroll to the top line
          textArea.setCaretPosition(0)  // Set caret to the start of the text

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
            options(0)               // Default button
          )

          // Handle user input after pressing OK
          if (result == 0) { // 0 is the index for "OK"
            val doNotShowAgain = checkbox.isSelected
            println(s"Do not show again: $doNotShowAgain")
          }
        })
      }

    } catch {
      case e: Exception => throw new Exception(s"Error in showJsonInDialog: ${e.getMessage}",e)
    }
  }
}

