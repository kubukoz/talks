package play 

class Inject
trait UserRepo

class UserService @Inject()(userRepo: UserRepo) {
  //...
}
