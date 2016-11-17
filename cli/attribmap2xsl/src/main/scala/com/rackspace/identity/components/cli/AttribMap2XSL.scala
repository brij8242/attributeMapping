/***
 *   Copyright 2016 Rackspace US, Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.rackspace.identity.components.cli

import java.io.File
import java.io.PrintStream
import java.io.InputStream

import java.net.URI

import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource

import org.clapper.argot.ArgotConverters._
import org.clapper.argot.{ArgotParser, ArgotUsageException}

import com.martiansoftware.nailgun.NGContext

import net.sf.saxon.s9api.Serializer
import net.sf.saxon.s9api.Destination


import com.rackspace.identity.components.AttributeMapper

object AttribMap2XSL {
  val title = getClass.getPackage.getImplementationTitle
  val version = getClass.getPackage.getImplementationVersion

  def parseArgs(args: Array[String], base : String,
                in : InputStream, out : PrintStream, err : PrintStream) : Option[(Source, Destination)] = {

    val parser = new ArgotParser("attribmap2xsl", preUsage=Some(s"$title v$version"))

    val policy = parser.parameter[String]("policy",
                                          "Attribute mapping policy",
                                          false)

    val output = parser.parameter[String]("output",
                                          "Output file. If not specified, stdout will be used.",
                                          true)

    val help = parser.flag[Boolean] (List("h", "help"),
                                     "Display usage.")

    val printVersion = parser.flag[Boolean] (List("version"),
                                             "Display version.")


    def policySource : Source = new StreamSource(URLResolver.toAbsoluteSystemId(policy.value.get, base).toString)
    def destination : Destination = {
      if (output.value.isEmpty) {
        AttributeMapper.processor.newSerializer(out)
      } else {
        AttributeMapper.processor.newSerializer(new File(URLResolver.toAbsoluteSystemId(output.value.get, base)))
      }
    }
    try {
      parser.parse(args)

      if (help.value.getOrElse(false)) {
        parser.usage() // throws ArgotUsageException
      }

      if (printVersion.value.getOrElse(false)) {
        err.println(s"$title v$version")
        None
      } else {
        Some((policySource, destination))
      }
    } catch {
      case e: ArgotUsageException => err.println(e.message)
                                     None
      case iae : IllegalArgumentException => err.println(iae.getMessage)
                                             None
    }
  }

  private def getBaseFromWorkingDir (workingDir : String) : String = {
    (new File(workingDir)).toURI().toString
  }

  //
  // Local run...
  //
  def main(args : Array[String]) = {
    parseArgs (args, getBaseFromWorkingDir(System.getProperty("user.dir")),
               System.in, System.out, System.err) match {
      case Some((policy : Source,  dest : Destination)) =>
        AttributeMapper.generateXSL (policy,  dest)
      case None => /* Bad args, Ignore */
    }
  }

  //
  // Nailgun run...
  //
  def nailMain(context : NGContext) = {
    parseArgs (context.getArgs, getBaseFromWorkingDir(context.getWorkingDirectory),
               context.in, context.out, context.err) match {
      case Some((policy : Source,  dest : Destination)) =>
        AttributeMapper.generateXSL (policy,  dest)
      case None => /* Bad args, Ignore */
    }
  }
}