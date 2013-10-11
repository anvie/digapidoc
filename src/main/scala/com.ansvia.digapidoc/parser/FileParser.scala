package com.ansvia.digapidoc.parser

import java.io._

/**
 * Author: robin
 * Date: 10/11/13
 * Time: 12:39 PM
 *
 */
object FileParser {

    def parse(file:String):Seq[Doc] = parse(new File(file))

    def parse(file:File):Seq[Doc] = {
        parse(new FileInputStream(file))
    }

    def parse(fileIs:InputStream):Seq[Doc] = {
        val isr = new InputStreamReader(fileIs)
        val bfr = new BufferedReader(isr)

        var docs = Seq.newBuilder[Doc]
        var line = ""
        do {
            line = bfr.readLine()

            if (line != null){
                if (Doc.stripIdent(line).startsWith("/**")){
                    line = line + "\n" + bfr.readLine()
                }
                if (Doc.isHeaderValid(line)){
                    val sb = StringBuilder.newBuilder
                    line = line + "\n"
                    do {
                        sb.append(Doc.stripIdent(line))
                        line = bfr.readLine() + "\n"
                    }while(line != null && !line.contains("*/"))
                    val textRaw = sb.result().trim + "\n*/"
                    docs += Doc.parse(textRaw)
                }
            }

        }while(line != null)

        bfr.close()
        isr.close()

        docs.result()
    }

    def scan(dir:String):Seq[Doc] = scan(new File(dir))

    def scan(dir:File):Seq[Doc] = {
        var rv = Seq.newBuilder[Doc]

        dir.listFiles().foreach { f =>

            if (f.isDirectory){
                rv ++= scan(f)

            }else{

                rv ++= parse(f)

            }
        }

        rv.result()
    }
}
