package authorization

import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar

class JWTVerifierProviderTest extends FlatSpec with MockitoSugar {

  // be used in other tests

  val Provider = mock[JWTVerifierProvider]
  "The JWT provider" should "get a public key" in {
    val token = Provider.get
    // assert( token.isInstanceOf[JWTVerifier] )
  }

}
