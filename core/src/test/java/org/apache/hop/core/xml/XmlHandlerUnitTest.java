/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.core.xml;

import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopXmlException;
import org.apache.hop.junit.rules.RestoreHopEnvironment;
import org.junit.ClassRule;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.*;

public class XmlHandlerUnitTest {
  @ClassRule public static RestoreHopEnvironment env = new RestoreHopEnvironment();
  /** @see <a href="https://en.wikipedia.org/wiki/Billion_laughs" /> */
  private static final String MALICIOUS_XML =
      "<?xml version=\"1.0\"?>\n"
          + "<!DOCTYPE lolz [\n"
          + " <!ENTITY lol \"lol\">\n"
          + " <!ELEMENT lolz (#PCDATA)>\n"
          + " <!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">\n"
          + " <!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">\n"
          + " <!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">\n"
          + " <!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">\n"
          + " <!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">\n"
          + " <!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">\n"
          + " <!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">\n"
          + " <!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">\n"
          + " <!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">\n"
          + "]>\n"
          + "<lolz>&lol9;</lolz>";

  private static final String cr = Const.CR;

  @Test
  public void openTagWithNotNull() {
    assertEquals("<qwerty>", XmlHandler.openTag("qwerty"));
  }

  @Test
  public void openTagWithNull() {
    assertEquals("<null>", XmlHandler.openTag(null));
  }

  @Test
  public void openTagWithExternalBuilder() {
    StringBuilder builder = new StringBuilder("qwe");
    XmlHandler.openTag(builder, "rty");
    assertEquals("qwe<rty>", builder.toString());
  }

  @Test
  public void closeTagWithNotNull() {
    assertEquals("</qwerty>", XmlHandler.closeTag("qwerty"));
  }

  @Test
  public void closeTagWithNull() {
    assertEquals("</null>", XmlHandler.closeTag(null));
  }

  @Test
  public void closeTagWithExternalBuilder() {
    StringBuilder builder = new StringBuilder("qwe");
    XmlHandler.closeTag(builder, "rty");
    assertEquals("qwe</rty>", builder.toString());
  }

  @Test
  public void buildCdataWithNotNull() {
    assertEquals("<![CDATA[qwerty]]>", XmlHandler.buildCDATA("qwerty"));
  }

  @Test
  public void buildCdataWithNull() {
    assertEquals("<![CDATA[]]>", XmlHandler.buildCDATA(null));
  }

  @Test
  public void buildCdataWithExternalBuilder() {
    StringBuilder builder = new StringBuilder("qwe");
    XmlHandler.buildCDATA(builder, "rty");
    assertEquals("qwe<![CDATA[rty]]>", builder.toString());
  }

  @Test
  public void timestamp2stringTest() {
    String actual = XmlHandler.timestamp2string(null);
    assertNull(actual);
  }

  @Test
  public void date2stringTest() {
    String actual = XmlHandler.date2string(null);
    assertNull(actual);
  }

  @Test
  public void addTagValueBigDecimal() {
    BigDecimal input = new BigDecimal("1234567890123456789.01");
    assertEquals(
        "<bigdec>1234567890123456789.01</bigdec>" + cr, XmlHandler.addTagValue("bigdec", input));
    assertEquals(
        "<bigdec>1234567890123456789.01</bigdec>" + cr,
        XmlHandler.addTagValue("bigdec", input, true));
    assertEquals(
        "<bigdec>1234567890123456789.01</bigdec>", XmlHandler.addTagValue("bigdec", input, false));
  }

  @Test
  public void addTagValueBoolean() {
    assertEquals("<abool>Y</abool>" + cr, XmlHandler.addTagValue("abool", true));
    assertEquals("<abool>Y</abool>" + cr, XmlHandler.addTagValue("abool", true, true));
    assertEquals("<abool>Y</abool>", XmlHandler.addTagValue("abool", true, false));
    assertEquals("<abool>N</abool>" + cr, XmlHandler.addTagValue("abool", false));
    assertEquals("<abool>N</abool>" + cr, XmlHandler.addTagValue("abool", false, true));
    assertEquals("<abool>N</abool>", XmlHandler.addTagValue("abool", false, false));
  }

  @Test
  public void addTagValueDate() {
    String result = "2014/12/29 15:59:45.789";
    Calendar aDate = new GregorianCalendar();
    aDate.set(2014, (12 - 1), 29, 15, 59, 45);
    aDate.set(Calendar.MILLISECOND, 789);

    assertEquals(
        "<adate>" + result + "</adate>" + cr, XmlHandler.addTagValue("adate", aDate.getTime()));
    assertEquals(
        "<adate>" + result + "</adate>" + cr,
        XmlHandler.addTagValue("adate", aDate.getTime(), true));
    assertEquals(
        "<adate>" + result + "</adate>", XmlHandler.addTagValue("adate", aDate.getTime(), false));
  }

