package com.ansvia.digapidoc.parser

import java.io.{InputStreamReader, BufferedReader, ByteArrayInputStream}
import scala.xml.NodeSeq
import eu.henkelmann.actuarius.ActuariusTransformer

/**
 * Author: robin
 * Date: 10/10/13
 * Time: 8:17 PM
 *
 */

sealed abstract class DocBase {
    def toHtmlString:String
}

case class DocGroup(name:String, file:String) extends DocBase {

    var docs = Seq.empty[Doc]

    def toHtmlString:String = {
        val id = name.replaceAll("""\W+""","-").trim
        <div class="page-header">
            <h1 id={id}>
                {name}
            </h1>
        </div>.toString()
    }
}

case class Doc(endpoint:DocEndpointDef, desc:String, symbols:Seq[DocSymbol], params:Seq[DocParam], tags:Seq[String]) extends DocBase {


    override def toString = {
        "%s %s".format(endpoint.method, endpoint.uriFormat)
    }

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
            <div class="panel-body">
                <div>{Doc.markdownToNs(desc)}</div>
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
                                    <td>{Doc.markdownToNs(param.desc.replace("\n", "\n\n"))}</td></tr>
                            }.foldLeft(NodeSeq.Empty)(_ ++ _)
                            }
                        </table>
                }else{
                    NodeSeq.Empty
                }
                }
            </div>
        </div>.toString()
    }


}

case class DocEndpointDef(method:String, uriFormat:String)
case class DocSymbol(name:String, desc:String)
case class DocParam(name:String, desc:String, requirement:String, defaultValue:String)

trait SymbolMapper {
    def map(key:String):String
}

object NopSymbolMapper extends SymbolMapper {
    def map(key: String) = key
}


/**
 * Documentation class representation.
 */
object Doc {

    var symbolMapper:SymbolMapper = NopSymbolMapper

    val markdownTransform = new ActuariusTransformer()

    def markdownToNs(text:String):NodeSeq = {
        scala.xml.XML.loadString("<div>" + markdownTransform(text) + "</div>")
    }

    def normalize(text:String) = {
        val newText = StringBuilder.newBuilder
        val fixIdentText = stripIdent(text)

        var offsetA = 0
        var offsetL = 0
        var foundChar = false

        for ((t, i) <- fixIdentText.zipWithIndex){
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

    private val groupExtractorRe = """(?s).+?GROUP\: (.*?)\n.+""".r
    private val symbolKeyNameExtractorRe = """\{[a-zA-Z0-9_\-]*?\}""".r

    def parse(text:String, fileName:String="-",
              includeSymbols:Map[String,String]=Map.empty[String,String]):DocBase = {

        var normText = normalize(text)

        if (isGroup(text)){

            text match {
                case groupExtractorRe(name) =>
                    DocGroup(name, fileName)
                case x =>
                    throw new ParserException("Cannot extract group name")
            }

        }else{
            import com.ansvia.commons.StringTemplate._

            for ((k, v) <- includeSymbols){
                normText = normText.render(Map(("include=" + k) -> v))
            }

            val endpointDef = getEndpointDef(normText)
            val desc = getDescription(normText)

            /**
             * Parse symbols
             */
            val symbols =
                try {
                    getSymbols(normText)
                }
                catch {
                    case e:ParserException if e.getMessage == "No `+ Symbols` sign" =>
                        Seq.empty[DocSymbol]
                }
            val symbolsByUri = {
                // get symbol from uri
                val ss = symbolKeyNameExtractorRe.findAllIn(endpointDef.uriFormat)
                ss.map { s =>
                    val sNoCurly = stripCurlyBraces(s)
                    DocSymbol(sNoCurly, symbolMapper.map(sNoCurly))
                }.toSeq.filter(s => !symbols.map(_.name).contains(s.name))
            }

            /**
             * Parse parameters.
             */
            val params =
                try {
                    getParams(normText + "\n")
                }
                catch {
                    case e:ParserException if e.getMessage == "No `+ Parameters` sign" =>
                        Seq.empty[DocParam]
                    case e:ParserException if e.getMessage == "`+ Parameters` sign defined but no any param defined" =>
                        Seq.empty[DocParam]
                }

            /**
             * Parse tags.
             */
            val tags = try {

                getTags(normText + "\n")

            }catch{
                case e:ParserException if e.getMessage == "No `+ Tags` sign" || e.getMessage == "`+ Tags` sign defined but no any tags defined" =>
                    Nil
            }

            Doc(endpointDef, desc, symbols ++ symbolsByUri, params, tags)
        }


    }

    private val curlyBracesStriperRe = """^\{|\}$""".r
    private def stripCurlyBraces(text:String) = {
        curlyBracesStriperRe.replaceAllIn(text, "")
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

    val paramExtractorRe = """(?s)\+ ([a-zA-Z0-9_\-]+)(\=`(.*?)`)? ?(\((required|optional)\))?( ?\- (.*))?""".r

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


    private val _keywords = Seq(
    "Parameters", "Tags"
    )
    private def getKeywords(text:String):Seq[(String, Int)] = {
        _keywords.flatMap { k =>
            val i = text.indexOf("+ " + k)
            if (i > -1){
                Some( (text.substring(i + 2).split(" ").head.trim, i) )
            }else
                None
        }
    }


    def getParams(text:String):Seq[DocParam] = {
        val startingIndex = text.indexOf("+ Parameters:\n")

        if (startingIndex == -1)
            throw new ParserException("No `+ Parameters` sign")

        if (text.length < startingIndex + 15)
            throw new ParserException("`+ Parameters` sign defined but no any param defined")

        val startingText = text.substring(startingIndex + 15)
        var sb = StringBuilder.newBuilder
        var firstIter = true
        var readyCapture = false
        var captureDone = false
        var allDone = false
        var rv = Seq.newBuilder[DocParam]
        val eot = {
            // get next keyword if any

            val ks = getKeywords(startingText)

            if (ks.length > 0)
                ks(0)._2 - 1
            else
                startingText.length
        }

        for ( (t, i) <- startingText.zipWithIndex if !allDone ){
            try {
                if (firstIter){
                    if (t != '+')
                        throw Ignore

                    firstIter = false
                    readyCapture = true
                }

                if (i+1 < eot){
                    if (readyCapture && t == '\n'){

                        if (startingText(i+1) == '\n' || startingText(i+1) == '+'){
                            captureDone = true
                            readyCapture = false
                        }else{
                            sb += t
                        }
                    }else if(readyCapture && i == eot){
                        sb += t
                        captureDone = true
                        readyCapture = false
                    }
                    else {
                        sb += t
                    }
                }else{
                    sb += t
                    captureDone = true
                    readyCapture = false
                    allDone = true
                }

                val chunk = sb.mkString.trim

                if (captureDone && chunk.length > 1){

                    val (name, desc, requirement, defaultValue) =
                        parseParam(chunk)

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
    

    def getTags(text:String):Seq[String] = {
        val startingIndex = text.indexOf("+ Tags:")

        if (startingIndex == -1)
            throw new ParserException("No `+ Tags` sign")

        if (text.length < startingIndex + 9)
            throw new ParserException("`+ Tags` sign defined but no any tags defined")

        val startingText = text.substring(startingIndex + 8)

        val tagsString = startingText.substring(0, startingText.indexOf('\n'))
        val s = tagsString.split(',').map(_.trim).filter(_.length > 0)
        s.toSeq
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