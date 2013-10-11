package com.ansvia.digapidoc.parser

import java.io.{InputStreamReader, BufferedReader, ByteArrayInputStream}
import scala.xml.NodeSeq

/**
 * Author: robin
 * Date: 10/10/13
 * Time: 8:17 PM
 *
 */

sealed abstract class DocBase {
    def toHtmlString:String
}

case class DocGroup(name:String) extends DocBase {

    def toHtmlString:String = {
        val id = name.replaceAll("""\W+""","-").trim
        <div class="page-header">
            <h1 id={id}>
                {name}
            </h1>
        </div>.toString()
    }
}

case class Doc(endpoint:DocEndpointDef, desc:String, symbols:Seq[DocSymbol], params:Seq[DocParam]) extends DocBase {

    def toHtmlString:String = {
        val panelClass = {
            endpoint.method match {
                case "GET" => "panel panel-info endpoint-section"
                case "DELETE" => "panel panel-danger endpoint-section"
                case "POST" => "panel panel-success endpoint-section"
                case "PUT" => "panel panel-warning endpoint-section"
            }
        }
        <div class={panelClass}>
            <div class="panel-heading">
                <h4>
                    {
                    endpoint.method match {
                        case "GET" => <span class="label label-primary">GET</span>
                        case "DELETE" => <span class="label label-danger">DELETE</span>
                        case "POST" => <span class="label label-success">POST</span>
                        case "PUT" => <span class="label label-warning">PUT</span>
                    }
                    }
                    <span><code>{endpoint.uriFormat}</code></span>
                </h4>
            </div>
            <p class="lead"><small>{desc}</small></p>
            {
            if (symbols.length > 0){
                <div><strong>Symbols:</strong></div>
                <table class="table table-striped">
                    {
                    symbols.map { sym =>
                        <tr><td>{sym.name}</td><td>{sym.desc}</td></tr>
                    }.foldLeft(NodeSeq.Empty)(_ ++ _)
                    }
                </table>
            }else{
                NodeSeq.Empty
            }
            }
            {
            if (params.length > 0){
                {
                    if (symbols.length > 0){
                        <div style="margin-top: 10px; height: 10px;"></div>
                    }else
                        NodeSeq.Empty
                } ++
                <div><strong>Parameters:</strong></div>
                <table class="table table-striped">
                    {
                    params.map { param =>
                        <tr><td style="width: 150px;"><code>{param.name}</code></td>
                            <td style="width: 200px;">
                                {
                                param.requirement match {
                                    case "required" =>
                                        <span class="label label-danger">{param.requirement}</span>
                                    case "optional" =>
                                        <span class="label label-info">{param.requirement}</span>
                                }
                                }

                                {
                                if (param.defaultValue.length > 0){
                                    <span> default: {param.defaultValue}</span>
                                }else{
                                    NodeSeq.Empty
                                }
                                }
                            </td>
                            <td>{param.desc}</td></tr>
                    }.foldLeft(NodeSeq.Empty)(_ ++ _)
                    }
                </table>
            }else{
                NodeSeq.Empty
            }
            }
        </div>.toString()
    }


}

case class DocEndpointDef(method:String, uriFormat:String)
case class DocSymbol(name:String, desc:String)
case class DocParam(name:String, desc:String, requirement:String, defaultValue:String)

/**
 * Documentation class representation.
 */
object Doc {

    def normalize(text:String) = {
        val newText = StringBuilder.newBuilder
        val fixIdentText = stripIdent(text)

        var offsetA = 0
        var offsetL = 0
        var foundChar = false

        for ( (t, i) <- fixIdentText.zipWithIndex){
            try {
                if (i == 0) {
                    if (!text.startsWith("/*"))
                        throw new ParserException("Not started with /*")
                    throw Ignore
                }

                if ((offsetL == 0 || offsetL == 1) && t == '*'){
                    throw Ignore
                }

                if (t == '\n'){
                    offsetL = 0
                    foundChar = false
                }

                offsetL = offsetL + 1
                offsetA = offsetA + 1

                if (offsetL > 0 && !foundChar){
                    if (t == ' ')
                        throw Ignore
                    else if (t != '\n')
                        foundChar = true
                }

                newText += t
            }
            catch {
                case Ignore =>
            }
        }

        val rvStr = newText.mkString.trim

        rvStr.substring(0, rvStr.length - 1).trim
    }