  @Test
  public void addTagValueLong() {
    long input = 123;
    assertEquals("<along>123</along>" + cr, XmlHandler.addTagValue("along", input));
    assertEquals("<along>123</along>" + cr, XmlHandler.addTagValue("along", input, true));
    assertEquals("<along>123</along>", XmlHandler.addTagValue("along", input, false));

    assertEquals(
        "<along>" + String.valueOf(Long.MAX_VALUE) + "</along>",
        XmlHandler.addTagValue("along", Long.MAX_VALUE, false));
    assertEquals(
        "<along>" + String.valueOf(Long.MIN_VALUE) + "</along>",
        XmlHandler.addTagValue("along", Long.MIN_VALUE, false));
  }

  @Test
  public void addTagValueInt() {
    int input = 456;
    assertEquals("<anint>456</anint>" + cr, XmlHandler.addTagValue("anint", input));
    assertEquals("<anint>456</anint>" + cr, XmlHandler.addTagValue("anint", input, true));
    assertEquals("<anint>456</anint>", XmlHandler.addTagValue("anint", input, false));

    assertEquals(
        "<anint>" + String.valueOf(Integer.MAX_VALUE) + "</anint>",
        XmlHandler.addTagValue("anint", Integer.MAX_VALUE, false));
    assertEquals(
        "<anint>" + String.valueOf(Integer.MIN_VALUE) + "</anint>",
        XmlHandler.addTagValue("anint", Integer.MIN_VALUE, false));
  }

  @Test
  public void addTagValueDouble() {
    double input = 123.45;
    assertEquals("<adouble>123.45</adouble>" + cr, XmlHandler.addTagValue("adouble", input));
    assertEquals("<adouble>123.45</adouble>" + cr, XmlHandler.addTagValue("adouble", input, true));
    assertEquals("<adouble>123.45</adouble>", XmlHandler.addTagValue("adouble", input, false));

    assertEquals(
        "<adouble>" + String.valueOf(Double.MAX_VALUE) + "</adouble>",
        XmlHandler.addTagValue("adouble", Double.MAX_VALUE, false));
    assertEquals(
        "<adouble>" + String.valueOf(Double.MIN_VALUE) + "</adouble>",
        XmlHandler.addTagValue("adouble", Double.MIN_VALUE, false));
    assertEquals(
        "<adouble>" + String.valueOf(Double.MIN_NORMAL) + "</adouble>",
        XmlHandler.addTagValue("adouble", Double.MIN_NORMAL, false));
  }

  @Test
  public void addTagValueBinary() throws IOException {
    byte[] input = "Test Data".getBytes();
    String result = "H4sIAAAAAAAAAAtJLS5RcEksSQQAL4PL8QkAAAA=";

    assertEquals(
        "<bytedata>" + result + "</bytedata>" + cr, XmlHandler.addTagValue("bytedata", input));
    assertEquals(
        "<bytedata>" + result + "</bytedata>" + cr,
        XmlHandler.addTagValue("bytedata", input, true));
    assertEquals(
        "<bytedata>" + result + "</bytedata>", XmlHandler.addTagValue("bytedata", input, false));
  }

  @Test
  public void addTagValueWithSurrogateCharacters() throws Exception {
    String expected =
        "<testTag attributeTest=\"test attribute value \uD842\uDFB7\" >a\uD800\uDC01\uD842\uDFB7ﻉＤtest \uD802\uDF44&lt;</testTag>";
    String tagValueWithSurrogates = "a\uD800\uDC01\uD842\uDFB7ﻉＤtest \uD802\uDF44<";
    String attributeValueWithSurrogates = "test attribute value \uD842\uDFB7";
    String result =
        XmlHandler.addTagValue(
            "testTag",
            tagValueWithSurrogates,
            false,
            "attributeTest",
            attributeValueWithSurrogates);
    assertEquals(expected, result);
    DocumentBuilder builder = XmlHandler.createDocumentBuilder(false, false);
    builder.parse(new ByteArrayInputStream(result.getBytes()));
  }

  @Test
  public void testEscapingXmlBagCharacters() throws Exception {
    String testString = "[value_start (\"\'<&>) value_end]";
    String expectedStrAfterConversion =
        "<[value_start (&#34;&#39;&lt;&amp;&gt;) value_end] "
            + "[value_start (&#34;&#39;&lt;&amp;&gt;) value_end]=\""
            + "[value_start (&#34;&#39;&lt;&amp;>) value_end]\" >"
            + "[value_start (&#34;&#39;&lt;&amp;&gt;) value_end]"
            + "</[value_start (&#34;&#39;&lt;&amp;&gt;) value_end]>";
    String result = XmlHandler.addTagValue(testString, testString, false, testString, testString);
    assertEquals(expectedStrAfterConversion, result);
  }

  @Test(expected = SAXParseException.class)
  public void createdDocumentBuilderThrowsExceptionWhenParsingXmlWithABigAmountOfExternalEntities()
      throws Exception {
    DocumentBuilder builder = XmlHandler.createDocumentBuilder(false, false);
    builder.parse(new ByteArrayInputStream(MALICIOUS_XML.getBytes()));
  }

