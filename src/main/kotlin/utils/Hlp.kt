package utils.Hlp

import brut.androlib.Androlib
import brut.androlib.ApkDecoder
import brut.androlib.ApkOptions
import brut.androlib.err.CantFindFrameworkResException
import brut.androlib.err.InFileNotFoundException
import brut.androlib.err.OutDirExistsException
import brut.common.BrutException
import brut.directory.DirectoryException
import java.io.File
import java.io.IOException
import java.util.logging.Logger
import kotlin.random.Random

fun processApk(apkName: String): Boolean{
    val logger = Logger.getLogger(Androlib::class.java.name)

    val decoder = ApkDecoder()
    decoder.setForceDelete(true)
    decoder.setForceDecodeManifest(1.toShort())
    decoder.setKeepBrokenResources(true)
    val tmpPath = "/tmp/" + Random.nextInt(100, 999).toString()
    val outDir = File(tmpPath)
    decoder.setOutDir(outDir)
    decoder.setApkFile(File(apkName))
    try {
        decoder.decode()
    } catch (var22: OutDirExistsException) {
        System.err.println("Destination directory (" + outDir.getAbsolutePath() + ") already exists. Use -f switch if you want to overwrite it.")
        logger.info("Destination directory (" + outDir.getAbsolutePath() + ") already exists. Use -f switch if you want to overwrite it.")

        System.exit(1)
    } catch (var23: InFileNotFoundException) {
        System.err.println("Input file ($apkName) was not found or was not readable.")
        logger.info("Input file ($apkName) was not found or was not readable.")
    } catch (var24: CantFindFrameworkResException) {
        System.err.println("Can't find framework resources for package of id: " + var24.pkgId.toString() + ". You must install proper framework files, see project website for more info.")
        logger.info("Can't find framework resources for package of id: " + var24.pkgId.toString() + ". You must install proper framework files, see project website for more info.")
    } catch (var25: IOException) {
        System.err.println("Could not modify file. Please ensure you have permission.")
        logger.info("Could not modify file. Please ensure you have permission.")
    } catch (var26: DirectoryException) {
        System.err.println("Could not modify internal dex files. Please ensure you have permission.")
        logger.info("Could not modify internal dex files. Please ensure you have permission.")
    } finally {

        try {
            decoder.close()
        } catch (var21: IOException) {
        }

    }

    val manifestFile = File(outDir.absolutePath + "/AndroidManifest.xml")
    if (!manifestFile.exists()){
//        cloneApk.isDisable = false
        return false
    }

    var manifestData = manifestFile.inputStream().readBytes().toString(Charsets.UTF_8)

    val regex = "package=\"([^\"]+)".toRegex()
    val packageName = regex.find(manifestData)?.groups?.get(1)?.value ?: ""

    if (packageName.isEmpty()){
//        cloneApk.isDisable = false
        return false
    }

    val replacement =  Random.nextInt(100, 999).toString()

    val packageNameNew = packageName + replacement

    manifestData = manifestData.replace(packageName, packageNameNew)
    manifestFile.outputStream().write(manifestData.toByteArray(Charsets.UTF_8))
    val smaliPackageNameNew = packageNameNew.replace('.', '/')
    val smaliPackageName = packageName.replace('.', '/')


    outDir.walk(FileWalkDirection.TOP_DOWN).forEach {
        val tmpF = File(it.absolutePath ?: "")
        if (!tmpF.exists() || tmpF.isDirectory || !(tmpF.absolutePath ?: "").endsWith(".smali"))
            return@forEach

        val tmpData = tmpF.inputStream().readBytes().toString(Charsets.UTF_8).replace(smaliPackageName, smaliPackageNameNew)
        tmpF.outputStream().write(tmpData.toByteArray(Charsets.UTF_8))

        val fPath = tmpF.absolutePath ?: ""

        val newFPath = fPath.replace(smaliPackageName, smaliPackageNameNew)
        if (!fPath.equals(newFPath)){
            tmpF.copyTo(File(newFPath))
            tmpF.delete()
        }


    }


    val apkOptions = ApkOptions()
    apkOptions.forceBuildAll = true

    val fDirectory = File(apkName).parent ?: ""
    val resultFileName = fDirectory + "/" + apkName.replace(fDirectory, "").replace(Regex("\\.apk", RegexOption.IGNORE_CASE), "$replacement.apk")

    logger.info("***Compiling all together***\n")

    try {

        Androlib(apkOptions).build(File(tmpPath), File(resultFileName))
    }
    catch (ex: BrutException){
        logger.info("{${ex.message}}\n")
    }

    outDir.deleteRecursively()

    return true

}