package com.ansvia.digapidoc

import org.streum.configrity.Configuration
import java.io.File
import com.ansvia.commons.logging.Slf4jLogger
import com.ansvia.digapidoc.parser._
import com.ansvia.digapidoc.parser.InvalidParameter

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

            import scala.io.Source

            val incDir = conf[String]("include-dir")

            val desc = {
                val _desc = conf[String]("desc")
                if (_desc.startsWith("file:"))
                    Source.fromFile(incDir + "/" + _desc.substring(5)).mkString
                else
                    _desc
            }
            val hb = HtmlBuilder.create(conf[String]("title"), desc,
                docs, conf[String]("output-dir"))

            hb.setTemplateDir(conf[String]("template-dir"))
                .generate()


        }
        catch {
            case e:DigapiException =>
                error(e.getMessage)
                sys.exit(2)
        }

    }

}