  @Test(expected = HopXmlException.class)
  public void loadingXmlFromStreamThrowsExceptionWhenParsingXmlWithBigAmountOfExternalEntities()
      throws Exception {
    XmlHandler.loadXmlFile(
        new ByteArrayInputStream(MALICIOUS_XML.getBytes()), "<def>", false, false);
  }

  @Test(expected = HopXmlException.class)
  public void loadingXmlFromURLThrowsExceptionWhenParsingXmlWithBigAmountOfExternalEntities()
      throws Exception {
    File tmpFile = createTmpFile(MALICIOUS_XML);

    XmlHandler.loadXmlFile(tmpFile.toURI().toURL());
  }

  private File createTmpFile(String content) throws Exception {
    File tmpFile = File.createTempFile("XmlHandlerUnitTest", ".xml");
    tmpFile.deleteOnExit();

    try (PrintWriter writer = new PrintWriter(tmpFile)) {
      writer.write(content);
    }

    return tmpFile;
  }

  @Test
  public void testGetSubNode() throws Exception {
    String testXML =
        "<?xml version=\"1.0\"?>\n"
            + "<root>\n"
            + "<xpto>A</xpto>\n"
            + "<xpto>B</xpto>\n"
            + "<xpto>C</xpto>\n"
            + "<xpto>D</xpto>\n"
            + "</root>\n";

    DocumentBuilder builder = XmlHandler.createDocumentBuilder(false, false);

    Document parse = builder.parse(new ByteArrayInputStream(testXML.getBytes()));
    Node rootNode = parse.getFirstChild();
    Node lastSubNode = XmlHandler.getSubNode(rootNode, "xpto");
    assertNotNull(lastSubNode);
    assertEquals("A", lastSubNode.getTextContent());
  }

  @Test
  public void testGetLastSubNode() throws Exception {
    String testXML =
        "<?xml version=\"1.0\"?>\n"
            + "<root>\n"
            + "<xpto>A</xpto>\n"
            + "<xpto>B</xpto>\n"
            + "<xpto>C</xpto>\n"
            + "<xpto>D</xpto>\n"
            + "</root>\n";

    DocumentBuilder builder = XmlHandler.createDocumentBuilder(false, false);

    Document parse = builder.parse(new ByteArrayInputStream(testXML.getBytes()));
    Node rootNode = parse.getFirstChild();
    Node lastSubNode = XmlHandler.getLastSubNode(rootNode, "xpto");
    assertNotNull(lastSubNode);
    assertEquals("D", lastSubNode.getTextContent());
  }

  @Test
  public void testGetSubNodeByNr_WithCache() throws Exception {
    String testXML =
        "<?xml version=\"1.0\"?>\n"
            + "<root>\n"
            + "<xpto>0</xpto>\n"
            + "<xpto>1</xpto>\n"
            + "<xpto>2</xpto>\n"
            + "<xpto>3</xpto>\n"
            + "</root>\n";

    DocumentBuilder builder = XmlHandler.createDocumentBuilder(false, false);

    Document parse = builder.parse(new ByteArrayInputStream(testXML.getBytes()));
    Node rootNode = parse.getFirstChild();

    Node subNode = XmlHandler.getSubNodeByNr(rootNode, "xpto", 0);
    assertNotNull(subNode);
    assertEquals("0", subNode.getTextContent());
    subNode = XmlHandler.getSubNodeByNr(rootNode, "xpto", 1);
    assertNotNull(subNode);
    assertEquals("1", subNode.getTextContent());
    subNode = XmlHandler.getSubNodeByNr(rootNode, "xpto", 2);
    assertNotNull(subNode);
    assertEquals("2", subNode.getTextContent());
    subNode = XmlHandler.getSubNodeByNr(rootNode, "xpto", 3);
    assertNotNull(subNode);
    assertEquals("3", subNode.getTextContent());
  }

  @Test
  public void testGetSubNodeByNr_WithoutCache() throws Exception {
    String testXML =
        "<?xml version=\"1.0\"?>\n"
            + "<root>\n"
            + "<xpto>0</xpto>\n"
            + "<xpto>1</xpto>\n"
            + "<xpto>2</xpto>\n"
            + "<xpto>3</xpto>\n"
            + "</root>\n";

    DocumentBuilder builder = XmlHandler.createDocumentBuilder(false, false);

    Document parse = builder.parse(new ByteArrayInputStream(testXML.getBytes()));
    Node rootNode = parse.getFirstChild();

    Node subNode = XmlHandler.getSubNodeByNr(rootNode, "xpto", 0, false);
    assertNotNull(subNode);
    assertEquals("0", subNode.getTextContent());
    subNode = XmlHandler.getSubNodeByNr(rootNode, "xpto", 1, false);
    assertNotNull(subNode);
    assertEquals("1", subNode.getTextContent());
    subNode = XmlHandler.getSubNodeByNr(rootNode, "xpto", 2, false);
    assertNotNull(subNode);
    assertEquals("2", subNode.getTextContent());
    subNode = XmlHandler.getSubNodeByNr(rootNode, "xpto", 3, false);
    assertNotNull(subNode);
    assertEquals("3", subNode.getTextContent());
  }
}
