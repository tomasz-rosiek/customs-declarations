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

package uk.gov.hmrc.customs.declaration.model

import java.util.UUID

import org.joda.time.LocalDate
import play.api.libs.json._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, CredentialRole}

import scala.xml.{Elem, NodeSeq}

case class RequestedVersion(versionNumber: String, configPrefix: Option[String])

case class Eori(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class NrSubmissionId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
}

object NrSubmissionId {
  implicit val format = Json.format[NrSubmissionId]
}

sealed trait RetrievalData

case class CspRetrievalData(internalId: Option[String],
                            externalId: Option[String],
                            agentCode: Option[String],
                            confidenceLevel: ConfidenceLevel,
                            nino: Option[String],
                            saUtr: Option[String],
                            mdtpInformation: Option[MdtpInformation],
                            affinityGroup: Option[AffinityGroup],
                            credentialStrength: Option[String],
                            loginTimes: LoginTimes) extends RetrievalData

case class NonCspRetrievalData(internalId: Option[String],
                               externalId: Option[String],
                               agentCode: Option[String],
                               credentials: Credentials,
                               confidenceLevel: ConfidenceLevel,
                               nino: Option[String],
                               saUtr: Option[String],
                               name: Name,
                               dateOfBirth: Option[LocalDate],
                               email: Option[String],
                               agentInformation: AgentInformation,
                               groupIdentifier: Option[String],
                               credentialRole: Option[CredentialRole],
                               mdtpInformation: Option[MdtpInformation],
                               itmpName: ItmpName,
                               itmpDateOfBirth: Option[LocalDate],
                               itmpAddress: ItmpAddress,
                               affinityGroup: Option[AffinityGroup],
                               credentialStrength: Option[String],
                               loginTimes: LoginTimes) extends RetrievalData
object CspRetrievalData {
  implicit val mdtpInformationFormat = Json.format[MdtpInformation]
  implicit val loginTimesFormat = Json.format[LoginTimes]
  implicit val cspRetrievalData = Json.format[CspRetrievalData]
}

object NonCspRetrievalData {
  implicit val credentialsFormat = Json.format[Credentials]
  implicit val nameFormat = Json.format[Name]
  implicit val agentInformationFormat = Json.format[AgentInformation]
  implicit val mdtpInformationFormat = Json.format[MdtpInformation]
  implicit val itmpNameFormat = Json.format[ItmpName]
  implicit val itmpAddressFormat = Json.format[ItmpAddress]
  implicit val loginTimesFormat = Json.format[LoginTimes]
  implicit val nonCspRetrievalData = Json.format[NonCspRetrievalData]
}

object RetrievalData {

  implicit val mdtpInformationFormat = Json.format[MdtpInformation]
  implicit val loginTimesFormat = Json.format[LoginTimes]
  implicit val credentialsFormat = Json.format[Credentials]
  implicit val nameFormat = Json.format[Name]
  implicit val agentInformationFormat = Json.format[AgentInformation]
  implicit val itmpNameFormat = Json.format[ItmpName]
  implicit val itmpAddressFormat = Json.format[ItmpAddress]
  implicit val cspRetrievalDataFormat = Json.format[CspRetrievalData]
  implicit val nonCspRetrievalDataFormat = Json.format[NonCspRetrievalData]

  implicit val retrievalDataFormat: Format[RetrievalData] = new Format[RetrievalData] {
    def reads(json: JsValue): JsResult[RetrievalData] = {

      val validateResult = json.validate[NonCspRetrievalData]
      validateResult match {
        case JsSuccess(_, _) => Json.fromJson[NonCspRetrievalData](json)(nonCspRetrievalDataFormat)
        case JsError(_) => json.validate[CspRetrievalData] match {
          case JsSuccess(_, _) => Json.fromJson[CspRetrievalData](json)(cspRetrievalDataFormat)
          case JsError(errors) => JsError(s"Unexpected JSON value: $json, errors: $errors")
        }
      }
    }

    def writes(foo: RetrievalData): JsValue = {
      foo match {
        case b: CspRetrievalData => Json.toJson(b)(cspRetrievalDataFormat)
        case b: NonCspRetrievalData => Json.toJson(b)(nonCspRetrievalDataFormat)
      }
    }
  }
}

