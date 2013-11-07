package com.ansvia.digapidoc.parser

import org.specs2.mutable.Specification

/**
 * Author: robin
 * Date: 10/10/13
 * Time: 8:18 PM
 *
 */
class DocFunctionalSpec extends Specification {

    val exampleData =
        """
          |/**
          |* GET channels/{ID-OR-NAME}/articles
          |*
          |* Untuk mendapatkan stream articles pada channel.
          |*
          |* + Symbols:
          |*
          |*      + {ID-OR-NAME} - Can be channel ID or channel name.
          |*
          |* + Parameters:
          |*
          |*      + sticky=`all` - about sticky.
          |*      + offset=`0` - starting offset.
          |*
          |*/
        """.stripMargin.trim

    val expected =
        """
          |GET channels/{ID-OR-NAME}/articles
          |
          |Untuk mendapatkan stream articles pada channel.
          |
          |+ Symbols:
          |
          |+ {ID-OR-NAME} - Can be channel ID or channel name.
          |
          |+ Parameters:
          |
          |+ sticky=`all` - about sticky.
          |+ offset=`0` - starting offset.
          |
          |
        """.stripMargin.trim

    val multiSymbol =
        """
          |GET channels/{ID-OR-NAME}/articles/response/{RESP-ID}/{REF-ID}
          |
          |Untuk mendapatkan stream articles pada channel.
          |
          |+ Symbols:
          |
          |+ {ID-OR-NAME} - Can be channel ID or channel name.
          |+ {RESP-ID} - Response ID.
          |
          |+ {REF-ID}
          |
          |+ Parameters:
          |
          |+ attribute
          |
          |
        """.stripMargin.trim


    val dataMultiParamLine =
        """
          |/**
          | * GET /user/[USER-ID]
          | *
          | * Get user data
          | *
          | *     + Parameters:
          | *
          | *         + state - state can be one of:
          | *                    * deleted - return only deleted user.
          | *                    * actived - return only actived user.
          | *                    * all -- return all.
          | *
          | */
          |def test(){
          |}
        """.stripMargin.trim


    "Doc" should {
        "normalize text and strip comment correctly" in {
            Doc.normalize(exampleData) must_== expected
        }
        "parse params all visible" in {
            val (name, desc, req, dv) = Doc.parseParam("+ sticky=`all` (required) - about sticky")
            name must_== "sticky"
            desc must_== "about sticky"
            req must_== "required"
            dv must_== "all"
        }
        "parse params partial visible #1" in {
            val (name, desc, req, dv) = Doc.parseParam("+ sticky=`all` - about sticky")
            name must_== "sticky"
            desc must_== "about sticky"
            req must_== "optional"
            dv must_== "all"
        }
        "parse params partial visible #2" in {
            val (name, desc, req, dv) = Doc.parseParam("+ sticky=`all`")
            name must_== "sticky"
            desc must_== ""
            req must_== "optional"
            dv must_== "all"
        }
        "parse params partial visible #3" in {
            val (name, desc, req, dv) = Doc.parseParam("+ sticky")
            name must_== "sticky"
            desc must_== ""
            req must_== "required"
            dv must_== ""
        }
        "get endpoint definition" in {
            val epd = Doc.getEndpointDef(expected)
            epd.method must_== "GET"
            epd.uriFormat must_== "channels/{ID-OR-NAME}/articles"
        }
        "get description" in {
            val desc = Doc.getDescription(expected)
            desc must_== "Untuk mendapatkan stream articles pada channel."
        }
        "get symbols" in {
            val symbs = Doc.getSymbols(expected)
            symbs(0).name must_== "ID-OR-NAME"
            symbs(0).desc must_== "Can be channel ID or channel name."
        }
        "get params" in {
            val params = Doc.getParams(expected)
            params.length must_== 2
            params(0).name must_== "sticky"
            params(0).desc must_== "about sticky."
            params(0).requirement must_== "optional"
            params(0).defaultValue must_== "all"
            params(1).name must_== "offset"
            params(1).desc must_== "starting offset."
            params(1).requirement must_== "optional"
            params(1).defaultValue must_== "0"
        }
        "get symbols multi" in {
            val symbs = Doc.getSymbols(multiSymbol)
            symbs(0).name must_== "ID-OR-NAME"
            symbs(0).desc must_== "Can be channel ID or channel name."
            symbs(1).name must_== "RESP-ID"
            symbs(1).desc must_== "Response ID."
            symbs(2).name must_== "REF-ID"
            symbs(2).desc must_== ""
        }
        "strip identation" in {
            val _data =
                """
                  |     /** hello
                  |      * world
                  |      */
                """.stripMargin
            val _expected =
                """
                  |/** hello
                  |* world
                  |*/
                """.stripMargin.trim
            Doc.stripIdent(_data).trim must_== _expected
        }
        "parse to DocGroup" in {
            val _data =
                """
                  |/** apidoc **
                  |* GROUP: User
                  |*/
                """.stripMargin.trim
            val dg = Doc.parse(_data)
            dg must beAnInstanceOf[DocGroup]
            dg.asInstanceOf[DocGroup].name must_== "User"
        }
        "parse to Doc" in {
            val doc = Doc.parse(exampleData).asInstanceOf[Doc]
            doc.endpoint.method must_== "GET"
            doc.endpoint.uriFormat must_== "channels/{ID-OR-NAME}/articles"
            doc.desc must_== "Untuk mendapatkan stream articles pada channel."
            doc.symbols.length must_== 1
            doc.params.length must_== 2
        }
        "parse to Doc complex" in {

            val data =
                """
                  |/**
                  |* GET channels/{ID-OR-NAME}/articles
                  |*
                  |* Untuk mendapatkan stream articles pada channel.
                  |* dan membuatnya semakin keren.
                  |*
                  |* + Symbols:
                  |*
                  |*      + {ID-OR-NAME} - Can be channel ID or channel name.
                  |*
                  |* + Parameters:
                  |*
                  |*      + sticky=`all` - about sticky.
                  |*      + offset=`0` - starting offset.
                  |*
                  |*/
                """.stripMargin.trim

            val doc = Doc.parse(data).asInstanceOf[Doc]
            doc.endpoint.method must_== "GET"
            doc.endpoint.uriFormat must_== "channels/{ID-OR-NAME}/articles"
            doc.desc must_== "Untuk mendapatkan stream articles pada channel.\n" +
                "dan membuatnya semakin keren."
            doc.symbols.length must_== 1
            doc.params.length must_== 2
        }
        "parse multi param correctly" in {
            val doc = Doc.parse(dataMultiParamLine).asInstanceOf[Doc]
            doc.endpoint.method must_== "GET"
            doc.endpoint.uriFormat must_== "/user/[USER-ID]"
            doc.params(0).desc must_==
                """
                  |state can be one of:
                  |* deleted - return only deleted user.
                  |* actived - return only actived user.
                  |* all -- return all.
                """.stripMargin.trim
        }
    }


}
