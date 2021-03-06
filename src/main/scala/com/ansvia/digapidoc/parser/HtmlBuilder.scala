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

            doc match {
                case dg:DocGroup =>
                    apiNavsStr ++= <li><a href={"#" + dg.name}>{dg.name}</a></li>.toString

                    docStr ++= dg.toHtmlString

                    for (docItem <- dg.docs.sortBy(_.endpoint.uriFormat.length)){
                        if (this.excludedTags.length > 0){
                            if (!docItem.tags.containsSlice(this.excludedTags)){
                                if (this.includedTags.length > 0){
                                    if (docItem.tags.containsSlice(this.includedTags)){
                                        docStr ++= docItem.toHtmlString
                                    }
                                }else
                                    docStr ++= docItem.toHtmlString
                            }
                        }else if (this.includedTags.length > 0){
                            if (docItem.tags.containsSlice(this.includedTags)){
                                docStr ++= docItem.toHtmlString
                            }
                        }else{
                            docStr ++= docItem.toHtmlString
                        }

                    }

                case d:Doc =>
            }
        }

        val unknownDocs = docs.filter(_.isInstanceOf[Doc])

        if (unknownDocs.length > 0){
            apiNavsStr ++= <li><a href="#Unknown-Group">Unknown group</a></li>.toString
            docStr ++= DocGroup("Unknown Group", "??").toHtmlString
            unknownDocs.foreach { uDoc =>
                docStr ++= uDoc.toHtmlString
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

    private var excludedTags:Seq[String] = Nil
    private var includedTags:Seq[String] = Nil

    def excludeTags(tags:Seq[String]) = {
        this.excludedTags = tags
        this
    }

    def includeTags(tags:Seq[String]) = {
        this.includedTags = tags
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
