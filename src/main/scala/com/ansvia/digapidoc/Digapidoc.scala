package com.ansvia.digapidoc

import org.streum.configrity.Configuration
import java.io.File
import com.ansvia.commons.logging.Slf4jLogger
import com.ansvia.digapidoc.parser._
import com.ansvia.digapidoc.parser.InvalidParameter
import scala.io.Source


object Digapidoc extends Slf4jLogger {


    def main(args:Array[String]){

        try {

            if (args.length < 1)
                throw InvalidParameter("No config file")

            val configFile = new File(args(0))

            if (!configFile.exists())
                throw InvalidParameter("Config file not found: " + configFile.getAbsolutePath)

            val conf = Configuration.load(configFile.getAbsolutePath)

            Doc.symbolMapper = new SymbolMapper {
                def map(key: String) = {
                    conf("symbols." + key, "-")
                }
            }

            val includeSymbols = conf.detach("include-text").data.map(x => x._1 -> x._2.replace("\\n", "\n"))
            val docs = FileParser.scan(conf[String]("source-dir"), includeSymbols)

            val incDir = conf[String]("include-dir")
            val outDir = conf[String]("output-dir")

            val desc = {
                val _desc = conf[String]("desc")
                if (_desc.startsWith("file:"))
                    Source.fromFile(incDir + "/" + _desc.substring(5)).mkString
                else
                    _desc
            }
            val hb = HtmlBuilder.create(conf[String]("title"), desc,
                docs, outDir)

            hb.setTemplateDir(conf[String]("template-dir"))
                .excludeTags(conf[String]("exclude.tags", "").split(',').map(_.trim).filter(_.length > 0).toSeq)
                .includeTags(conf[String]("include.tags", "").split(',').map(_.trim).filter(_.length > 0).toSeq)
                .generate()

            info("html generated: " + outDir + "/index.html")

        }
        catch {
            case e:InvalidParameter =>
                error(e.getMessage)
                println("Usage: java -jar digapidoc.jar [CONFIG-FILE]")
            case e:DigapiException =>
                error(e.getMessage)
                sys.exit(2)
        }

    }

}
