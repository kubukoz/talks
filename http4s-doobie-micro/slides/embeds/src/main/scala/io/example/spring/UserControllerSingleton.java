package io.example.spring;

import java.util.List;

@interface RestController {
}

@interface Autowired {
}

@interface GetMapping {
    String value();
}

class User {
}

interface UserService {
    List<User> getUsers();
}

@RestController
class UserController {
    private UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        return userService.getUsers();
    }
}
