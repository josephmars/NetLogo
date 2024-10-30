package org.nlogo.app

import org.json.simple.parser.{JSONParser, ParseException}
import org.json.simple.{JSONArray, JSONObject}
import org.nlogo.app.common.FindDialog
import org.nlogo.app.infotab.InfoFormatter

import java.awt._
import javax.swing._
import java.util.prefs.{Preferences => JPreferences}
import org.nlogo.core.I18N

import java.awt.event.{FocusEvent, FocusListener}
import scala.io.Source
import scala.collection.immutable.List

// JsonObject case class
case class JsonObject(eventId: Int, date: String, title: String, fullText: String)

object InAppAnnouncementsHelper {
  val lastSeenEventIdKey: String = "lastSeenEventId"  // The key for the most recently seen event-id

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
        // Sort the list by eventId in descending order
        .sortBy(_.eventId)(Ordering[Int].reverse)
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
    val prefs = JPreferences.userNodeForPackage(getClass)

    try {
      val jsonContent = fetchJsonFromUrl(url)
      val jsonObjectList = parseJsonToList(jsonContent)
      println(s"prefs is ${prefs}\n\n")
      // Format the list of JsonObjects into a string
      val formattedString = formatJsonObjectList(jsonObjectList)
      val lastSeenEventId = prefs.getInt(lastSeenEventIdKey, -1); // Returns -1 if "event-id" is not found
      println(s"lastSeenEventId: $lastSeenEventId, head of the list: ${jsonObjectList.head.eventId}")
      if(jsonObjectList.head.eventId > lastSeenEventId){
        println(s"Show this ${jsonObjectList.head.eventId}")
      }
      else {
        println(s"Don't show this  ${jsonObjectList.head.eventId}")
        return
      }
      val html  = InfoFormatter.toInnerHtml(formattedString)

      if (!jsonContent.trim.isEmpty) {

        // Show the JSON content in a modal dialog with JEditorPane
        SwingUtilities.invokeLater(() => {
          val editorPane: JEditorPane = new JEditorPane { self =>
            addFocusListener(new FocusListener {
              def focusGained(fe: FocusEvent): Unit = { FindDialog.watch(self) }
              def focusLost(fe: FocusEvent): Unit = { if (!fe.isTemporary) FindDialog.dontWatch(self) }
            })
            setDragEnabled(false)
            setEditable(false)
            setContentType("text/html")
            setOpaque(false)  // Make sure JEditorPane is transparent for scrollPane
            setText(html)
            setCaretPosition(0)
          }

          // Create a JScrollPane to wrap the JEditorPane
          val scrollPane = new JScrollPane(editorPane)
          scrollPane.setPreferredSize(new Dimension(500, 400))  // Set preferred size for scroll area

          // Create a JCheckBox
          val checkbox = new JCheckBox(I18N.gui.get("dialog.interface.newsNotificationDoNotShowAgain"))

          // Create a JPanel to hold both the JTextArea and JCheckBox
          val panel = new JPanel(new BorderLayout())
          panel.add(scrollPane, BorderLayout.CENTER)
          panel.add(checkbox, BorderLayout.SOUTH)

          // Custom OK button text
          val options: Array[AnyRef] = Array(I18N.gui.get("common.buttons.ok"))

          // Show the JOptionPane dialog with only an OK button
          JOptionPane.showOptionDialog(
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
          val doNotShowAgain = checkbox.isSelected
          if (doNotShowAgain) {
            println(s"Do not show again: $doNotShowAgain")
            prefs.putInt("lastSeenEventId", jsonObjectList.head.eventId)
          }
        })
      }
    } catch {
      case e: Exception => throw new Exception(s"Error in showJsonInDialog: ${e.getMessage}",e)
    }
  }
}

