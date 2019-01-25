package io.example.spring;

import java.util.List;

@RestController
class UserController2 {
    @GetMapping("/users")
    public List<User> getUsers(@Autowired UserService userService) {
        return userService.getUsers();
    }
}
