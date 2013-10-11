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

    "HtmlBuilder" should {
        "build generated html" in {
            val docs = FileParser.scan("test/source/src")
            val hb = HtmlBuilder.create("API keren", "Ini dokumentasi dari API keren",
                docs, "test/out")

            hb.setTemplateDir("web-templates/twbs")
                .generate()

            val outIndex = hb.outDir + "/index.html"

            new File(outIndex).exists() must beTrue
        }
    }

}
