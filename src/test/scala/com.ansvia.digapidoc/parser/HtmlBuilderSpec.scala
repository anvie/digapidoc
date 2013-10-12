package com.ansvia.digapidoc.parser

import org.specs2.mutable.Specification
import java.io.File

/**
 * Author: robin
 * Date: 10/11/13
 * Time: 2:38 PM
 *
 */
class HtmlBuilderSpec extends Specification {
    args(sequential = true)

    "HtmlBuilder" should {
        "generate html" in {

            val docs = FileParser.scan("test/source/src", Map.empty[String,String])
            val hb = HtmlBuilder.create("API keren", "Ini dokumentasi dari API keren",
                docs, "test/out")

            hb.setTemplateDir("web-templates/twbs")
                .generate()

            val outIndex = hb.outDir + "/index.html"

            new File(outIndex).exists() must beTrue
        }
    }

}