    private val groupExtractorRe = """(?s).+?GROUP\: (.*)\n.+""".r

    def parse(text:String):DocBase = {
        val normText = normalize(text)

        if (isGroup(text)){

            text match {
                case groupExtractorRe(name) =>
                    DocGroup(name)
                case x =>
                    throw new ParserException("Cannot extract group name")
            }

        }else{
            val endpointDef = getEndpointDef(normText)
            val desc = getDescription(normText)
            val symbols =
                try {
                    getSymbols(normText)
                }
                catch {
                    case e:ParserException if e.getMessage == "No `+ Symbols` sign" =>
                        Seq.empty[DocSymbol]
                }
            val params =
                try {
                    getParams(normText + "\n")
                }
                catch {
                    case e:ParserException if e.getMessage == "No `+ Parameters` sign" =>
                        Seq.empty[DocParam]
                }

            Doc(endpointDef, desc, symbols, params)
        }


    }

    def isGroup(text:String) = {
        text.contains("GROUP: ")
    }

    
    private val supportedMethods = Seq(
        "POST", "PUT", "GET", "DELETE"
    )
    
    def getEndpointDef(text:String):DocEndpointDef = {
        if (!supportedMethods.exists(text.startsWith))
            throw new ParserException("No method definition specified")
        
        val firstSpace = text.indexOf(' ')
        val method = text.substring(0, firstSpace)
        val newLine = text.indexOf("\n")
        val uri = text.substring(firstSpace + 1, newLine)
        
        DocEndpointDef(method, uri)
    }
    
    
    def getDescription(text:String) = {
        import scala.util.control.Breaks._

        val sb = StringBuilder.newBuilder
        
        val firstNl = text.indexOf('\n')
        var firstItr = true
        var newLineAppear = 0

        breakable {

            for ((t, i) <- text.substring(firstNl).zipWithIndex){
                try {

                    if (firstItr){
                        if (t == ' ' || t == '\n')
                            throw Ignore

                        if (t == '+')
                            throw new ParserException("No description?")
                    }

                    firstItr = false

                    if (t == '\n'){
                        newLineAppear = newLineAppear + 1
                    }else{
                        newLineAppear = 0
                    }

                    if (newLineAppear == 2){
                        break()
                    }

                    sb += t
                }
                catch {
                    case Ignore =>
                }
            }
        }

        sb.mkString.trim
    }

    private val symbolExtractorRe = """\{(.*?)\}( \- (.*?))?""".r

    def getSymbols(text:String):Seq[DocSymbol] = {
        val startingIndex = text.indexOf("+ Symbols:")
        if (startingIndex == -1)
            throw new ParserException("No `+ Symbols` sign")

        var firstItr = true
        var captureMode = false
        var captureDone = false
        var newLineCount = 0

        val startingText = text.substring(startingIndex + 10)
        var sb = StringBuilder.newBuilder
        var rv = Seq.newBuilder[DocSymbol]

        try {
            for ((t, i) <- startingText.zipWithIndex) {
                try {
                    if (firstItr) {
                        if (t == ' ' || t == '\n')
                            throw Ignore

                        if (t != '+')
                            throw new ParserException("No + sign for symbol?")

                        firstItr = false
                        throw Ignore
                    }

                    if (!captureMode) {
                        if (t == ' ')
                            throw Ignore

                        if (t != '{')
                            throw Done
                        //                        throw new ParserException("symbol name must wrapped inside curly braches eg. {ID-OR-NAME}")
                    }

                    if (!captureMode) {
                        sb.clear()
                        captureMode = true
                    }


                    if (captureMode) {
                        if (t == '\n')
                            newLineCount = newLineCount + 1

                        if (newLineCount == 1 && startingText.substring(i + 1).startsWith("+")){
                            // save this and start new iter
                            captureMode = false
                            captureDone = true
                        }else{
                            if (newLineCount == 2) {
                                captureMode = false
                                captureDone = true
                            }
                            sb += t
                        }
                    }

                    if (captureDone) {
                        newLineCount = 0
                        captureDone = false
                        firstItr = true

                        val oneLine = sb.mkString.trim
//                        println("oneLine: " + oneLine)
                        oneLine match {
                            case symbolExtractorRe(keyName, _, null) => {
                                rv += DocSymbol(keyName, "")
                            }
                            case symbolExtractorRe(keyName, _, keyDesc) => {
                                rv += DocSymbol(keyName, keyDesc)
                            }
                        }
                    }
                    //
                    //                val newLine = startingText.substring(i).indexOf("\n\n")
                    //
                    //                if (newLine == -1)
                    //                    throw new ParserException("No new line after symbol definition")
                    //
                    //                val oneLine = startingText.substring(i, i + newLine)
                    //                oneLine match {
                    //                    case symbolRe(keyName, keyDesc) => {
                    //                        rv += DocSymbol(keyName, keyDesc)
                    //                    }
                    //                }
                }
                catch {
                    case Ignore =>
                }
            }
        }
        catch {
            case Done =>
        }

        rv.result()
    }

