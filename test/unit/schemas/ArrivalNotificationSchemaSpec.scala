/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unit.schemas

import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.Configuration
import uk.gov.hmrc.customs.declaration.services.XmlValidationService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.xml.{Elem, Node, SAXException}

class ArrivalNotificationSchemaSpec extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  protected val MockConfiguration = mock[Configuration]
  protected val MockXml = mock[Node]

  protected val propertyName: String = "xsd.locations.arrival"

  protected val xsdLocations: Seq[String] = Seq("/api/conf/2.0/schemas/wco/declaration/ARRIVAL_NOTIFICATION_METADATA.xsd",
    "/api/conf/2.0/schemas/wco/declaration/ARRIVAL_NOTIFICATION_MSG_DEFINITION.xsd"
  )

  def xmlValidationService: XmlValidationService = new XmlValidationService(MockConfiguration, schemaPropertyName = propertyName) {}

  override protected def beforeEach() {
    reset(MockConfiguration)
    when(MockConfiguration.getStringSeq(propertyName)).thenReturn(Some(xsdLocations))
    when(MockConfiguration.getInt("xml.max-errors")).thenReturn(None)
  }

  "Arrival notification request" should {
    "be successfully validated if correct" in {
      val result: Unit = await(xmlValidationService.validate(ValidArrivalNotificationXML))

      result should be(())
    }

    "fail validation if is incorrect" in {
      val caught = intercept[SAXException] {
        await(xmlValidationService.validate(InvalidArrivalNotificationXML))
      }

      caught.getMessage shouldBe "cvc-complex-type.2.4.b: The content of element 'Declaration' is not complete. One of '{\"urn:wco:datamodel:WCO:DEC-DMS:2\":Amendment}' is expected."

      Option(caught.getException) shouldBe None
    }
  }

  private val InvalidArrivalNotificationXML = <Declaration xmlns="urn:wco:datamodel:WCO:DEC-DMS:2">
    <ID>token</ID>
    <TypeCode>GPR</TypeCode> <!-- TODO what are valid values here ? -->
    <!--Optional:-->
    <Submitter>
      <ID>token</ID>
    </Submitter>
  </Declaration>


  private val ValidArrivalNotificationXML: Elem =
    <Declaration xmlns="urn:wco:datamodel:WCO:DEC-DMS:2">
      <ID>token</ID>
      <TypeCode>GPR</TypeCode> <!-- TODO what are valid values here ? -->
      <!--Optional:-->
      <Submitter>
        <ID>token</ID>
      </Submitter>
      <!--1 or more repetitions:-->
      <Amendment>
        <!--1 or more repetitions:-->
        <Pointer>
          <SequenceNumeric>1000</SequenceNumeric>
          <DocumentSectionCode>tok</DocumentSectionCode>
          <TagID>toke</TagID>
        </Pointer>
      </Amendment>
      <GoodsShipment>
        <Consignment>
          <GoodsLocation>
            <!--Optional:-->
            <Name>string</Name>
            <!--Optional:-->
            <ID>token</ID>
            <!--Optional:-->
            <TypeCode>tok</TypeCode>
            <!--Optional:-->
            <Address>
              <!--Optional:-->
              <TypeCode>tok</TypeCode>
              <!--Optional:-->
              <CityName>string</CityName>
              <!--Optional:-->
              <CountryCode>to</CountryCode>
              <!--Optional:-->
              <Line>string</Line>
              <!--Optional:-->
              <PostcodeID>token</PostcodeID>
            </Address>
          </GoodsLocation>
        </Consignment>
      </GoodsShipment>
      <BorderTransportMeans>
        <RegistrationNationalityCode>to</RegistrationNationalityCode>
      </BorderTransportMeans>
    </Declaration>

}
