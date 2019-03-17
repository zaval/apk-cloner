package controller
import brut.androlib.Androlib
import brut.androlib.ApkDecoder
import brut.androlib.err.CantFindFrameworkResException
import brut.androlib.err.InFileNotFoundException
import brut.androlib.err.OutDirExistsException
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


    fun initialize(){

        logger.addHandler(TextAreaHandler(loggerText))

        browseApk.setOnAction {
            val openFileDialog = FileChooser()
            openFileDialog.extensionFilters.addAll(FileChooser.ExtensionFilter("APK File", "*.apk"))
            var fname: File? = openFileDialog.showOpenDialog(mainBox.scene.window)
            if (fname != null){
                apkFname.text = fname.canonicalPath
            }
        }

        cloneApk.setOnAction {

            cloneApk.isDisable = true
            loggerText.appendText("*** Start cloning file ${apkFname.text} ***\n")



             GlobalScope.launch{


                val apkName = apkFname.text
                val decoder = ApkDecoder()
                decoder.setForceDelete(true)
                decoder.setForceDecodeManifest(1.toShort())
                decoder.setKeepBrokenResources(true)
                val outDir = File("/tmp/1")
                decoder.setOutDir(outDir)
                decoder.setApkFile(File(apkFname.text))
                try {
                    decoder.decode()
                } catch (var22: OutDirExistsException) {
                    System.err.println("Destination directory (" + outDir.getAbsolutePath() + ") already exists. Use -f switch if you want to overwrite it.")
                    loggerText.appendText("Destination directory (" + outDir.getAbsolutePath() + ") already exists. Use -f switch if you want to overwrite it.")

                    System.exit(1)
                } catch (var23: InFileNotFoundException) {
                    System.err.println("Input file ($apkName) was not found or was not readable.")
                    loggerText.appendText("Input file ($apkName) was not found or was not readable.")
                } catch (var24: CantFindFrameworkResException) {
                    System.err.println("Can't find framework resources for package of id: " + var24.pkgId.toString() + ". You must install proper framework files, see project website for more info.")
                    loggerText.appendText("Can't find framework resources for package of id: " + var24.pkgId.toString() + ". You must install proper framework files, see project website for more info.")
                } catch (var25: IOException) {
                    System.err.println("Could not modify file. Please ensure you have permission.")
                    loggerText.appendText("Could not modify file. Please ensure you have permission.")
                } catch (var26: DirectoryException) {
                    System.err.println("Could not modify internal dex files. Please ensure you have permission.")
                    loggerText.appendText("Could not modify internal dex files. Please ensure you have permission.")
                } finally {
                    try {
                        decoder.close()
                    } catch (var21: IOException) {
                    }

                }

                 val manifestFile = File(outDir.absolutePath + "/AndroidManifest.xml")

                 var manifestData = manifestFile.inputStream().readBytes().toString(Charsets.UTF_8)

                 val regex = "package=\"([^\"]+)".toRegex()
                 val packageName = regex.find(manifestData)?.groups?.get(1)?.value ?: ""
                 loggerText.appendText("$packageName\n")
                 if (packageName.isEmpty()){
                     return@launch
                 }

                val packageNameNew = packageName + Random.nextInt(100, 999).toString()

                 manifestData = manifestData.replace(packageName, packageNameNew)
                 manifestFile.outputStream().write(manifestData.toByteArray(Charsets.UTF_8))
                 val smaliPackageName = packageNameNew.replace('.', '/')


                outDir.walk(FileWalkDirection.TOP_DOWN).forEach {
                    val tmpF = File(it.absolutePath ?: "")
                    if (!tmpF.exists() || tmpF.isDirectory)
                        return@forEach
                    var tmpData = tmpF.inputStream().readBytes().toString(Charsets.UTF_8).replace(packageName, smaliPackageName)
                    tmpF.outputStream().write(tmpData.toByteArray(Charsets.UTF_8))


                }


                 cloneApk.isDisable = false
                 loggerText.appendText("*** Clone finished ***\n")

             }

        }

    }
}
