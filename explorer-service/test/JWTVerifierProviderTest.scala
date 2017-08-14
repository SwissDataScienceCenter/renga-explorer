package authorization

import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.interfaces.DecodedJWT
import org.scalatest.FlatSpec
import org.scalatest.mockito.MockitoSugar
import org.mockito.Mockito._

class JWTVerifierProviderTest extends FlatSpec with MockitoSugar {

  // be used in other tests


  val Provider = mock[JWTVerifierProvider]
  "The JWT provider" should "get a public key" in {
    val token = Provider.get
    // assert( token.isInstanceOf[JWTVerifier] )
  }

}
