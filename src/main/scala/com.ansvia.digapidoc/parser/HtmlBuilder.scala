package com.ansvia.digapidoc.parser

import java.io.{FileOutputStream, File}
import com.ansvia.commons.logging.Slf4jLogger
import org.apache.commons.io.FileUtils

/**
 * Author: robin
 * Date: 10/11/13
 * Time: 2:26 PM
 *
 */

class HtmlBuilder(docs:Seq[DocBase]) extends Slf4jLogger {

    var title = ""
    var desc = ""
    var outDir = ""
    var templateDir = ""

    def generate(){

        if (title == "")
            warn("Title not set")

        if (desc == "")
            warn("Description not set")

        if (templateDir == "")
            throw new Exception("No template dir")

        if (outDir == "")
            throw new Exception("No out dir")

        if (!new File(templateDir).exists())
            throw new Exception("out dir " + outDir + " not exists")

        if (!new File(outDir).exists())
            throw new Exception("out dir " + outDir + " not exists")

        import scala.io
        import com.ansvia.commons.StringTemplate._

        val sf = sourceFile("index.html")
        val source = io.Source.fromFile(sf).mkString

        var docStr = StringBuilder.newBuilder
        var apiNavsStr = StringBuilder.newBuilder

        for (doc <- docs){
            docStr ++= doc.toHtmlString

            doc match {
                case DocGroup(name) =>
                    apiNavsStr ++= <li><a href={"#" + name}>{name}</a></li>.toString
                case _ =>
            }
        }

        val result = source.render(Map("api.header.title" -> title,
            "api.header.desc" -> desc,
            "api-navs" -> apiNavsStr.result(),
            "api-docs" -> docStr.result()))

        docStr.clear()
        apiNavsStr.clear()

        val out = outFile("index.html")

        val os = new FileOutputStream(out)
        os.write(result.getBytes)
        os.flush()

        os.close()


        /************************************************
         * copy assets
         ***********************************************/
         FileUtils.copyDirectory(sourceFile("assets"), outFile("assets"))


    }



    def setTemplateDir(dir:String) = {
        if (!new File(dir).exists())
            throw new Exception("Template dir %s not exists".format(dir))
        if (!new File(dir + "/index.html").exists())
            throw new Exception("Template dir %s has no index.html".format(dir))
        templateDir = dir
        this
    }

    def absPath(tail:String) =
        templateDir + tail

    def sourcePath(tail:String) = {
        templateDir + "/" + tail
    }

    def sourceFile(tail:String) = {
        new File(sourcePath(tail))
    }

    def outPath(tail:String) = {
        outDir + "/" + tail
    }

    def outFile(tail:String) = {
        new File(outPath(tail))
    }

}

object HtmlBuilder {

    def create(title:String, desc:String, docs:Seq[DocBase], outDir:String) = {
        val hb = new HtmlBuilder(docs)
        hb.title = title
        hb.desc = desc
        hb.outDir = outDir
        hb
    }



}
