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

package component

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo, verify}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, OptionValues}
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.customs.declaration.model.{ApiSubscriptionKey, VersionOne, VersionThree, VersionTwo}
import util.FakeRequests._
import util.RequestHeaders.X_CONVERSATION_ID_NAME
import util.externalservices.{ApiSubscriptionFieldsService, AuthService, GoogleAnalyticsService, MdgCancellationDeclarationService}
import util.{AuditService, CustomsDeclarationsExternalServicesConfig}

import scala.concurrent.Future

class CustomsDeclarationCancellationSpec extends ComponentTestSpec with AuditService with ExpectedTestResponses
  with Matchers
  with OptionValues
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MdgCancellationDeclarationService
  with ApiSubscriptionFieldsService
  with AuthService
  with GoogleAnalyticsService {

  private val endpoint = "/cancellation-requests"

  private val apiSubscriptionKeyForXClientIdV1 =
    ApiSubscriptionKey(clientId = clientId, context = "customs%2Fdeclarations", VersionOne)

  private val apiSubscriptionKeyForXClientIdV2 = apiSubscriptionKeyForXClientIdV1.copy(version = VersionTwo)

  private val apiSubscriptionKeyForXClientIdV3 = apiSubscriptionKeyForXClientIdV1.copy(version = VersionThree)

  override protected def beforeAll() {
    startMockServer()
  }

  override protected def beforeEach() {
    resetMockServer()
  }

  override protected def afterAll() {
    stopMockServer()
  }

  feature("Declaration API authorises cancellation of submissions from CSPs with v1.0 accept header") {
    scenario("An authorised CSP successfully submits a cancellation request") {
      Given("A CSP wants to submit a valid cancellation request")
      startMdgCancellationV1Service()
      val request: FakeRequest[AnyContentAsXml] = ValidCancellationRequestWithV1AcceptHeader.fromCsp.postTo(endpoint)
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV1)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED

      And("the response body is empty")
      contentAsString(result) shouldBe 'empty

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCsp())

      And("v1 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContext))))

      And("GA call was made")
      eventually(verifyGoogleAnalyticsServiceWasCalled())
    }
  }

  feature("Declaration API authorises cancellation of submissions from CSPs with v2.0 accept header") {
    scenario("An authorised CSP successfully submits a cancellation request") {
      Given("A CSP wants to submit a valid cancellation request")
      startMdgCancellationV2Service()
      val request: FakeRequest[AnyContentAsXml] = ValidCancellationV2Request.fromCsp.postTo(endpoint)
      startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV2)

      And("the CSP is authorised with its privileged application")
      authServiceAuthorizesCSP()

      When("a POST request with data is sent to the API")
      val result: Future[Result] = route(app = app, request).value

      Then("a response with a 202 (ACCEPTED) status is received")
      status(result) shouldBe ACCEPTED

      And("the response body is empty")
      contentAsString(result) shouldBe 'empty

      And("the request was authorised with AuthService")
      eventually(verifyAuthServiceCalledForCsp())

      And("v2 config was used")
      eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContextV2))))

      And("GA call was made")
      eventually(verifyGoogleAnalyticsServiceWasCalled())
    }
  }
    feature("Declaration API authorises cancellation of submissions from CSPs with v3.0 accept header") {
      scenario("An authorised CSP successfully submits a cancellation request") {
        Given("A CSP wants to submit a valid cancellation request")
        startMdgCancellationV3Service()
        val request: FakeRequest[AnyContentAsXml] = ValidCancellationV3Request.fromCsp.postTo(endpoint)
        startApiSubscriptionFieldsService(apiSubscriptionKeyForXClientIdV3)

        And("the CSP is authorised with its privileged application")
        authServiceAuthorizesCSP()

        When("a POST request with data is sent to the API")
        val result: Future[Result] = route(app = app, request).value

        Then("a response with a 202 (ACCEPTED) status is received")
        status(result) shouldBe ACCEPTED

        And("the response body is empty")
        contentAsString(result) shouldBe 'empty

        And("the request was authorised with AuthService")
        eventually(verifyAuthServiceCalledForCsp())

        And("v2 config was used")
        eventually(verify(1, postRequestedFor(urlEqualTo(CustomsDeclarationsExternalServicesConfig.MdgCancellationDeclarationServiceContextV3))))

        And("GA call was made")
        eventually(verifyGoogleAnalyticsServiceWasCalled())
      }
    }

  feature("Declaration API handles cancellation of submission errors from CSPs as expected") {

    scenario("Response status 400 when user submits an xml payload that does not adhere to schema having multiple errors") {
      Given("the API is available")
      stubAuditService()
      authServiceAuthorizesCSP()
      val request = InvalidCancellationRequestWith2Errors.fromCsp.postTo(endpoint)

      When("a POST request with data is sent to the API")
      val result: Option[Future[Result]] = route(app = app, request)

      Then(s"a response with a 400 status is received")
      result shouldBe 'defined
      val resultFuture = result.value

      status(resultFuture) shouldBe BAD_REQUEST
      headers(resultFuture).get(X_CONVERSATION_ID_NAME) shouldBe 'defined

      And("the response body is a \"invalid xml\" XML")
      string2xml(contentAsString(resultFuture)) shouldBe string2xml(BadRequestErrorWith2Errors)

      And("GA call was made")
      eventually(verifyGoogleAnalyticsServiceWasCalled())
    }

  }
}
