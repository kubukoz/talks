package io.example;

@interface Scheduled {
    int fixedRate();
}

@interface Service {}

@Service
class MySchedulingService {
    @Scheduled(fixedRate = 1000)
    void runJobAndThenWhat() {
        System.out.println("Dumping data to disk...");
        //try to figure out yourself
        //how to change error handling strategy
        throw new RuntimeException();
    }
}
