package authorization

import javax.inject.{ Inject, Singleton }

import com.auth0.jwt.algorithms.Algorithm
import play.api.Configuration

@Singleton
class MockTokenSignerProvider @Inject() (
    rSAKeyPairProvider: RSAKeyPairProvider,
    configuration:      Configuration
) {
  def get: Algorithm = Algorithm.RSA256( rSAKeyPairProvider.getPublicKey, rSAKeyPairProvider.getPrivateKey )

}
