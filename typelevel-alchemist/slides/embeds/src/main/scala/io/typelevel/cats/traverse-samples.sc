val f: String => Future[User]

z             : List[String]
z.traverse(f) : Future[List[User]]
