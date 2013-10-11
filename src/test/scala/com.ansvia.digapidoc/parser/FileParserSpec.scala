package com.ansvia.digapidoc.parser

import org.specs2.mutable.Specification
import java.io.ByteArrayInputStream

/**
 * Author: robin
 * Date: 10/11/13
 * Time: 12:38 PM
 *
 */
class FileParserSpec extends Specification {

    val data =
        """
          | def codeX(){
          |      /* here is nothing spesial */
          | }
          |
          | /**
          | * GET channels/{ID-OR-NAME}/articles
          | *
          | * Untuk mendapatkan stream articles pada channel.
          | *
          | * + Symbols:
          | *
          | *      + {ID-OR-NAME} - Can be channel ID or channel name.
          | *
          | * + Parameters:
          | *
          | *      + sticky=`all` - about sticky.
          | *      + offset=`0` - starting offset.
          | *      %{offsetLimit}
          | */
          | def code(){
          |     println("code here")
          | }
          |
          | /**
          |  * PUT user/{ID-OR-NAME}
          |  *
          |  * Untuk meng-update informasi user
          |  * agar bisa keren dan mantap.
          |  */
        """.stripMargin


    "FileParser" should {
//        "parse file and producing sequence of doc" in {
//            val rv = FileParser.parse(new ByteArrayInputStream(data.getBytes))
//            rv.length must_== 2
//            rv(0).endpoint.method must_== "GET"
//            rv(0).endpoint.uriFormat must_== "channels/{ID-OR-NAME}/articles"
//            rv(1).endpoint.method must_== "PUT"
//            rv(1).endpoint.uriFormat must_== "user/{ID-OR-NAME}"
//            rv(1).desc must_== "Untuk meng-update informasi user\n" +
//                "agar bisa keren dan mantap."
//        }
        "scan source dir" in {
            val docs = FileParser.scan("test/source")
            docs.length must_== 3
            docs(0).endpoint.method must_== "GET"
            docs(0).endpoint.uriFormat must_== "/user/{USER-ID-OR-NAME}"
            docs(0).desc must_== "Mendapatkan informasi user."
            docs(0).params.length must_== 1
            docs(0).params(0).name must_== "ref"
            docs(0).params(0).desc must_== "reference id."
        }
    }

}