case class ClientId(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class ConversationId(uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}

sealed trait GoogleAnalyticsValues {
  val success: String
  val failure: String
}

object GoogleAnalyticsValues {
  val Submit: GoogleAnalyticsValues = new GoogleAnalyticsValues {
    override val success: String = "declarationSubmitSuccess"
    override val failure: String = "declarationSubmitFailure"
  }

  val Cancel: GoogleAnalyticsValues = new GoogleAnalyticsValues {
    override val success: String = "declarationCancellationSuccess"
    override val failure: String = "declarationCancellationFailure"
  }

  val Fileupload = new GoogleAnalyticsValues {
    override val success: String = "declarationFileUploadSuccess"
    override val failure: String = "declarationFileUploadFailure"
  }

  val Clearance = new GoogleAnalyticsValues {
    override val success: String = "declarationClearanceSuccess"
    override val failure: String = "declarationClearanceFailure"
  }

  //according to the ticket, amend endpoint should not call GA
  //hence, to be on a safe side exception might be in order
  val Amend = new GoogleAnalyticsValues {
    override lazy val success: String = ???
    override lazy val failure: String = ???
  }

  val ArrivalNotification = new GoogleAnalyticsValues {
    override val success: String = "declarationArrivalNotificationSuccess"
    override val failure: String = "declarationArrivalNotificationFailure"
  }
}


case class CorrelationId(uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}

case class BadgeIdentifier(value: String) extends AnyVal {
  override def toString: String = value.toString
}

case class SubscriptionFieldsId(value: String) extends AnyVal{
  override def toString: String = value.toString
}

case class DeclarationId(value: String) extends AnyVal{
  override def toString: String = value.toString
}

case class DocumentationType(value: String) extends AnyVal{
  override def toString: String = value.toString
}

sealed trait ApiVersion {
  val value: String
  val configPrefix: String
  override def toString: String = value
}
object VersionOne extends ApiVersion{
  override val value: String = "1.0"
  override val configPrefix: String = ""
}
object VersionTwo extends ApiVersion{
  override val value: String = "2.0"
  override val configPrefix: String = "v2."
}
object VersionThree extends ApiVersion{
  override val value: String = "3.0"
  override val configPrefix: String = "v3."
}

sealed trait AuthorisedAs {
  val retrievalData: Option[RetrievalData]
}
case class Csp(badgeIdentifier: BadgeIdentifier, retrievalData: Option[CspRetrievalData]) extends AuthorisedAs
case class NonCsp(eori: Eori, retrievalData: Option[NonCspRetrievalData]) extends AuthorisedAs

case class UpscanInitiatePayload(callbackUrl: String)

object UpscanInitiatePayload {
  implicit val format: OFormat[UpscanInitiatePayload] = Json.format[UpscanInitiatePayload]
}

case class AuthorisedRetrievalData(retrievalJSONBody: String)

case class FileUploadPayload(declarationID: String, documentationType: String)

case class UpscanInitiateResponsePayload(reference: String, uploadRequest: UpscanInitiateUploadRequest)

object UpscanInitiateUploadRequest {
  implicit val format: OFormat[UpscanInitiateUploadRequest] = Json.format[UpscanInitiateUploadRequest]
}

case class UpscanInitiateUploadRequest
(
  href: String,
  fields: Map[String, String]
)
{
  def addChild(n: NodeSeq, newChild: NodeSeq): NodeSeq = n match {
    case Elem(prefix, label, attribs, scope, child @ _*) =>
      Elem(prefix, label, attribs, scope, true, child ++ newChild : _*)
    case _ => sys.error("Can only add children to elements!")
  }

  def toXml: NodeSeq = {
    var xmlFields: NodeSeq = <fields></fields>

    fields.foreach { f =>
      val tag = f._1
      val content = f._2
      xmlFields = addChild(xmlFields, <a/>.copy(label = tag, child = scala.xml.Text(content)))
    }
    <fileUpload>
      <href>
        {href}
      </href>
      {xmlFields}
    </fileUpload>
  }
}

object UpscanInitiateResponsePayload {
  implicit val format: OFormat[UpscanInitiateResponsePayload] = Json.format[UpscanInitiateResponsePayload]
}

case class GoogleAnalyticsRequest(payload: String)

object GoogleAnalyticsRequest {
  implicit val format = Json.format[GoogleAnalyticsRequest]
}

case class NrsMetadata(businessId: String, notableEvent: String, payloadContentType: String, payloadSha256Checksum: String,
                       userSubmissionTimestamp: String, identityData: RetrievalData, userAuthToken: String, headerData: JsValue, searchKeys: JsValue)

object NrsMetadata {
  implicit val format = Json.format[NrsMetadata]
}

case class NrsPayload(payload: String, metadata: NrsMetadata)

object NrsPayload {
  implicit val format = Json.format[NrsPayload]
}

case class NrsResponsePayload(nrSubmissionId: NrSubmissionId)

object NrsResponsePayload {
  implicit val format = Json.format[NrsResponsePayload]
}
