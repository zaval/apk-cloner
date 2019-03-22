
import javafx.application.Application
import javafx.fxml.FXMLLoader.load
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage


class Main : Application() {

    private val layout = "/main.fxml"
    private val icon = "/logo.png"

    override fun start(primaryStage: Stage?) {
        System.setProperty("prism.lcdtext", "false")
        primaryStage?.title = "APK Cloner"
        primaryStage?.scene = Scene(load<Parent?>(Main.javaClass.getResource(layout)))

        primaryStage?.icons?.add(Image(Main.javaClass.getResourceAsStream(icon)))
        primaryStage?.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Main::class.java)
        }
    }
}
