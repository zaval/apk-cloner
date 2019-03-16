
import javafx.application.Application
import javafx.fxml.FXMLLoader.load
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage


class Main : Application() {

    val layout = "/main.fxml"
//    val layout = "resources/main.fxml"

    override fun start(primaryStage: Stage?) {
        System.setProperty("prism.lcdtext", "false") // for beautiful fonts on linux
        primaryStage?.title = "APK Cloner"
        primaryStage?.scene = Scene(load<Parent?>(Main.javaClass.getResource(layout)))
        primaryStage?.show()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            launch(Main::class.java)
        }
    }
}
///home/zaval/IdeaProjects/com.github.zaval.apkcloner/src/resources/main.fxml