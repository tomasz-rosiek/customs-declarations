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

package uk.gov.hmrc.customs.declaration.connectors

import com.google.inject._
import org.joda.time.DateTime
import play.api.http.HeaderNames._
import play.api.http.MimeTypes
import uk.gov.hmrc.customs.api.common.config.ServiceConfigProvider
import uk.gov.hmrc.customs.declaration.config.DeclarationsCircuitBreaker
import uk.gov.hmrc.customs.declaration.controllers.CustomHeaderNames.{XCorrelationIdHeaderName, XConversationIdHeaderName}
import uk.gov.hmrc.customs.declaration.logging.DeclarationsLogger
import uk.gov.hmrc.customs.declaration.model._
import uk.gov.hmrc.customs.declaration.model.actionbuilders.AuthorisedStatusRequest
import uk.gov.hmrc.customs.declaration.services.DeclarationsConfigService
import uk.gov.hmrc.customs.declaration.xml.MdgPayloadDecorator
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.xml.NodeSeq

@Singleton
class DeclarationStatusConnector @Inject() (val http: HttpClient,
  val logger: DeclarationsLogger,
  override val serviceConfigProvider: ServiceConfigProvider,
  override val config: DeclarationsConfigService) extends DeclarationsCircuitBreaker {

  override val configKey = "declaration-status"

  private val mdgPayloadDecorator = new MdgPayloadDecorator() // TODO inject this

  def send[A](date: DateTime,
              correlationId: CorrelationId,
              dmirId: DeclarationManagementInformationRequestId,
              apiVersion: ApiVersion,
              mrn: Mrn)(implicit asr: AuthorisedStatusRequest[A]): Future[HttpResponse] = {

    val config = Option(serviceConfigProvider.getConfig(s"${apiVersion.configPrefix}$configKey")).getOrElse(throw new IllegalArgumentException("config not found"))
    val bearerToken = "Bearer " + config.bearerToken.getOrElse(throw new IllegalStateException("no bearer token was found in config"))
    implicit val hc: HeaderCarrier = HeaderCarrier(extraHeaders = getHeaders(date, asr.conversationId, correlationId), authorization = Some(Authorization(bearerToken)))

    val declarationStatusPayload = mdgPayloadDecorator.status(asr, correlationId, date, mrn, dmirId)
    withCircuitBreaker(post(declarationStatusPayload, config.url, correlationId)).map{
      response => logger.debug(s"Declaration status response: ${response.body}")
      response
    }
  }

  private def getHeaders(date: DateTime, conversationId: ConversationId, correlationId: CorrelationId) = {
    Seq(
        (X_FORWARDED_HOST, "MDTP"),
        (XCorrelationIdHeaderName, correlationId.toString),
        (XConversationIdHeaderName, conversationId.toString),
        (DATE, date.toString("EEE, dd MMM yyyy HH:mm:ss z")),
        (CONTENT_TYPE, MimeTypes.XML + "; charset=utf-8"),
        (ACCEPT, MimeTypes.XML)
    )
  }

  private def post[A](xml: NodeSeq, url: String, correlationId: CorrelationId)(implicit asr: AuthorisedStatusRequest[A], hc: HeaderCarrier) = {
    logger.debug(s"Sending request to $url. Headers ${hc.headers} Payload: ${xml.toString()}")

    http.POSTString[HttpResponse](url, xml.toString())
      .recoverWith {
        case httpError: HttpException =>
          logger.error(s"Call to declaration status failed. url=$url")
          Future.failed(new RuntimeException(httpError))
        case e: Throwable =>
          logger.error(s"Call to declaration status failed. url=$url")
          Future.failed(e)
      }
  }
}
