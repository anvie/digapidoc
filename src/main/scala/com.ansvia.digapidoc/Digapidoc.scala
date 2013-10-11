package com.ansvia.digapidoc

import org.streum.configrity.Configuration
import java.io.File
import com.ansvia.commons.logging.Slf4jLogger
import com.ansvia.digapidoc.parser.{DigapiException, InvalidParameter, FileParser, HtmlBuilder}

object Digapidoc extends Slf4jLogger {


    def main(args:Array[String]){

        try {

            if (args.length < 1)
                throw InvalidParameter("No config file")

            val configFile = new File(args(0))

            if (!configFile.exists())
                throw InvalidParameter("Config file not found: " + configFile.getAbsolutePath)

            val conf = Configuration.load(configFile.getAbsolutePath)

            val docs = FileParser.scan(conf[String]("source-dir"))
            val hb = HtmlBuilder.create(conf[String]("title"), conf[String]("desc"),
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