    val paramExtractorRe = """\+ ([a-zA-Z0-9_\-]+)(\=`(.*?)`)? ?(\((required|optional)\))?( ?\- (.*))?""".r

    def parseParam(paramText:String) = {
        if (!paramText.startsWith("+"))
            throw new ParserException("No `+` sign for param?")

        paramText match {
            case paramExtractorRe(paramKeyName, _, null, _, null, _, null) =>
                (paramKeyName, "", "required", "")
            case paramExtractorRe(paramKeyName, _, null, _, null, _, desc) =>
                (paramKeyName, desc, "required", "")
            case paramExtractorRe(paramKeyName, _, defaultValue, _, null, _, null) =>
                (paramKeyName, "", "optional", defaultValue)
            case paramExtractorRe(paramKeyName, _, defaultValue, _, null, _, desc) =>
                (paramKeyName, desc, "optional", defaultValue)
            case paramExtractorRe(paramKeyName, _, defaultValue, _, requirement, _, desc) =>
                (paramKeyName, desc, requirement, defaultValue)
        }
    }


    def getParams(text:String):Seq[DocParam] = {
        val startingIndex = text.indexOf("+ Parameters:\n")

        if (startingIndex == -1)
            throw new ParserException("No `+ Parameters` sign")

        val startingText = text.substring(startingIndex + 15)
        var sb = StringBuilder.newBuilder
        var firstIter = true
        var readyCapture = false
        var captureDone = false
        var rv = Seq.newBuilder[DocParam]

        for ( (t, i) <- startingText.zipWithIndex ){
            try {
                if (firstIter){
                    if (t != '+')
                        throw Ignore

                    firstIter = false
                    readyCapture = true
                }


                if (readyCapture && t == '\n'){
                    captureDone = true
                    readyCapture = false
                }
                else {
                    sb += t
                }

                if (captureDone){

                    val (name, desc, requirement, defaultValue) =
                        parseParam(sb.mkString.trim)

                    rv += DocParam(name, desc, requirement, defaultValue)

                    sb.clear()
                    firstIter = true
                    captureDone = false
                }

            }
            catch {
                case Ignore =>
            }
        }

        rv.result()
    }
    
    
    def validate(text:String) = {
        if (!isHeaderValid(text))
            throw new ParserException("Invalid header tag")
    }
    
    private lazy val headerValidRe =
        """(?s)(/\*\* apidoc \*\*|/\*\*\n?[\n\s\*]+(GET|PUT|POST|DELETE)).+""".r
    
    def isHeaderValid(text:String) = {
        if (headerValidRe.pattern.matcher(stripIdent(text)).matches())
            true
        else
            false
    }
    
    def stripIdent(text:String):String = {
        val sb = StringBuilder.newBuilder

        val bais = new ByteArrayInputStream(text.getBytes)
        val isr = new InputStreamReader(bais)
        val bis = new BufferedReader(isr)

        var line = bis.readLine()
        while(line != null){
            line = line.replaceAll("""^\s+""", "")
            if (line.length > 0)
                line = line + "\n"
            sb.append(line)
            line = bis.readLine()
        }

        bis.close()
        isr.close()
        bais.close()

        sb.result()
    }


}

class ParserException(msg:String) extends Exception(msg)
object Ignore extends ParserException("ignored")
object Done extends ParserException("done")