import com.kubukoz.tagless.examples.UserService
import com.kubukoz.tagless.examples.MailService
import com.kubukoz.tagless.examples.MainService
import cats.tagless.autoFunctorK

object AllAtOnce {

  @autoFunctorK
  trait MainModule[F[_]] {
    def users: UserService[F]
    def mail: MailService[F]
    def main: MainService[F]
  }
}
