package utils


import javafx.scene.control.TextArea
import java.util.logging.Handler
import java.util.logging.LogRecord

class TextAreaHandler(textArea: TextArea) : Handler() {
    private val textArea = textArea
    override fun publish(record: LogRecord?) {
        textArea.appendText(record?.message + "\n")
    }

    override fun flush() {

    }

    override fun close() {

    }
}