package com.ansvia.digapidoc.parser

/**
 * Author: robin
 * Date: 10/11/13
 * Time: 6:41 PM
 *
 */
class DigapiException(msg:String) extends Exception(msg)

case class InvalidParameter(msg:String) extends DigapiException(msg)
case class NotExists(msg:String) extends DigapiException(msg)

