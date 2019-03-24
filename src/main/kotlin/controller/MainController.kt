package controller
import brut.androlib.Androlib
import brut.androlib.ApkDecoder
import brut.androlib.ApkOptions
import brut.androlib.err.CantFindFrameworkResException
import brut.androlib.err.InFileNotFoundException
import brut.androlib.err.OutDirExistsException
import brut.common.BrutException
import brut.directory.DirectoryException
import utils.TextAreaHandler
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import java.io.File
import java.io.IOException
import java.util.logging.Logger
import kotlinx.coroutines.*
import kotlin.random.Random
import kotlin.system.exitProcess


class MainController {
    @FXML
    lateinit var browseApk: Button

    @FXML
    lateinit var mainBox: VBox

    @FXML
    lateinit var apkFname: TextField

    @FXML
    lateinit var cloneApk : Button

    @FXML
    lateinit var loggerText: TextArea


    private var logger = Logger.getLogger(Androlib::class.java.name)

    fun switchUI(enable: Boolean){
        browseApk.isDisable = !enable
        apkFname.isDisable = !enable
        cloneApk.isDisable = !enable
    }


    fun initialize(){

        utils.Hlp.processApk("asd")

        logger.addHandler(TextAreaHandler(loggerText))

        browseApk.setOnAction {
            val openFileDialog = FileChooser()
            openFileDialog.extensionFilters.addAll(FileChooser.ExtensionFilter("APK File", "*.apk"))
            val fname: File? = openFileDialog.showOpenDialog(mainBox.scene.window)
            if (fname != null){
                apkFname.text = fname.canonicalPath
            }
        }

        cloneApk.setOnAction {

            cloneApk.isDisable = true
            loggerText.appendText("*** Start cloning file ${apkFname.text} ***\n")



             GlobalScope.launch{

                 switchUI(false)
                 try {
                     utils.Hlp.processApk(apkFname.text)
                 } catch (e: Exception){
                     loggerText.appendText("@@@ Can't clone this apk @@@\n${e.localizedMessage}")
                 }

                 switchUI(true)
                 loggerText.appendText("*** Clone finished ***\n")

             }

        }

    }
}
