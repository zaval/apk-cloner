package controller
import brut.androlib.Androlib
import brut.androlib.ApkDecoder
import brut.androlib.ApkOptions
import brut.androlib.err.CantFindFrameworkResException
import brut.androlib.err.InFileNotFoundException
import brut.androlib.err.OutDirExistsException
import brut.common.BrutException
import brut.directory.DirectoryException
import javafx.beans.Observable
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import utils.TextAreaHandler
import javafx.fxml.FXML
import javafx.scene.control.*
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

    @FXML
    lateinit var devicesCombo: ComboBox<String>

    @FXML
    lateinit var updateDevices: Button

    @FXML
    lateinit var appsList: ListView<String>

    @FXML
    lateinit var mainTab: TabPane


    private var logger = Logger.getLogger(Androlib::class.java.name)

    private fun switchUI(enable: Boolean){
        browseApk.isDisable = !enable
        apkFname.isDisable = !enable
        cloneApk.isDisable = !enable
        updateDevices.isDisable = !enable
        devicesCombo.isDisable = !enable
        appsList.isDisable = !enable
    }


    lateinit var  lines: ObservableList<String>

    private fun updateAdbDevices(){
        val res = GlobalScope.async {
            lines = FXCollections.observableList(utils.Hlp.getAdbDevices())

        }

        runBlocking {
            res.await()
            devicesCombo.items = lines
            if (lines.size > 0)
                devicesCombo.selectionModel.select(0)

        }

        loadApps()


    }

    lateinit var apps: ObservableList<String>

    private fun loadApps(){

        if (devicesCombo.selectionModel.isEmpty)
            return
        val res = GlobalScope.async {
            apps = FXCollections.observableList(utils.Hlp.getAdbInstalledApps(devicesCombo.selectionModel.selectedItem))
        }

        runBlocking {
            res.await()
            appsList.items = apps
            println(apps)
        }

    }

    fun initialize(){

        updateAdbDevices()

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

            var apkPathText = apkFname.text
            loggerText.appendText("*** Start cloning file ${apkPathText} ***\n")


            GlobalScope.launch{




                 if (mainTab.selectionModel.selectedIndex == 1){
                     if (devicesCombo.selectionModel.isEmpty){
//                         switchUI(true)
                         logger.info("*** Clone finished ***\n")
                         return@launch
                     }

                     val tmpPath = "/tmp/" + Random.nextInt(100, 999).toString()
                     File(tmpPath).mkdirs()
                     apkPathText = utils.Hlp.downloadAdbApp(devicesCombo.selectionModel.selectedItem, appsList.selectionModel.selectedItem ?: "", tmpPath)
                     if (apkPathText.isEmpty())
                     {
//                         switchUI(true)
                         logger.info("*** Clone finished ***\n")
                         return@launch
                     }

                 }

                 switchUI(false)
                 try {
                     utils.Hlp.processApk(apkPathText)
                 } catch (e: Exception){
                     loggerText.appendText("@@@ Can't clone this apk @@@\n${e.localizedMessage}")
                 }

                 if (mainTab.selectionModel.selectedIndex == 1){

                     val fnameToInstall = File(apkPathText).parentFile.listFiles {
                         _, name ->

                         name.endsWith("-signed.apk")

                     }

                     println(fnameToInstall.size)

                     if (fnameToInstall.isNotEmpty()){
                         logger.info("*** Uploading application ***\nCheck, maybe you need some action on your device!")
                         utils.Hlp.installAdbApp(devicesCombo.selectionModel.selectedItem, fnameToInstall[0]?.absolutePath ?: "")
                     }

                 }

                 switchUI(true)
                 loggerText.appendText("*** Clone finished ***\n")

             }

        }

        updateDevices.setOnAction {
            updateAdbDevices()
        }

        devicesCombo.selectionModel.selectedItemProperty().addListener {
                _: Observable ->
                loadApps()

        }

    }
}
