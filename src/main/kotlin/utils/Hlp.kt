package utils.Hlp

import brut.androlib.Androlib
import brut.androlib.ApkDecoder
import brut.androlib.ApkOptions
import brut.androlib.err.CantFindFrameworkResException
import brut.androlib.err.InFileNotFoundException
import brut.androlib.err.OutDirExistsException
import brut.common.BrutException
import brut.directory.DirectoryException
import com.android.apksig.ApkSigner
import com.android.apksig.apk.MinSdkVersionException
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import kotlin.random.Random
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import sun.security.provider.X509Factory
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


private val adbPath = System.getenv("ADB") ?: ((System.getenv("HOME") ?: "/home/user") + "/Android/Sdk/platform-tools/adb")

fun String.asResource(): String {
    return javaClass::class.java.getResource(this).readText()
}

fun sign(apkName: String) :Boolean {

    val newFname = apkName.replace(Regex("\\.apk$"), "-signed.apk")
    val signerConfigs = ArrayList<ApkSigner.SignerConfig>(1)

    var privateKeyContent = "/private_key.pem".asResource()
    privateKeyContent = privateKeyContent.replace("\n", "").replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
    val kf = KeyFactory.getInstance("RSA")
    val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyContent))
    val privKey: PrivateKey = kf.generatePrivate(keySpecPKCS8)

    var publicKeyContent = "/cert.pem".asResource()
    publicKeyContent = publicKeyContent.replace("\n", "").replace("-----BEGIN PUBLIC KEY-----", "")
        .replace("-----END PUBLIC KEY-----", "")


    val decoded =
        Base64.getDecoder().decode(publicKeyContent.replace(X509Factory.BEGIN_CERT, "").replace(X509Factory.END_CERT, ""))

    val cert: X509Certificate = CertificateFactory.getInstance("X.509")?.generateCertificate(ByteArrayInputStream(decoded)) as X509Certificate

    val certificates = ArrayList<X509Certificate>(1)
    certificates.add(cert)

    val signerConfig = ApkSigner.SignerConfig.Builder(
        "key", privKey, certificates.toList()
    )
        .build()
    signerConfigs.add(signerConfig)


    val apkSignerBuilder = ApkSigner.Builder(signerConfigs)
        .setInputApk(File(apkName))
        .setOutputApk(File(newFname))
        .setOtherSignersSignaturesPreserved(false)
    val apkSigner = apkSignerBuilder.build()

    try {
        apkSigner.sign()
    } catch (e: MinSdkVersionException) {
        var msg = e.message
        if (msg != null) {
            if (!msg.endsWith(".")) {
                msg += '.'.toString()
            }
        }
        throw MinSdkVersionException(
            "Failed to determine APK's minimum supported platform version" + ". Use --min-sdk-version to override",
            e
        )
    }


    return true

}

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
        return false
    }

    var manifestData = manifestFile.inputStream().readBytes().toString(Charsets.UTF_8)

    val regex = "package=\"([^\"]+)".toRegex()
    val packageName = regex.find(manifestData)?.groups?.get(1)?.value ?: ""

    if (packageName.isEmpty()){
        return false
    }

    val replacement =  Random.nextInt(100, 999).toString()

    val packageNameNew = packageName + replacement

    manifestData = manifestData.replace(packageName, packageNameNew)
    manifestFile.outputStream().write(manifestData.toByteArray(Charsets.UTF_8))
    val smaliPackageNameNew = packageNameNew.replace('.', '/')
    val smaliPackageName = packageName.replace('.', '/')


    logger.info("*** Changing classes ***")
    outDir.walk(FileWalkDirection.TOP_DOWN).forEach {
        val tmpF = File(it.absolutePath ?: "")
        if (!tmpF.exists() || tmpF.isDirectory || !(tmpF.absolutePath ?: "").endsWith(".smali"))
            return@forEach

        val tmpData = tmpF.inputStream().readBytes().toString(Charsets.UTF_8).replace(smaliPackageName, smaliPackageNameNew)
        tmpF.outputStream().write(tmpData.toByteArray(Charsets.UTF_8))

        val fPath = tmpF.absolutePath ?: ""

        val newFPath = fPath.replace(smaliPackageName, smaliPackageNameNew)
        if (fPath != newFPath){
            tmpF.copyTo(File(newFPath))
            tmpF.delete()
        }


    }


    val apkOptions = ApkOptions()
    apkOptions.forceBuildAll = true

    val fDirectory = File(apkName).parent ?: ""
    val resultFileName = fDirectory + "/" + apkName.replace(fDirectory, "").replace(Regex("\\.apk", RegexOption.IGNORE_CASE), "$replacement.apk")

    logger.info("*** Compiling all together ***")

    try {

        Androlib(apkOptions).build(File(tmpPath), File(resultFileName))
    }
    catch (ex: BrutException){
        logger.info("{${ex.message}}\n")
    }

    sign(resultFileName)

//    outDir.deleteRecursively()

    return true

}


fun String.execute(workingDir: File): String {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(2, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch(e: Exception) {
        e.printStackTrace()
        return ""
    }
}

fun parseAll(pattern: Regex, str: String): List<String>{
    val result = ArrayList<String>()

    var match = pattern.find(str)
    while (match != null){
        result.add(match.groupValues[1])
        match = match.next()
    }
    return result.toList()
}

fun parse(pattern: Regex, str: String): String{

    val match = pattern.find(str)
    return match?.groupValues?.get(1) ?: ""

}



fun getAdbDevices(): List<String>{

    val adbResult = "$adbPath devices".execute(File("."))
    println(adbResult)

    return parseAll("(\\S+)\tdevice".toRegex(), adbResult)

}

fun getAdbInstalledApps(device: String): List<String>{
    val adbResult = "$adbPath -s $device shell pm list packages".execute(File("."))
    val result = parseAll("package:(\\S+)".toRegex(), adbResult)
    return  result
}

fun downloadAdbApp(device: String, app: String, path: String): String{
    if (app.isEmpty())
        return ""
    println(app)
    val adbResult = "$adbPath -s $device shell pm path $app".execute(File("."))

    val packagePath =  parse("package:(\\S+)".toRegex(), adbResult)

    val res = "$adbPath -s $device pull $packagePath $path".execute(File("."))
    println(res)
    return "$path/${File(packagePath).name}"
}

fun installAdbApp(device: String, apk: String){
    val res = "$adbPath -s $device install $apk".execute(File("."))
    println(res)
}